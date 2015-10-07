/**
 * 
 */
package org.roda.wui.ingest.submit.client;

import java.util.List;

import org.roda.wui.common.client.BadHistoryTokenException;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.UserLogin;
import org.roda.wui.common.client.tools.Tools;
import org.roda.wui.ingest.client.Ingest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.IngestSubmitConstants;

/**
 * @author Luis Faria
 * 
 */
public class IngestSubmit {

  public static final HistoryResolver RESOLVER = new HistoryResolver() {

    @Override
    public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
      getInstance().resolve(historyTokens, callback);
    }

    @Override
    public void isCurrentUserPermitted(AsyncCallback<Boolean> callback) {
      UserLogin.getInstance().checkRole(this, callback);
    }

    @Override
    public String getHistoryToken() {
      return "submit";
    }

    @Override
    public List<String> getHistoryPath() {
      return Tools.concat(Ingest.RESOLVER.getHistoryPath(), getHistoryToken());
    }
  };

  private static IngestSubmit instance = null;

  /**
   * Get the singleton instance
   * 
   * @return {@link IngestSubmit}
   */
  public static IngestSubmit getInstance() {
    if (instance == null) {
      instance = new IngestSubmit();
    }
    return instance;
  }

  private static IngestSubmitConstants constants = (IngestSubmitConstants) GWT.create(IngestSubmitConstants.class);

  private TabPanel layout;

  private UploadSIP uploadSIP;

  private CreateSIP createSIP;

  private IngestSubmit() {

    layout = new TabPanel();
    uploadSIP = new UploadSIP();
    createSIP = new CreateSIP();
    layout.add(createSIP.getWidget(), constants.createTabTitle());
    layout.add(uploadSIP.getWidget(), constants.uploadTabTitle());

    layout.addSelectionHandler(new SelectionHandler<Integer>() {

      @Override
      public void onSelection(SelectionEvent<Integer> event) {
        String newHistoryToken;
        switch (event.getSelectedItem()) {
          case 0:
            newHistoryToken = "create";
            break;
          case 1:
          default:
            newHistoryToken = "upload";
            break;
        }

        if (!History.getToken().equals(newHistoryToken)) {
          Tools.newHistory(RESOLVER, newHistoryToken);
        }
      }
    });

    layout.addStyleName("wui-ingest-submit");
  }

  public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
    if (historyTokens.size() == 0) {
      Tools.newHistory(RESOLVER, "create");
      callback.onSuccess(null);
    } else if (historyTokens.size() == 1) {
      if (historyTokens.get(0).equals("upload")) {
        GWT.log("init upload SIP");
        uploadSIP.init();
        layout.selectTab(1);
        callback.onSuccess(layout);
      } else if (historyTokens.get(0).equals("create")) {
        createSIP.init();
        layout.selectTab(0);
        callback.onSuccess(layout);
      } else {
        callback.onFailure(new BadHistoryTokenException(historyTokens.get(0)));
      }
    } else {
      Tools.newHistory(RESOLVER, "create");
      callback.onSuccess(null);
    }
  }

}
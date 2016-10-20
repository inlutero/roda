/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
/**
 * 
 */
package org.roda.wui.client.browse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.common.Pair;
import org.roda.core.data.v2.index.facet.Facets;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.select.SelectedItemsList;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.wui.client.common.Dialogs;
import org.roda.wui.client.common.LastSelectedItemsSingleton;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.client.common.lists.SearchFileList;
import org.roda.wui.client.common.search.SearchFilters;
import org.roda.wui.client.common.search.SearchPanel;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;
import org.roda.wui.client.common.utils.HtmlSnippetUtils;
import org.roda.wui.client.common.utils.JavascriptUtils;
import org.roda.wui.client.main.BreadcrumbItem;
import org.roda.wui.client.main.BreadcrumbPanel;
import org.roda.wui.client.planning.RiskIncidenceRegister;
import org.roda.wui.client.process.CreateJob;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.tools.DescriptionLevelUtils;
import org.roda.wui.common.client.tools.RestErrorOverlayType;
import org.roda.wui.common.client.tools.RestUtils;
import org.roda.wui.common.client.tools.Tools;
import org.roda.wui.common.client.widgets.Toast;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;

import config.i18n.client.ClientMessages;

/**
 * @author Luis Faria
 * 
 */
public class Representation extends Composite {

  public static final HistoryResolver RESOLVER = new HistoryResolver() {

    @Override
    public void resolve(List<String> historyTokens, final AsyncCallback<Widget> callback) {
      if (historyTokens.size() == 2) {
        final String aipId = historyTokens.get(0);
        final String representationUUID = historyTokens.get(1);

        BrowserService.Util.getInstance().retrieveItemBundle(aipId, LocaleInfo.getCurrentLocale().getLocaleName(),
          new AsyncCallback<BrowseItemBundle>() {

            @Override
            public void onFailure(Throwable caught) {
              Toast.showError(caught.getClass().getSimpleName(), caught.getMessage());
              errorRedirect(callback);
            }

            @Override
            public void onSuccess(final BrowseItemBundle itemBundle) {
              BrowserService.Util.getInstance().retrieve(IndexedRepresentation.class.getName(), representationUUID,
                new AsyncCallback<IndexedRepresentation>() {

                  @Override
                  public void onFailure(Throwable caught) {
                    Toast.showError(caught.getClass().getSimpleName(), caught.getMessage());
                    errorRedirect(callback);
                  }

                  @Override
                  public void onSuccess(IndexedRepresentation indexedRepresentation) {
                    Representation representation = new Representation(itemBundle, indexedRepresentation);
                    callback.onSuccess(representation);
                  }
                });
            }
          });

      } else {
        errorRedirect(callback);
      }
    }

    @Override
    public void isCurrentUserPermitted(AsyncCallback<Boolean> callback) {
      UserLogin.getInstance().checkRoles(new HistoryResolver[] {Browse.RESOLVER}, false, callback);
    }

    public List<String> getHistoryPath() {
      return Tools.concat(Browse.RESOLVER.getHistoryPath(), getHistoryToken());
    }

    public String getHistoryToken() {
      return "representation";
    }

    private void errorRedirect(AsyncCallback<Widget> callback) {
      Tools.newHistory(Browse.RESOLVER);
      callback.onSuccess(null);
    }
  };

  public static final List<String> getViewItemHistoryToken(String id) {
    return Tools.concat(RESOLVER.getHistoryPath(), id);
  }

  interface MyUiBinder extends UiBinder<Widget, Representation> {
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  private static ClientMessages messages = (ClientMessages) GWT.create(ClientMessages.class);

  @UiField
  SimplePanel representationIcon;

  @UiField
  Label representationType;

  @UiField
  Label representationId;

  @UiField
  BreadcrumbPanel breadcrumb;

  @UiField
  TabPanel itemMetadata;

  @UiField
  Button newDescriptiveMetadata;

  @UiField(provided = true)
  SearchPanel searchPanel;

  @UiField(provided = true)
  SearchFileList filesList;

  private List<HandlerRegistration> handlers;
  private BrowseItemBundle itemBundle;
  private IndexedRepresentation representation;
  private String aipId;
  private String repId;
  private List<DescriptiveMetadataViewBundle> representationDescriptiveMetadata;

  private static final String ALL_FILTER = SearchFilters.allFilter(IndexedFile.class.getName());

  public Representation(BrowseItemBundle itemBundle, IndexedRepresentation representation) {
    this.itemBundle = itemBundle;
    this.representation = representation;
    this.aipId = representation.getAipId();
    this.repId = representation.getUUID();
    this.representationDescriptiveMetadata = itemBundle.getRepresentationsDescriptiveMetadata()
      .get(representation.getUUID());

    handlers = new ArrayList<HandlerRegistration>();

    String summary = messages.representationListOfFiles();
    boolean selectable = false;

    Filter filter = new Filter(new SimpleFilterParameter(RodaConstants.FILE_REPRESENTATION_UUID, repId));
    filesList = new SearchFileList(filter, true, Facets.NONE, summary, selectable);

    filesList.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        IndexedFile selected = filesList.getSelectionModel().getSelectedObject();
        if (selected != null) {
          Tools.newHistory(Browse.RESOLVER, ViewRepresentation.RESOLVER.getHistoryToken(), aipId, repId,
            selected.getUUID());
        }
      }
    });

    searchPanel = new SearchPanel(filter, ALL_FILTER, messages.searchPlaceHolder(), false, false, false);
    searchPanel.setDefaultFilterIncremental(true);
    searchPanel.setList(filesList);

    initWidget(uiBinder.createAndBindUi(this));

    HTMLPanel representationIconHtmlPanel = new HTMLPanel(
      DescriptionLevelUtils.getRepresentationTypeIcon(representation.getType(), false));
    representationIconHtmlPanel.addStyleName("browseItemIcon-other");
    representationIcon.setWidget(representationIconHtmlPanel);
    representationType.setText(representation.getType() != null ? representation.getType() : representation.getId());
    representationId.setText(repId);

    breadcrumb.updatePath(getBreadcrumbs());
    breadcrumb.setVisible(true);

    final List<Pair<String, HTML>> descriptiveMetadataContainers = new ArrayList<Pair<String, HTML>>();
    final Map<String, DescriptiveMetadataViewBundle> bundles = new HashMap<>();
    for (DescriptiveMetadataViewBundle bundle : representationDescriptiveMetadata) {
      String title = bundle.getLabel() != null ? bundle.getLabel() : bundle.getId();
      HTML container = new HTML();
      container.addStyleName("metadataContent");
      itemMetadata.add(container, title);
      descriptiveMetadataContainers.add(Pair.create(bundle.getId(), container));
      bundles.put(bundle.getId(), bundle);
    }

    HandlerRegistration tabHandler = itemMetadata.addSelectionHandler(new SelectionHandler<Integer>() {

      @Override
      public void onSelection(SelectionEvent<Integer> event) {
        if (event.getSelectedItem() < descriptiveMetadataContainers.size()) {
          Pair<String, HTML> pair = descriptiveMetadataContainers.get(event.getSelectedItem());
          String descId = pair.getFirst();
          final HTML html = pair.getSecond();
          final DescriptiveMetadataViewBundle bundle = bundles.get(descId);
          if (html.getText().length() == 0) {
            getDescriptiveMetadataHTML(descId, bundle, new AsyncCallback<SafeHtml>() {

              @Override
              public void onFailure(Throwable caught) {
                if (!AsyncCallbackUtils.treatCommonFailures(caught)) {
                  Toast.showError(messages.errorLoadingDescriptiveMetadata(caught.getMessage()));
                }
              }

              @Override
              public void onSuccess(SafeHtml result) {
                html.setHTML(result);
              }
            });
          }
        }
      }
    });

    final int addTabIndex = itemMetadata.getWidgetCount();
    FlowPanel addTab = new FlowPanel();
    addTab.add(new HTML(SafeHtmlUtils.fromSafeConstant("<i class=\"fa fa-plus-circle\"></i>")));
    itemMetadata.add(new Label(), addTab);
    HandlerRegistration addTabHandler = itemMetadata.addSelectionHandler(new SelectionHandler<Integer>() {
      @Override
      public void onSelection(SelectionEvent<Integer> event) {
        if (event.getSelectedItem() == addTabIndex) {
          newRepresentationDescriptiveMetadata();
        }
      }
    });
    addTab.addStyleName("addTab");
    addTab.getParent().addStyleName("addTabWrapper");

    handlers.add(tabHandler);
    handlers.add(addTabHandler);

    if (!representationDescriptiveMetadata.isEmpty()) {
      newDescriptiveMetadata.setVisible(false);
      itemMetadata.setVisible(true);
      itemMetadata.selectTab(0);
    } else {
      newDescriptiveMetadata.setVisible(true);
      itemMetadata.setVisible(false);
    }
  }
  
  @Override
  protected void onLoad() {
    super.onLoad();
    JavascriptUtils.stickSidebar();
  }

  private List<BreadcrumbItem> getBreadcrumbs() {
    IndexedAIP aip = itemBundle.getAip();
    List<IndexedAIP> aipAncestors = itemBundle.getAIPAncestors();

    List<BreadcrumbItem> breadcrumb = new ArrayList<>();
    breadcrumb
      .add(new BreadcrumbItem(DescriptionLevelUtils.getTopIconSafeHtml(), "", Browse.RESOLVER.getHistoryPath()));

    if (aipAncestors != null) {
      for (IndexedAIP ancestor : aipAncestors) {
        if (ancestor != null) {
          SafeHtml breadcrumbLabel = HtmlSnippetUtils.getBreadcrumbLabel(ancestor);
          String breadcrumbTitle = HtmlSnippetUtils.getBreadcrumbTitle(ancestor);
          BreadcrumbItem ancestorBreadcrumb = new BreadcrumbItem(breadcrumbLabel, breadcrumbTitle,
            Tools.concat(Browse.RESOLVER.getHistoryPath(), ancestor.getId()));
          breadcrumb.add(1, ancestorBreadcrumb);
        } else {
          SafeHtml breadcrumbLabel = DescriptionLevelUtils.getElementLevelIconSafeHtml(RodaConstants.AIP_GHOST, false);
          BreadcrumbItem unknownAncestorBreadcrumb = new BreadcrumbItem(breadcrumbLabel, "", new Command() {

            @Override
            public void execute() {
              // TODO find better error message
              Toast.showError(messages.unknownAncestorError());
            }
          });
          breadcrumb.add(unknownAncestorBreadcrumb);
        }
      }
    }

    // AIP
    breadcrumb.add(new BreadcrumbItem(HtmlSnippetUtils.getBreadcrumbLabel(aip),
      HtmlSnippetUtils.getBreadcrumbTitle(aip), Tools.concat(Browse.RESOLVER.getHistoryPath(), aipId)));

    // Representation
    breadcrumb.add(new BreadcrumbItem(DescriptionLevelUtils.getRepresentationTypeIcon(representation.getType(), true),
      representation.getType(), Tools.concat(Representation.RESOLVER.getHistoryPath(), aipId, repId)));

    return breadcrumb;
  }

  private void getDescriptiveMetadataHTML(final String descId, final DescriptiveMetadataViewBundle bundle,
    final AsyncCallback<SafeHtml> callback) {
    SafeUri uri = RestUtils.createRepresentationDescriptiveMetadataHTMLUri(repId, descId);
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, uri.asString());
    requestBuilder.setHeader("Authorization", "Custom");
    try {
      requestBuilder.sendRequest(null, new RequestCallback() {

        @Override
        public void onResponseReceived(Request request, Response response) {
          if (200 == response.getStatusCode()) {
            String html = response.getText();

            SafeHtmlBuilder b = new SafeHtmlBuilder();
            b.append(SafeHtmlUtils.fromSafeConstant("<div class='descriptiveMetadataLinks'>"));

            if (bundle.hasHistory()) {
              // History link
              String historyLink = Tools.createHistoryHashLink(DescriptiveMetadataHistory.RESOLVER, aipId, repId,
                descId);
              String historyLinkHtml = "<a href='" + historyLink
                + "' class='toolbarLink'><i class='fa fa-history'></i></a>";
              b.append(SafeHtmlUtils.fromSafeConstant(historyLinkHtml));
            }
            // Edit link
            String editLink = Tools.createHistoryHashLink(EditDescriptiveMetadata.RESOLVER, aipId, repId, descId);
            String editLinkHtml = "<a href='" + editLink + "' class='toolbarLink'><i class='fa fa-edit'></i></a>";
            b.append(SafeHtmlUtils.fromSafeConstant(editLinkHtml));

            // Download link
            SafeUri downloadUri = RestUtils.createRepresentationDescriptiveMetadataDownloadUri(repId, descId);
            String downloadLinkHtml = "<a href='" + downloadUri.asString()
              + "' class='toolbarLink'><i class='fa fa-download'></i></a>";
            b.append(SafeHtmlUtils.fromSafeConstant(downloadLinkHtml));

            b.append(SafeHtmlUtils.fromSafeConstant("</div>"));

            b.append(SafeHtmlUtils.fromSafeConstant("<div class='descriptiveMetadataHTML'>"));
            b.append(SafeHtmlUtils.fromTrustedString(html));
            b.append(SafeHtmlUtils.fromSafeConstant("</div>"));
            SafeHtml safeHtml = b.toSafeHtml();

            callback.onSuccess(safeHtml);
          } else {
            String text = response.getText();
            String message;
            try {
              RestErrorOverlayType error = (RestErrorOverlayType) JsonUtils.safeEval(text);
              message = error.getMessage();
            } catch (IllegalArgumentException e) {
              message = text;
            }

            SafeHtmlBuilder b = new SafeHtmlBuilder();
            b.append(SafeHtmlUtils.fromSafeConstant("<div class='descriptiveMetadataLinks'>"));

            if (bundle.hasHistory()) {
              // History link
              String historyLink = Tools.createHistoryHashLink(DescriptiveMetadataHistory.RESOLVER, aipId, repId,
                descId);
              String historyLinkHtml = "<a href='" + historyLink
                + "' class='toolbarLink'><i class='fa fa-history'></i></a>";
              b.append(SafeHtmlUtils.fromSafeConstant(historyLinkHtml));
            }

            // Edit link
            String editLink = Tools.createHistoryHashLink(EditDescriptiveMetadata.RESOLVER, aipId, repId, descId);
            String editLinkHtml = "<a href='" + editLink + "' class='toolbarLink'><i class='fa fa-edit'></i></a>";
            b.append(SafeHtmlUtils.fromSafeConstant(editLinkHtml));

            b.append(SafeHtmlUtils.fromSafeConstant("</div>"));

            // error message
            b.append(SafeHtmlUtils.fromSafeConstant("<div class='error'>"));
            b.append(messages.descriptiveMetadataTranformToHTMLError());
            b.append(SafeHtmlUtils.fromSafeConstant("<pre><code>"));
            b.append(SafeHtmlUtils.fromString(message));
            b.append(SafeHtmlUtils.fromSafeConstant("</core></pre>"));
            b.append(SafeHtmlUtils.fromSafeConstant("</div>"));

            callback.onSuccess(b.toSafeHtml());
          }
        }

        @Override
        public void onError(Request request, Throwable exception) {
          callback.onFailure(exception);
        }
      });
    } catch (RequestException e) {
      callback.onFailure(e);
    }
  }

  private void newRepresentationDescriptiveMetadata() {
    Tools.newHistory(CreateDescriptiveMetadata.RESOLVER, "representation", aipId, repId);
  }

  @UiHandler("newDescriptiveMetadata")
  void buttonNewDescriptiveMetadataEventsHandler(ClickEvent e) {
    newRepresentationDescriptiveMetadata();
  }

  @UiHandler("download")
  void buttonDownloadHandler(ClickEvent e) {
    SafeUri downloadUri = null;
    if (repId != null) {
      downloadUri = RestUtils.createRepresentationDownloadUri(repId);
    }
    if (downloadUri != null) {
      Window.Location.assign(downloadUri.asString());
    }
  }

  @UiHandler("remove")
  void buttonRemoveHandler(ClickEvent e) {
    Dialogs.showConfirmDialog(messages.representationRemoveTitle(), messages.representationRemoveMessage(),
      messages.dialogCancel(), messages.dialogYes(), new AsyncCallback<Boolean>() {

        @Override
        public void onSuccess(Boolean confirmed) {
          if (confirmed) {
            SelectedItemsList<IndexedRepresentation> selected = new SelectedItemsList<IndexedRepresentation>(
              Arrays.asList(repId), IndexedRepresentation.class.getName());
            BrowserService.Util.getInstance().deleteRepresentation(selected, new AsyncCallback<Void>() {

              @Override
              public void onSuccess(Void result) {
                Tools.newHistory(Browse.RESOLVER, aipId);
              }

              @Override
              public void onFailure(Throwable caught) {
                AsyncCallbackUtils.defaultFailureTreatment(caught);
              }
            });
          }
        }

        @Override
        public void onFailure(Throwable caught) {
          // nothing to do
        }
      });
  }

  @UiHandler("viewer")
  void buttonViewHandler(ClickEvent e) {
    Tools.newHistory(Browse.RESOLVER, ViewRepresentation.RESOLVER.getHistoryToken(), aipId, repId);
  }

  @UiHandler("newProcess")
  void buttonNewProcessHandler(ClickEvent e) {
    if (repId != null) {
      LastSelectedItemsSingleton selectedItems = LastSelectedItemsSingleton.getInstance();
      selectedItems.setSelectedItems(SelectedItemsList.create(IndexedRepresentation.class, repId));
      Tools.newHistory(CreateJob.RESOLVER, "action");
    }
  }

  @UiHandler("risks")
  void buttonRisksHandler(ClickEvent e) {
    if (aipId != null) {
      Tools.newHistory(RiskIncidenceRegister.RESOLVER, aipId, representation.getId());
    }
  }

  @UiHandler("preservationEvents")
  void buttonPreservationEventsHandler(ClickEvent e) {
    if (aipId != null) {
      Tools.newHistory(PreservationEvents.RESOLVER, aipId, repId);
    }
  }
}

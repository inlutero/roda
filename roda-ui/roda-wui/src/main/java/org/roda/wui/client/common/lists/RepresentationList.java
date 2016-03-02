/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.client.common.lists;

import java.util.HashMap;
import java.util.Map;

import org.roda.core.data.adapter.facet.Facets;
import org.roda.core.data.adapter.filter.Filter;
import org.roda.core.data.adapter.sort.Sorter;
import org.roda.core.data.adapter.sublist.Sublist;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.IdUtils;
import org.roda.core.data.v2.index.IndexResult;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.wui.client.browse.BrowserService;
import org.roda.wui.common.client.ClientLogger;
import org.roda.wui.common.client.tools.Humanize;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ProvidesKey;

public class RepresentationList extends AsyncTableCell<IndexedRepresentation> {

  private static final int PAGE_SIZE = 20;

  private final ClientLogger logger = new ClientLogger(getClass().getName());

  private TextColumn<IndexedRepresentation> idColumn;
  private TextColumn<IndexedRepresentation> originalColumn;
  private TextColumn<IndexedRepresentation> sizeInBytesColumn;
  private TextColumn<IndexedRepresentation> totalNumberOfFilesColumn;

  public RepresentationList() {
    this(null, null, null);
  }

  public RepresentationList(Filter filter, Facets facets, String summary) {
    super(filter, facets, summary);
  }

  @Override
  protected void configureDisplay(CellTable<IndexedRepresentation> display) {

    idColumn = new TextColumn<IndexedRepresentation>() {

      @Override
      public String getValue(IndexedRepresentation rep) {
        return rep != null ? rep.getId() : null;
      }
    };

    originalColumn = new TextColumn<IndexedRepresentation>() {

      @Override
      public String getValue(IndexedRepresentation rep) {
        return rep != null ? rep.isOriginal() ? "original" : "alternative" : null;
      }
    };

    sizeInBytesColumn = new TextColumn<IndexedRepresentation>() {

      @Override
      public String getValue(IndexedRepresentation rep) {
        return rep != null ? Humanize.readableFileSize(rep.getSizeInBytes()) : null;
      }
    };

    totalNumberOfFilesColumn = new TextColumn<IndexedRepresentation>() {

      @Override
      public String getValue(IndexedRepresentation rep) {
        return rep != null ? rep.getTotalNumberOfFiles() + " files" : null;
      }
    };

    /* add sortable */
    idColumn.setSortable(true);
    originalColumn.setSortable(true);
    sizeInBytesColumn.setSortable(true);
    totalNumberOfFilesColumn.setSortable(true);

    // TODO externalize strings into constants
    display.addColumn(idColumn, "Id");
    display.addColumn(originalColumn, "Original");
    display.addColumn(sizeInBytesColumn, "Size");
    display.addColumn(totalNumberOfFilesColumn, "Number of files");

    Label emptyInfo = new Label("No items to display");
    display.setEmptyTableWidget(emptyInfo);
    display.setColumnWidth(idColumn, "100%");

    originalColumn.setCellStyleNames("nowrap");
    sizeInBytesColumn.setCellStyleNames("nowrap");
    totalNumberOfFilesColumn.setCellStyleNames("nowrap");

    // define default sorting
    display.getColumnSortList().push(new ColumnSortInfo(idColumn, false));

    addStyleName("my-representation-table");
    emptyInfo.addStyleName("my-representation-empty-info");

  }

  @Override
  protected void getData(Sublist sublist, ColumnSortList columnSortList,
    AsyncCallback<IndexResult<IndexedRepresentation>> callback) {
    Filter filter = getFilter();
    if (filter == null) {
      // search not yet ready, deliver empty result
      callback.onSuccess(null);
    } else {

      Map<Column<IndexedRepresentation, ?>, String> columnSortingKeyMap = new HashMap<Column<IndexedRepresentation, ?>, String>();
      columnSortingKeyMap.put(idColumn, RodaConstants.SRO_ID);
      columnSortingKeyMap.put(originalColumn, RodaConstants.SRO_ORIGINAL);
      columnSortingKeyMap.put(sizeInBytesColumn, RodaConstants.SRO_SIZE_IN_BYTES);
      columnSortingKeyMap.put(totalNumberOfFilesColumn, RodaConstants.SRO_TOTAL_NUMBER_OF_FILES);

      Sorter sorter = createSorter(columnSortList, columnSortingKeyMap);

      BrowserService.Util.getInstance().find(IndexedRepresentation.class.getName(), filter, sorter, sublist,
        getFacets(), LocaleInfo.getCurrentLocale().getLocaleName(), callback);
    }
  }

  @Override
  protected ProvidesKey<IndexedRepresentation> getKeyProvider() {
    return new ProvidesKey<IndexedRepresentation>() {

      @Override
      public Object getKey(IndexedRepresentation item) {
        String uuid = IdUtils.getRepresentationId(item.getAipId(), item.getId());
        return uuid;
      }
    };
  }

  @Override
  protected int getInitialPageSize() {
    return PAGE_SIZE;
  }

}
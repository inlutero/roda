/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.client.common.lists;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.roda.core.data.adapter.facet.Facets;
import org.roda.core.data.adapter.filter.Filter;
import org.roda.core.data.adapter.sort.Sorter;
import org.roda.core.data.adapter.sublist.Sublist;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.IndexResult;
import org.roda.core.data.v2.Job;
import org.roda.wui.client.browse.BrowserService;
import org.roda.wui.common.client.ClientLogger;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ProvidesKey;

/**
 * 
 * @author Luis Faria <lfaria@keep.pt>
 *
 */
public class JobList extends AsyncTableCell<Job> {

  private static final int PAGE_SIZE = 20;

  private final ClientLogger logger = new ClientLogger(getClass().getName());

  // private TextColumn<SIPReport> idColumn;
  private TextColumn<Job> nameColumn;
  private Column<Job, Date> startDateColumn;
  private Column<Job, Date> endDateColumn;
  private TextColumn<Job> statusColumn;
  private TextColumn<Job> percentageColumn;
  private TextColumn<Job> usernameColumn;

  public JobList() {
    this(null, null, null);
  }

  public JobList(Filter filter, Facets facets, String summary) {
    super(filter, facets, summary);
  }

  @Override
  protected void configureDisplay(CellTable<Job> display) {

    nameColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        return job != null ? job.getName() : null;
      }
    };

    startDateColumn = new Column<Job, Date>(new DateCell(DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss.SSS"))) {
      @Override
      public Date getValue(Job job) {
        return job != null ? job.getStartDate() : null;
      }
    };

    endDateColumn = new Column<Job, Date>(new DateCell(DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss.SSS"))) {
      @Override
      public Date getValue(Job job) {
        return job != null ? job.getEndDate() : null;
      }
    };

    statusColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        return job != null ? job.getState().toString() : null;
      }
    };

    percentageColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        return job != null ? NumberFormat.getPercentFormat().format(job.getCompletionPercentage() / 100.0) : null;
      }
    };

    usernameColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        return job != null ? job.getUsername() : null;
      }
    };

    nameColumn.setSortable(true);
    startDateColumn.setSortable(true);
    endDateColumn.setSortable(true);
    statusColumn.setSortable(true);
    percentageColumn.setSortable(true);
    usernameColumn.setSortable(true);

    // TODO externalize strings into constants
    display.addColumn(nameColumn, "Name");
    display.addColumn(startDateColumn, "Start date");
    display.addColumn(endDateColumn, "End date");
    display.addColumn(statusColumn, "Status");
    display.addColumn(percentageColumn, "% done");
    display.addColumn(usernameColumn, "Creator");

    Label emptyInfo = new Label("No items to display");
    display.setEmptyTableWidget(emptyInfo);
    display.setColumnWidth(nameColumn, "100%");

    startDateColumn.setCellStyleNames("nowrap text-align-right");
    endDateColumn.setCellStyleNames("nowrap text-align-right");
    statusColumn.setCellStyleNames("nowrap");
    percentageColumn.setCellStyleNames("nowrap text-align-right");
    usernameColumn.setCellStyleNames("nowrap");
  }

  @Override
  protected void getData(Sublist sublist, ColumnSortList columnSortList, AsyncCallback<IndexResult<Job>> callback) {

    Filter filter = getFilter();

    Map<Column<Job, ?>, String> columnSortingKeyMap = new HashMap<Column<Job, ?>, String>();
    columnSortingKeyMap.put(nameColumn, RodaConstants.JOB_NAME);
    columnSortingKeyMap.put(startDateColumn, RodaConstants.JOB_START_DATE);
    columnSortingKeyMap.put(endDateColumn, RodaConstants.JOB_END_DATE);
    columnSortingKeyMap.put(statusColumn, RodaConstants.JOB_STATE);
    columnSortingKeyMap.put(percentageColumn, RodaConstants.JOB_COMPLETION_PERCENTAGE);
    columnSortingKeyMap.put(usernameColumn, RodaConstants.JOB_USERNAME);

    Sorter sorter = createSorter(columnSortList, columnSortingKeyMap);

    BrowserService.Util.getInstance().findJobs(filter, sorter, sublist, getFacets(), callback);
  }

  @Override
  protected ProvidesKey<Job> getKeyProvider() {
    return new ProvidesKey<Job>() {

      @Override
      public Object getKey(Job item) {
        return item.getId();
      }
    };
  }

  @Override
  protected int getInitialPageSize() {
    return PAGE_SIZE;
  }

}
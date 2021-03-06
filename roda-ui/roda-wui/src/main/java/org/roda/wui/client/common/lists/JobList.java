/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.client.common.lists;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.facet.Facets;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.jobs.Job;
import org.roda.wui.client.common.lists.utils.BasicAsyncTableCell;
import org.roda.wui.client.common.lists.utils.TooltipTextColumn;
import org.roda.wui.client.common.utils.HtmlSnippetUtils;
import org.roda.wui.common.client.tools.Humanize;
import org.roda.wui.common.client.tools.Humanize.DHMSFormat;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.TextColumn;

import config.i18n.client.ClientMessages;

/**
 * 
 * @author Luis Faria <lfaria@keep.pt>
 *
 */
public class JobList extends BasicAsyncTableCell<Job> {

  // private final ClientLogger logger = new ClientLogger(getClass().getName());
  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  private TooltipTextColumn<Job> nameColumn;
  private TextColumn<Job> usernameColumn;
  private Column<Job, Date> startDateColumn;
  private TextColumn<Job> durationColumn;
  private Column<Job, SafeHtml> statusColumn;
  private TextColumn<Job> progressColumn;
  private TextColumn<Job> objectsTotalCountColumn;
  private Column<Job, SafeHtml> objectsSuccessCountColumn;
  private Column<Job, SafeHtml> objectsFailureCountColumn;
  // private Column<Job, SafeHtml> objectsProcessingCountColumn;
  // private Column<Job, SafeHtml> objectsWaitingCountColumn;

  public JobList() {
    this(null, null, null, false);
  }

  public JobList(Filter filter, Facets facets, String summary, boolean selectable) {
    super(Job.class, filter, true, facets, summary, selectable);
  }

  public JobList(Filter filter, Facets facets, String summary, boolean selectable, int pageSize, int incrementPage) {
    super(Job.class, filter, true, facets, summary, selectable, pageSize, incrementPage);
  }

  @Override
  protected void configureDisplay(CellTable<Job> display) {

    nameColumn = new TooltipTextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        return job != null ? job.getName() : null;
      }
    };

    usernameColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        return job != null ? job.getUsername() : null;
      }
    };

    startDateColumn = new Column<Job, Date>(
      new DateCell(DateTimeFormat.getFormat(RodaConstants.DEFAULT_DATETIME_FORMAT))) {
      @Override
      public Date getValue(Job job) {
        return job != null ? job.getStartDate() : null;
      }
    };

    durationColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        if (job == null) {
          return null;
        }
        Date end = job.getEndDate() != null ? job.getEndDate() : getDate();
        return Humanize.durationInDHMS(job.getStartDate(), end, DHMSFormat.SHORT);
      }
    };

    statusColumn = new Column<Job, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(Job job) {
        return HtmlSnippetUtils.getJobStateHtml(job);
      }
    };

    objectsTotalCountColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        String ret = "";
        if (job != null) {
          if (job.getJobStats().getSourceObjectsCount() > 0) {
            ret = job.getJobStats().getSourceObjectsCount() + "";
          }
        }
        return ret;
      }
    };

    objectsSuccessCountColumn = new Column<Job, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(Job job) {
        SafeHtmlBuilder b = new SafeHtmlBuilder();
        if (job != null) {
          b.append(
            job.getJobStats().getSourceObjectsProcessedWithSuccess() > 0 ? SafeHtmlUtils.fromSafeConstant("<span>")
              : SafeHtmlUtils.fromSafeConstant("<span class='ingest-process-counter-0'>"));
          b.append(job.getJobStats().getSourceObjectsProcessedWithSuccess());
          b.append(SafeHtmlUtils.fromSafeConstant("</span>"));
        }
        return b.toSafeHtml();
      }
    };

    objectsFailureCountColumn = new Column<Job, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(Job job) {
        SafeHtmlBuilder b = new SafeHtmlBuilder();
        if (job != null) {
          b.append(SafeHtmlUtils.fromSafeConstant("<span"));
          if (job.getJobStats().getSourceObjectsProcessedWithFailure() > 0) {
            b.append(SafeHtmlUtils.fromSafeConstant(" class='ingest-process-failed-column'"));
          } else {
            b.append(SafeHtmlUtils.fromSafeConstant(" class='ingest-process-counter-0'"));
          }
          b.append(SafeHtmlUtils.fromSafeConstant(">"));
          b.append(job.getJobStats().getSourceObjectsProcessedWithFailure());
          b.append(SafeHtmlUtils.fromSafeConstant("</span>"));
        }
        return b.toSafeHtml();
      }
    };

    // objectsWaitingCountColumn = new Column<Job, SafeHtml>(new SafeHtmlCell())
    // {
    // @Override
    // public SafeHtml getValue(Job job) {
    // SafeHtmlBuilder b = new SafeHtmlBuilder();
    // if (job != null) {
    // b.append(
    // job.getJobStats().getSourceObjectsWaitingToBeProcessed() > 0 ?
    // SafeHtmlUtils.fromSafeConstant("<span>")
    // : SafeHtmlUtils.fromSafeConstant("<span
    // class='ingest-process-counter-0'>"));
    // b.append(job.getJobStats().getSourceObjectsWaitingToBeProcessed());
    // b.append(SafeHtmlUtils.fromSafeConstant("</span>"));
    // }
    // return b.toSafeHtml();
    // }
    // };
    //
    // objectsProcessingCountColumn = new Column<Job, SafeHtml>(new
    // SafeHtmlCell()) {
    // @Override
    // public SafeHtml getValue(Job job) {
    // SafeHtmlBuilder b = new SafeHtmlBuilder();
    // if (job != null) {
    // b.append(job.getJobStats().getSourceObjectsBeingProcessed() > 0 ?
    // SafeHtmlUtils.fromSafeConstant("<span>")
    // : SafeHtmlUtils.fromSafeConstant("<span
    // class='ingest-process-counter-0'>"));
    // b.append(job.getJobStats().getSourceObjectsBeingProcessed());
    // b.append(SafeHtmlUtils.fromSafeConstant("</span>"));
    // }
    // return b.toSafeHtml();
    // }
    // };

    progressColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        return job != null ? job.getJobStats().getCompletionPercentage() + "%" : null;
      }
    };

    nameColumn.setSortable(true);
    usernameColumn.setSortable(true);
    startDateColumn.setSortable(true);
    statusColumn.setSortable(true);
    objectsTotalCountColumn.setSortable(true);
    objectsSuccessCountColumn.setSortable(true);
    objectsFailureCountColumn.setSortable(true);
    // objectsWaitingCountColumn.setSortable(true);
    // objectsProcessingCountColumn.setSortable(true);
    progressColumn.setSortable(true);

    addColumn(nameColumn, messages.jobName(), true, false);
    addColumn(usernameColumn, messages.jobCreator(), true, false);
    addColumn(startDateColumn, messages.jobStartDate(), true, false, 11);
    addColumn(durationColumn, messages.jobDuration(), true, true, 6);
    addColumn(statusColumn, messages.jobStatus(), true, false, 7);
    addColumn(progressColumn, messages.jobProgress(), true, true, 5);
    addColumn(objectsTotalCountColumn, messages.jobTotalCountMessage(), true, true, 5);
    addColumn(objectsSuccessCountColumn, messages.jobSuccessCountMessage(), true, true, 6);
    addColumn(objectsFailureCountColumn, messages.jobFailureCountMessage(), true, true, 5);
    // addColumn(objectsProcessingCountColumn,
    // messages.jobProcessingCountMessage(), true, true, 6);
    // addColumn(objectsWaitingCountColumn, messages.jobWaitingCountMessage(),
    // true, true, 5);

    // default sorting
    display.getColumnSortList().push(new ColumnSortInfo(startDateColumn, false));

  }

  @Override
  protected Sorter getSorter(ColumnSortList columnSortList) {
    Map<Column<Job, ?>, List<String>> columnSortingKeyMap = new HashMap<Column<Job, ?>, List<String>>();
    columnSortingKeyMap.put(nameColumn, Arrays.asList(RodaConstants.JOB_NAME));
    columnSortingKeyMap.put(startDateColumn, Arrays.asList(RodaConstants.JOB_START_DATE));
    columnSortingKeyMap.put(statusColumn, Arrays.asList(RodaConstants.JOB_STATE));
    columnSortingKeyMap.put(progressColumn, Arrays.asList(RodaConstants.JOB_COMPLETION_PERCENTAGE));
    columnSortingKeyMap.put(objectsTotalCountColumn, Arrays.asList(RodaConstants.JOB_SOURCE_OBJECTS_COUNT));
    columnSortingKeyMap.put(objectsSuccessCountColumn,
      Arrays.asList(RodaConstants.JOB_SOURCE_OBJECTS_PROCESSED_WITH_SUCCESS));
    columnSortingKeyMap.put(objectsFailureCountColumn,
      Arrays.asList(RodaConstants.JOB_SOURCE_OBJECTS_PROCESSED_WITH_FAILURE));
    // columnSortingKeyMap.put(objectsProcessingCountColumn,
    // Arrays.asList(RodaConstants.JOB_SOURCE_OBJECTS_BEING_PROCESSED));
    // columnSortingKeyMap.put(objectsWaitingCountColumn,
    // Arrays.asList(RodaConstants.JOB_SOURCE_OBJECTS_WAITING_TO_BE_PROCESSED));
    columnSortingKeyMap.put(usernameColumn, Arrays.asList(RodaConstants.JOB_USERNAME));
    return createSorter(columnSortList, columnSortingKeyMap);
  }

}

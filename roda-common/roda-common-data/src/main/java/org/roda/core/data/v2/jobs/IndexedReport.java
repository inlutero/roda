package org.roda.core.data.v2.jobs;

import java.util.Arrays;
import java.util.List;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.IsIndexed;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class IndexedReport extends Report implements IsIndexed {
  private static final long serialVersionUID = 3735723897367711258L;

  private String jobName = null;
  private String sourceObjectLabel = null;
  private String outcomeObjectLabel = null;

  public IndexedReport() {
    super();
  }

  public IndexedReport(IndexedReport report) {
    super(report);
    jobName = report.getJobName();
    sourceObjectLabel = report.getSourceObjectLabel();
    outcomeObjectLabel = report.getOutcomeObjectLabel();
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getSourceObjectLabel() {
    return sourceObjectLabel;
  }

  public void setSourceObjectLabel(String sourceObjectLabel) {
    this.sourceObjectLabel = sourceObjectLabel;
  }

  public String getOutcomeObjectLabel() {
    return outcomeObjectLabel;
  }

  public void setOutcomeObjectLabel(String outcomeObjectLabel) {
    this.outcomeObjectLabel = outcomeObjectLabel;
  }

  @Override
  public List<String> toCsvHeaders() {
    return Arrays.asList("id", "jobId", "sourceObjectId", "sourceObjectClass", "sourceObjectOriginalIds",
      "outcomeObjectId", "outcomeObjectClass", "outcomeObjectState", "title", "dateCreated", "dateUpdated",
      "completionPercentage", "stepsCompleted", "totalSteps", "plugin", "pluginName", "pluginVersion", "pluginState",
      "pluginDetails", "htmlPluginDetails", "reports");
  }

  @Override
  public List<Object> toCsvValues() {
    return Arrays.asList(super.getId(), super.getJobId(), super.getSourceObjectId(), super.getSourceObjectClass(),
      super.getSourceObjectOriginalIds(), super.getOutcomeObjectId(), super.getOutcomeObjectClass(),
      super.getOutcomeObjectState(), super.getTitle(), super.getDateCreated(), super.getDateUpdated(),
      super.getCompletionPercentage(), super.getStepsCompleted(), super.getTotalSteps(), super.getPlugin(),
      super.getPluginName(), super.getPluginVersion(), super.getPluginState(), super.getPluginDetails(),
      super.isHtmlPluginDetails(), super.getReports());
  }

  @JsonIgnore
  @Override
  public String getUUID() {
    return getId();
  }

  @Override
  public List<String> liteFields() {
    return Arrays.asList(RodaConstants.JOB_REPORT_JOB_ID, RodaConstants.INDEX_UUID);
  }
}

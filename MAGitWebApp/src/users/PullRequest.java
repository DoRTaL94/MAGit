package users;

import data.structures.Branch;
import data.structures.Difference;
import magit.Engine;

import java.text.SimpleDateFormat;

public class PullRequest {
    private int id                      = -1;
    private Branch target               = null;
    private Branch base                 = null;
    private String declineReason        = "";
    private String pullRequestMessage   = "";
    private String requestByUserName    = "";
    private String relevantRepoName     = "";
    private boolean isReferred          = false;
    private boolean isApproved          = false;
    private Difference commitDiff       = null;
    private String timeStamp            = null;

    public PullRequest() {
        timeStamp = new SimpleDateFormat(Engine.DATE_FORMAT).format(System.currentTimeMillis());
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Difference getCommitDiff() {
        return commitDiff;
    }

    public void setCommitDiff(Difference commitDiff) {
        this.commitDiff = commitDiff;
    }

    public Branch getTarget() {
        return target;
    }

    public void setTarget(Branch target) {
        this.target = target;
    }

    public Branch getBase() {
        return base;
    }

    public void setBase(Branch base) {
        this.base = base;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    public String getPullRequestMessage() {
        return pullRequestMessage;
    }

    public void setPullRequestMessage(String pullRequestMessage) {
        this.pullRequestMessage = pullRequestMessage;
    }

    public String getRequestByUserName() {
        return requestByUserName;
    }

    public void setRequestByUserName(String requestByUserName) {
        this.requestByUserName = requestByUserName;
    }

    public String getRelevantRepoName() {
        return relevantRepoName;
    }

    public void setRelevantRepoName(String relevantRepoName) {
        this.relevantRepoName = relevantRepoName;
    }

    public boolean isReferred() {
        return isReferred;
    }

    public void setReferred(boolean referred) {
        isReferred = referred;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }
}

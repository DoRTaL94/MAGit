package notifications;

import data.structures.Repository;
import users.User;

public class PullRequestNotification implements INotification {
    private String relevantRepoName;
    private String requestByUserName;
    private String pullRequestMessage;
    private String targetBranchName;
    private String baseBranchName;
    private boolean isRefered;
    private boolean isApproved;
    private String declineReason;

    public boolean isRefered() {
        return isRefered;
    }

    public void setIsRefered(boolean i_IsRefered) {
        isRefered = i_IsRefered;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setIsApproved(boolean i_IsApproved) {
        isApproved = i_IsApproved;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String i_DeclineReason) {
        declineReason = i_DeclineReason;
    }

    public String getRelevantRepo() {
        return relevantRepoName;
    }

    public void setRelevantRepo(String i_RelevantRepo) {
        relevantRepoName = i_RelevantRepo;
    }

    public String getRequestByUser() {
        return requestByUserName;
    }

    public void setRequestByUser(String i_RequestByUser) {
        requestByUserName = i_RequestByUser;
    }

    public String getPullRequestMessage() {
        return pullRequestMessage;
    }

    public void setPullRequestMessage(String i_PullRequestMessage) {
        pullRequestMessage = i_PullRequestMessage;
    }

    public String getTargetBranchName() {
        return targetBranchName;
    }

    public void setTargetBranchName(String i_TargetBranchName) {
        targetBranchName = i_TargetBranchName;
    }

    public String getBaseBranchName() {
        return baseBranchName;
    }

    public void setBaseBranchName(String i_BaseBranchName) {
        baseBranchName = i_BaseBranchName;
    }
}

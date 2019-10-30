package notifications;

import users.PullRequest;

public class PrAnswerNotification implements INotification {
    private PullRequest pullRequest = null;
    private String declineReason    = "";
    private boolean isReadByUser    = false;
    private final String type       = "prAnswer";
    private boolean isNotShow       = false;

    public void setNotShowNotification(boolean i_IsNotShow) {
        isNotShow = true;
    }
    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    public boolean isReadByUser() {
        return isReadByUser;
    }

    public void setReadByUser(boolean readByUser) {
        isReadByUser = readByUser;
    }
}

package notifications;

import users.PullRequest;

public class PullRequestNotification implements INotification {
    private PullRequest pullRequest = null;
    private boolean isReadByUser = false;

    public boolean isReadByUser() {
        return isReadByUser;
    }

    public void setReadByUser(boolean i_ReadByUser) {
        isReadByUser = i_ReadByUser;
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }
}

package notifications;

import magit.Engine;

import java.text.SimpleDateFormat;

public class ForkNotification implements INotification {
    private final String type;
    private final String forkingUser;
    private final String forkedRepoName;
    private final String timeStamp;
    private boolean isReadByUser;
    private boolean isNotShow;

    public ForkNotification(String i_ForkedRepoName, String i_ForkingUser) {
        forkingUser = i_ForkingUser;
        forkedRepoName = i_ForkedRepoName;
        type = "fork";
        timeStamp = new SimpleDateFormat(Engine.DATE_FORMAT).format(System.currentTimeMillis());
        isNotShow = false;
    }

    public void setNotShowNotification(boolean i_IsNotShow) {
        isNotShow = true;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getForkingUser() {
        return forkingUser;
    }

    public boolean isReadByUser() {
        return isReadByUser;
    }

    public void setReadByUser(boolean i_ReadByUser) {
        isReadByUser = i_ReadByUser;
    }

    public String getForkedRepo() {
        return forkedRepoName;
    }

    public String getNotificationType() {
        return type;
    }
}

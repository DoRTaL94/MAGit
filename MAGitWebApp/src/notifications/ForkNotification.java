package notifications;

import data.structures.Repository;
import users.User;

public class ForkNotification implements INotification {
    private final String notificationType;
    private final String notificationFactorUser;
    private final String forkedRepoName;
    private boolean isReadByUser;

    public ForkNotification(String i_NotificationFactor, String i_ForkedRepo) {
        notificationFactorUser = i_NotificationFactor;
        forkedRepoName = i_ForkedRepo;
        notificationType = "Fork";
    }

    public boolean isReadByUser() {
        return isReadByUser;
    }

    public void setReadByUser(boolean i_ReadByUser) {
        isReadByUser = i_ReadByUser;
    }

    public String getNotificationFactor() {
        return notificationFactorUser;
    }

    public String getForkedRepo() {
        return forkedRepoName;
    }

    public String getNotificationType() {
        return notificationType;
    }
}

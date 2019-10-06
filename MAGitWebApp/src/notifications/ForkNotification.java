package notifications;

import data.structures.Repository;
import users.User;

public class ForkNotification implements INotification {
    private final String notificationType;
    private final String notificationFactorUser;
    private final String forkedRepoName;

    public ForkNotification(String i_NotificationFactor, String i_ForkedRepo) {
        notificationFactorUser = i_NotificationFactor;
        forkedRepoName = i_ForkedRepo;
        notificationType = "Fork";
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

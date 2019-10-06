package notifications;

import java.util.LinkedList;
import java.util.List;

public class NotificationManager {
    private final LinkedList<INotification> notifications;

    public NotificationManager() {
        notifications = new LinkedList<>();
    }

    public void addNotification(INotification i_ToAdd) {
        notifications.addFirst(i_ToAdd);
    }

    public List<INotification> getNotifications() {
        return notifications;
    }
}
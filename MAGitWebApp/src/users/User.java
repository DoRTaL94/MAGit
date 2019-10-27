package users;

import notifications.NotificationManager;

import java.util.LinkedList;
import java.util.List;

public class User {
    private final NotificationManager notificationManager = new NotificationManager();
    private final List<PullRequest> pullRequests = new LinkedList<>();
    private String name = "";
    private String password = "";

    public void setPassword(String i_Password) {
        password = i_Password;
    }

    public void setName(String i_Name) {
        name = i_Name;
    }

    public List<PullRequest> getPullRequests() {
        return pullRequests;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    @Override
    public String toString() {
        return name + ":" + password;
    }
}

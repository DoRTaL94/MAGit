package users;

import data.structures.Repository;
import notifications.NotificationManager;

import java.util.ArrayList;
import java.util.List;

public class User {
    private final NotificationManager notificationManager = new NotificationManager();
    private final List<Repository> repositories = new ArrayList<>();
    private String name = "";
    private String password = "";

    public void setPassword(String i_Password) {
        password = i_Password;
    }

    public void setName(String i_Name) {
        name = i_Name;
    }

    public void addRepo(Repository i_Repositpory) {
        repositories.add(i_Repositpory);
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public int getRepoCount() {
        return repositories.size();
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
}

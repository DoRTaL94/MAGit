package users;

import MagitExceptions.FolderInLocationAlreadyExistsException;
import MagitExceptions.RepositoryAlreadyExistsException;
import MagitExceptions.xmlErrorsException;
import javafx.beans.property.SimpleStringProperty;
import magit.Engine;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class UsersManager {
    private final Map<String, User> nameToUserMap = new HashMap<>();
    private boolean isUserLoggedIn = false;
    private User loggedInUser;

    public void addUser(String i_Name, String i_Password) {
        User user = new User();
        user.setName(i_Name);
        user.setPassword(i_Password);
        nameToUserMap.put(i_Name, user);
    }

    public void addRepo(String i_UserName, String i_XmlPath) {
        try {
            Engine.Creator.getInstance().loadRepositoryFromXml(i_XmlPath, new SimpleStringProperty());
            // לבדוק אם הרפו כבר קיים
            nameToUserMap.get(i_UserName).addRepo(Engine.Creator.getInstance().getActiveRepository());
        } catch (FileNotFoundException | FolderInLocationAlreadyExistsException | xmlErrorsException | RepositoryAlreadyExistsException ignored) {
        }
    }

    public boolean isUserExists(String i_Name) {
        return nameToUserMap.containsKey(i_Name);
    }

    public void changePassword(String i_Name, String i_NewPassword) {
        nameToUserMap.get(i_Name).setPassword(i_NewPassword);
    }

    public User getUser(String i_Name) {
        return nameToUserMap.get(i_Name);
    }

    public boolean isUserLoggedIn() {
        return  isUserLoggedIn;
    }

    public void setLoggedInUser(User i_LoggedIn) {
        isUserLoggedIn = true;
        loggedInUser = i_LoggedIn;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }
}

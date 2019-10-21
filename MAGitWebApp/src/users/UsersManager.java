package users;

import IO.FileUtilities;
import MagitExceptions.FolderInLocationAlreadyExistsException;
import MagitExceptions.RepositoryAlreadyExistsException;
import MagitExceptions.xmlErrorsException;
import javafx.beans.property.SimpleStringProperty;
import magit.Engine;
import org.apache.commons.codec.digest.DigestUtils;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.io.File;

public class UsersManager {
    private final Map<String, User> nameToUserMap = new HashMap<>();
    private boolean isUserLoggedIn = false;
    private User loggedInUser;

    public void addUser(String i_Name, String i_Password) {
        User user = new User();
        user.setName(i_Name);
        user.setPassword(i_Password);
        nameToUserMap.put(i_Name, user);
        saveToDB(user);
    }

    private void saveToDB(User i_User) {
        String authString = DigestUtils.sha1Hex(i_User.toString());
        String path = Paths.get("c:/magit-ex3", i_User.getName(), "auth.txt").toString();
        FileUtilities.createFoldersInPath(path);
        FileUtilities.WriteToFile(path, authString);
    }

    public void addRepo(String i_UserName, String i_XmlPath) throws FileNotFoundException, RepositoryAlreadyExistsException, JAXBException, xmlErrorsException, FolderInLocationAlreadyExistsException {
        Engine.Creator.getInstance().loadRepositoryFromXml(i_XmlPath, new SimpleStringProperty());
        // לבדוק אם הרפו כבר קיים
        nameToUserMap.get(i_UserName).addRepo(Engine.Creator.getInstance().getActiveRepository());
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

    public boolean loginFromDb(String i_Username, String i_Password) throws IOException {
        String authFilePath = Paths.get("c:/magit-ex3", i_Username, "auth.txt").toString();
        User user = new User();
        user.setPassword(i_Password);
        user.setName(i_Username);

        File authFile = new File(authFilePath);
        boolean success = false;

        if(authFile.exists()) {
            String authString = FileUtilities.ReadTextFromFile(authFilePath);
            success = DigestUtils.sha1Hex(user.toString()).equals(authString);

            if(success) {
                addUser(user);
            }
        }

        return success;
    }

    public String getAuthString(String i_Username, String i_Password) {
        return DigestUtils.sha1Hex(i_Username + ":" + i_Password);
    }

    private void addUser(User i_User) {
        String name = i_User.getName();

        if (!name.isEmpty() && !i_User.getPassword().isEmpty()) {
            nameToUserMap.put(name, i_User);
        }
    }
}

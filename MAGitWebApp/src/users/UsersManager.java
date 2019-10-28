package users;

import IO.FileUtilities;
import data.structures.Branch;
import data.structures.Difference;
import data.structures.Repository;
import magit.Engine;
import notifications.INotification;
import notifications.PullRequestNotification;
import org.apache.commons.codec.digest.DigestUtils;
import utils.SessionUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

public class UsersManager {
    private final Map<String, User> nameToUserMap = new HashMap<>();
    private final Map<String, Boolean> nameToLoggedInUsers = new HashMap<>();
    private final Map<String, Engine> nameToEngines = new HashMap<>();

    public void addUser(String i_Name, String i_Password) {
        User user = new User();
        user.setName(i_Name);
        user.setPassword(i_Password);
        nameToUserMap.put(i_Name, user);
        nameToEngines.put(i_Name, new Engine());
        nameToLoggedInUsers.put(i_Name, true);
        saveToDB(user);
    }

    public List<String> getUsers(String i_CurrentUser) {
        List<String> users = new ArrayList<>(nameToUserMap.keySet());
        users.remove(i_CurrentUser);
        return users;
    }

    public void logoutUser(String i_Name) {
        nameToLoggedInUsers.replace(i_Name, false);
        nameToEngines.remove(i_Name);
    }

    public Engine getEngine(String i_Name) {
        if(nameToEngines.get(i_Name) == null) {
            nameToEngines.put(i_Name, new Engine());
        }

        return nameToEngines.get(i_Name);
    }

    private void saveToDB(User i_User) {
        String authString = DigestUtils.sha1Hex(i_User.toString());
        String path = Paths.get("c:/magit-ex3", i_User.getName(), "auth.txt").toString();
        FileUtilities.createFoldersInPath(path);
        FileUtilities.WriteToFile(path, authString);
    }

    public boolean isUserExists(String i_Name) {
        boolean isExists = nameToUserMap.containsKey(i_Name);

        if(!isExists) {
            isExists = new File(Paths.get("c:/magit-ex3", i_Name).toString()).exists();
        }

        return isExists;
    }

    public void changePassword(String i_Name, String i_NewPassword) {
        nameToUserMap.get(i_Name).setPassword(i_NewPassword);
        String newAuth = getAuthString(i_Name, i_NewPassword);
        FileUtilities.WriteToFile(Paths.get("c:/magit-ex3", i_Name, "auth.txt").toString(), newAuth);
    }

    public User getUser(String i_Name) {
        return nameToUserMap.get(i_Name);
    }

    public boolean isUserLoggedIn(String i_Name) {
        return nameToLoggedInUsers.get(i_Name);
    }

    public void setLoggedInUser(String i_Name) {
        if(nameToLoggedInUsers.containsKey(i_Name)) {
            nameToLoggedInUsers.replace(i_Name, true);
        } else {
            nameToLoggedInUsers.put(i_Name, true);
        }
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

    public boolean addPullRequest(PullRequest i_PullRequest, Engine i_PushingUserEngine, Engine i_PullingUserEngine) {
        boolean isAdded = false;
        User user = nameToUserMap.get(i_PullingUserEngine.getCurrentUserName());

        if(user != null) {
            user.getPullRequests().add(i_PullRequest);

            PullRequestNotification pullRequestNotification = new PullRequestNotification();
            pullRequestNotification.setPullRequest(i_PullRequest);
            user.getNotificationManager().addNotification(pullRequestNotification);

            Repository pushingUserRepo = i_PushingUserEngine.getActiveRepository();
            String pushedBranchName = pushingUserRepo.getHeadBranch().getName();
            String pushedPointedCommitSha1 = pushingUserRepo.getHeadBranch().getPointedCommitSha1();

            Branch pushedBranch = new Branch();
            pushedBranch.setName(pushedBranchName + "-pr");
            pushedBranch.setPointedCommitSha1(pushedPointedCommitSha1);
            pushedBranch.setPullRequested(true);

            i_PullingUserEngine.putBranchFromOtherRepo(pushedBranch, i_PullRequest.getRelevantRepoName(), i_PushingUserEngine);
            List<Difference> commitDifference = i_PullingUserEngine.getCommitDifference(i_PullRequest.getRelevantRepoName(), pushedPointedCommitSha1);

            i_PullRequest.setCommitDiff(commitDifference.get(0));

            isAdded = true;
        }

        return isAdded;
    }
}

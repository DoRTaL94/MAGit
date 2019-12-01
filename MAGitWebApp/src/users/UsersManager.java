package users;

import IO.FileUtilities;
import data.structures.Branch;
import data.structures.Difference;
import data.structures.Repository;
import magit.Constants;
import magit.Engine;
import notifications.PullRequestNotification;
import org.apache.commons.codec.digest.DigestUtils;
import sun.util.resources.cldr.uk.TimeZoneNames_uk;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.io.File;

public class UsersManager {
    private final Map<String, User> nameToUserMap = new HashMap<>();
    private final Map<String, Engine> nameToEngines = new HashMap<>();
    private final Set<String> loggedInUsers = new HashSet<>();

    public void addUser(String i_Name, String i_Password) {
        User user = new User();
        user.setName(i_Name);
        user.setPassword(i_Password);
        saveToDB(user);
    }

    public List<String> getUsers(String i_CurrentUser) {
        File usersDir = new File(Constants.DB_LOCATION);
        File[] usersFolders = usersDir.listFiles();
        List<String> users = new ArrayList<>();

        if(usersFolders != null) {
            for (File folder : usersFolders) {
                users.add(folder.getName());
            }

            users.remove(i_CurrentUser);
        }

        return users;
    }

    public void logout(String i_Name) {
        nameToEngines.remove(i_Name);
        nameToUserMap.remove(i_Name);
        loggedInUsers.remove(i_Name);
    }

    public Engine getEngine(String i_Name) {
        if(nameToEngines.get(i_Name) == null) {
            nameToEngines.put(i_Name, new Engine());
            nameToEngines.get(i_Name).setCurrentUserName(i_Name);
        }

        return nameToEngines.get(i_Name);
    }

    private void saveToDB(User i_User) {
        String authString = DigestUtils.sha1Hex(i_User.toString());
        String path = Paths.get(Constants.DB_LOCATION, i_User.getName(), "auth.txt").toString();
        FileUtilities.createFoldersInPath(path);
        FileUtilities.WriteToFile(path, authString);
    }

    public boolean isUserExists(String i_Name) {
        boolean isExists = nameToUserMap.containsKey(i_Name);

        if(!isExists) {
            isExists = new File(Paths.get(Constants.DB_LOCATION, i_Name).toString()).exists();
        }

        return isExists;
    }

    public void changePassword(String i_Name, String i_NewPassword) {
        nameToUserMap.get(i_Name).setPassword(i_NewPassword);
        String newAuth = getAuthString(i_Name, i_NewPassword);
        FileUtilities.WriteToFile(Paths.get(Constants.DB_LOCATION, i_Name, "auth.txt").toString(), newAuth);
    }

    public User getUser(String i_Name) {
        return nameToUserMap.get(i_Name);
    }

    public boolean isUserLoggedIn(String i_Name) {
        return loggedInUsers.contains(i_Name);
    }

    public boolean login(String i_Username, String i_Password) throws IOException {
        boolean success = false;

        if(!nameToUserMap.containsKey(i_Username)) {
            String authFilePath = Paths.get(Constants.DB_LOCATION, i_Username, "auth.txt").toString();
            User user = new User();
            user.setPassword(i_Password);
            user.setName(i_Username);

            File authFile = new File(authFilePath);

            if (authFile.exists()) {
                String authString = FileUtilities.ReadTextFromFile(authFilePath);
                success = DigestUtils.sha1Hex(user.toString()).equals(authString);

                if (success) {
                    nameToUserMap.put(i_Username, user);
                    nameToEngines.put(i_Username, new Engine());
                    nameToEngines.get(i_Username).setCurrentUserName(i_Username);
                    loggedInUsers.add(i_Username);
                }
            }
        }

        return success;
    }

    public String getAuthString(String i_Username, String i_Password) {
        return DigestUtils.sha1Hex(i_Username + ":" + i_Password);
    }

    public boolean addPullRequest(PullRequest i_PullRequest, Engine i_PushingUserEngine, Engine i_PullingUserEngine) throws IOException {
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
            i_PullingUserEngine.copyFilesInObjectsDir(i_PullRequest.getRelevantRepoName(), pushingUserRepo.getLocationPath(), pushingUserRepo.getHeadBranch());
            isAdded = true;
        }

        return isAdded;
    }
}

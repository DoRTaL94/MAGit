package utils;

import IO.FileUtilities;
import data.structures.*;
import magit.Engine;
import notifications.INotification;
import org.apache.commons.codec.digest.DigestUtils;
import string.StringUtilities;
import users.PullRequest;
import users.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RepositoryUpdates {
    private WorkingDirectory wc;
    private Repository repository;
    private List<PullRequest> pullRequests;
    private List<INotification> notifications;
    private boolean isOpenChanges;
    private boolean isOwnRepo;

    public RepositoryUpdates(Engine i_Engine, User i_CurrentUser) {
        repository = i_Engine.getActiveRepository();

        if(repository != null) {
            isOpenChanges = !i_Engine.isWcClean(repository.getName());
            isOwnRepo = i_CurrentUser.getName().equals(repository.getOwner());
            pullRequests = i_CurrentUser.getPullRequests();
            notifications = i_CurrentUser.getNotificationManager().getNotifications();

            createWc(i_Engine);
        }
    }

    public List<PullRequest> getPullRequests() {
        return pullRequests;
    }

    public List<INotification> getNotifications() {
        return notifications;
    }

    public boolean isOwnRepo() {
        return isOwnRepo;
    }

    public void setOwnRepo(boolean ownRepo) {
        isOwnRepo = ownRepo;
    }

    public boolean getIsOpenChanges() {
        return isOpenChanges;
    }

    public Repository getRepository() {
        return repository;
    }

    private void createWc(Engine i_Engine) {
        wc = new WorkingDirectory();

        if(repository != null) {
            try {
                Folder root = createFolder(repository.getLocationPath(), i_Engine);
                String sha1 = DigestUtils.sha1Hex(root.toStringForSha1(Paths.get(repository.getLocationPath())));
                root.setIsRoot(true);
                wc.addFolder(sha1, root);
                wc.setRootSha1(sha1);
                wc.setRepoName(repository.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Folder createFolder(String i_Path, Engine i_Engine) throws IOException {
        Folder root = new Folder();
        File repoDir = new File(i_Path);
        File[] files = repoDir.listFiles();
        List<File> filesList = Arrays.stream(Objects.requireNonNull(files)).filter(file -> !file.getName().contains(".magit")).collect(Collectors.toList());

        for (File file : filesList) {
            String sha1;
            String filePath = file.getPath();

            if(file.isDirectory()) {
                Folder subFolder = createFolder(filePath, i_Engine);
                sha1 = DigestUtils.sha1Hex(subFolder.toStringForSha1(Paths.get(i_Path)));
                wc.addFolder(sha1, subFolder);
            } else {
                Blob blob = new Blob();
                blob.setText(FileUtilities.ReadTextFromFile(filePath));
                blob.setName(file.getName());
                sha1 = DigestUtils.sha1Hex(blob.toStringForSha1());
                wc.addBlob(sha1, blob);
            }

            Folder.Data data = Folder.Data.Parse(file, sha1, i_Engine.getCurrentUserName());
            root.addFile(data);
        }

        return root;
    }
}

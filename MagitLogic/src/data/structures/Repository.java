package data.structures;
import IO.FileUtilities;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Repository {
    private String locationPath;
    private Map<String, Commit> commits = null;
    private Map<String, Folder> folders = null;
    private Map<String, Blob> blobs = null;
    private Map<String, Branch> branches = null;
    private Branch headBranch = null;
    private String repoName = null;
    private String owner = null;
    private boolean isForked = false;
    private String usernameForkedFrom = null;
    private String remoteRepositoryLocation = "";

    public String getRemoteRepositoryLocation() {
        return remoteRepositoryLocation;
    }

    public void setRemoteRepositoryLocation(String i_RemoteRepositoryLocation) {
        remoteRepositoryLocation = i_RemoteRepositoryLocation;
    }

    public String getUsernameForkedFrom() {
        return usernameForkedFrom;
    }

    public void setUsernameForkedFrom(String i_UsernameForkedFrom) {
        usernameForkedFrom = i_UsernameForkedFrom;
    }

    public void setForked(boolean i_IsForked) {
        isForked = i_IsForked;
    }

    public boolean isForked() {
        return isForked;
    }

    public void setOwner(String i_Owner) {
        owner = i_Owner;
    }

    public String getOwner() { return owner; }

    public String getLocationPath() {
        return locationPath;
    }

    public void setLocationPath(String i_Path) {
        locationPath = Paths.get(i_Path).toString().toLowerCase();
    }

    public Map<String, Commit> getCommits() {
        return commits;
    }

    public Map<String, Folder> getFolders() {
        return folders;
    }

    public Map<String, Blob> getBlobs() {
        return blobs;
    }

    public Map<String, Branch> getBranches() {
        return branches;
    }

    public void setCommits(Map<String, Commit> i_Commits) {
        commits = i_Commits;
    }

    public void setFolders(Map<String, Folder> i_Folders) {
        folders = i_Folders;
    }

    public void setBlobs(Map<String, Blob> i_Blobs) {
        blobs = i_Blobs;
    }

    public void setBranches(Map<String, Branch> i_Branches) {
        branches = i_Branches;
    }

    public Branch getHeadBranch() {
        return headBranch;
    }

    public void setHeadBranch(Branch i_HeadBranch) {
        headBranch = i_HeadBranch;
    }

    public String getName() {
        return repoName;
    }

    public void setName(String i_RepoName) {
        this.repoName = i_RepoName;

        if(locationPath != null) {
            String reponamePath = Paths.get(locationPath, ".magit", "reponame.txt").toString();
            FileUtilities.WriteToFile(reponamePath, i_RepoName);
        }
    }
}
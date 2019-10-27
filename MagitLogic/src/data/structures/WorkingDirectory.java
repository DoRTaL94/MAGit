package data.structures;

import java.util.HashMap;
import java.util.Map;

public class WorkingDirectory {
    private Map<String, Folder> folders = new HashMap<>();
    private Map<String, Blob> blobs = new HashMap<>();
    private String rootSha1;
    private String repoName;

    public void addFolder(String i_Sha1, Folder i_Folder) {
        folders.put(i_Sha1, i_Folder);
    }

    public void addBlob(String i_Sha1, Blob i_Blob) {
        blobs.put(i_Sha1, i_Blob);
    }

    public Map<String, Folder> getFolders() {
        return folders;
    }

    public Map<String, Blob> getBlobs() {
        return blobs;
    }

    public void setRootSha1(String i_Sha1) {
        rootSha1 = i_Sha1;
    }

    public void setRepoName(String i_Name) {
        repoName = i_Name;
    }
}

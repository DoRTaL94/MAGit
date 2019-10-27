package data.structures;

import java.util.HashMap;
import java.util.Map;

public class DiffRepository {
    private Map<String, Folder> folders = new HashMap<>();
    private Map<String, Blob> blobs = new HashMap<>();
    private String rootSha1 = "";

    public void setRootSha1(String rootSha1) {
        this.rootSha1 = rootSha1;
    }

    public Map<String, Folder> getFolders() {
        return folders;
    }

    public Map<String, Blob> getBlobs() {
        return blobs;
    }

    public String getRootSha1() {
        return rootSha1;
    }
}

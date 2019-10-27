package data.structures;

import magit.Engine;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

public class Difference {
    private Map<String, Blob> blobs = new HashMap<>();
    private List<Folder.Data> newFiles;
    private List<Folder.Data> changedFiles;
    private List<Folder.Data> deletedFiles;

    public void addBlob(String i_Sha1, Blob i_Blob) {
        blobs.put(i_Sha1, i_Blob);
    }

    public Map<String, Blob> getBlobs() {
        return blobs;
    }

    public List<Folder.Data> getNewFiles() {
        return newFiles;
    }

    public List<Folder.Data> getChangedFiles() {
        return changedFiles;
    }

    public List<Folder.Data> getDeletedFiles() {
        return deletedFiles;
    }

    public void setNewFiles(List<Folder.Data> newFiles) {
        this.newFiles = newFiles;
    }

    public void setChangedFiles(List<Folder.Data> changedFiles) {
        this.changedFiles = changedFiles;
    }

    public void setDeletedFiles(List<Folder.Data> deletedFiles) {
        this.deletedFiles = deletedFiles;
    }
}

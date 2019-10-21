package utils;

import IO.FileUtilities;
import data.structures.Blob;
import data.structures.Folder;
import magit.Engine;

import java.io.File;

public class RepositoryManager {
    public boolean deleteFile(Folder i_Parent, File i_File, Folder.Data i_Data) {
        boolean isSuccess = false;

        if(i_File.exists()) {
            isSuccess = FileUtilities.removeFile(i_File);
        }

        return isSuccess;
    }

    public void changeBlobText(String i_Sha1, String i_Text) {
        Blob blob = Engine.Creator.getInstance().getActiveRepository().getBlobs().get(i_Sha1);
        blob.setText(i_Text);
    }

    public RepositoryUpdates getRepositoryUpdates() {
        RepositoryUpdates repositoryUpdates = new RepositoryUpdates();
        repositoryUpdates.checkForUpdates();
        return repositoryUpdates;
    }
}

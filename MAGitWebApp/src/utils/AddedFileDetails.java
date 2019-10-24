package utils;

import data.structures.Blob;
import data.structures.Folder;
import data.structures.IRepositoryFile;

import java.util.List;

public class AddedFileDetails {
    private String parentFolderSha1;
    private Folder.Data fileData;
    private Folder folder;
    private Blob blob;

    public AddedFileDetails(String i_ParentFolderSha1, Folder.Data i_FileData, IRepositoryFile i_File) {
        parentFolderSha1 = i_ParentFolderSha1;
        fileData = i_FileData;

        if(i_File instanceof Folder) {
            folder = (Folder) i_File;
        } else {
            blob = (Blob) i_File;
        }
    }
}

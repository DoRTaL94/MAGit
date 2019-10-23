package utils;

import data.structures.IRepositoryFile;

import java.util.List;

public class AddedFileDetails {
    private List<String> prevFolders;
    private IRepositoryFile file;
    private String sha1;
    private String type;

    public AddedFileDetails(List<String> i_PrevFolders, IRepositoryFile i_File, String i_Sha1, String i_Type) {
        prevFolders = i_PrevFolders;
        file = i_File;
        sha1 = i_Sha1;
        type = i_Type;
    }
}

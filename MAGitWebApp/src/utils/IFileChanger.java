package utils;

import data.structures.Folder;
import java.io.File;

@FunctionalInterface
public interface IFileChanger {
    boolean apply(Folder i_Parent, File i_File, Folder.Data i_Data);
}

package utils;

import data.structures.Folder;
import java.io.File;

@FunctionalInterface
public interface IFileChanger {
    boolean apply(File i_File, Folder.Data i_Data);
}

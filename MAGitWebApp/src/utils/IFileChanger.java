package utils;

import data.structures.Folder;
import magit.Engine;

import java.io.File;

@FunctionalInterface
public interface IFileChanger {
    boolean apply(Engine i_Engine, File i_File, Folder.Data i_Data);
}

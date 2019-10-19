package data.structures;

import IO.FileUtilities;
import magit.Engine;
import string.StringUtilities;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Folder implements IRepositoryFile {
    private final LinkedList<Data> files = new LinkedList<>();
    private boolean isRoot = false;

    public final LinkedList<Data> getFiles() {
        return files;
    }

    public void addFile(Data i_Data) {
        files.add(i_Data);
        files.sort(Folder.Data::compare);
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setIsRoot(boolean i_IsRoot) {
        isRoot = i_IsRoot;
    }

    public String toStringForSha1(Path i_FolderPath) {
        StringBuilder sb = new StringBuilder();
        int filesCount = files.size();

        for(int data = 0; data < filesCount; data++) {
            sb.append(Paths.get(i_FolderPath.toString(), files.get(data).toStringForSha1()).toString().toLowerCase());

            if(data < filesCount - 1) {
                sb.append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int filesCount = files.size();

        for(int i = 0; i < filesCount; i++) {
            sb.append(files.get(i).toString());

            if(i+1 < filesCount){
                sb.append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    public static Folder parse(File i_FolderZippedFile) throws IOException {
        Folder newFolder = new Folder();
        String rootFolderContent;

        rootFolderContent = FileUtilities.UnzipFile(i_FolderZippedFile.getPath());

        List<String> rootFolderLines = StringUtilities.getLines(rootFolderContent);

        for(String line: rootFolderLines) {
            String[] parts = line.split(";");
            Folder.Data fileInFolderData = new Folder.Data();
            fileInFolderData.setName(parts[0]);
            fileInFolderData.setSHA1(parts[1]);
            fileInFolderData.setFileType(parts[2].equals("blob")? eFileType.BLOB : eFileType.FOLDER);
            fileInFolderData.setLastChanger(parts[3]);
            fileInFolderData.setlastUpdate(parts[4]);
            newFolder.addFile(fileInFolderData);
        }

        return newFolder;
    }

    public static class Data {
        private String name;
        private String sha1;
        private eFileType type;
        private String lastChanger;
        private String lastUpdate;
        private long creationTimeMillis;

        @Override
        public String toString(){
            return String.format("%s;%s;%s;%s;%s", name, sha1, type.toString().toLowerCase(), lastChanger, lastUpdate);
        }

        public String toStringForSha1() {
            return String.format("%s;%s;%s", name, sha1, type.toString().toLowerCase());
        }

        public String getName() {
            return name;
        }

        public void setName(String i_Name) {
            name = i_Name;
        }

        public String getSHA1() {
            return sha1;
        }

        public void setSHA1(String i_SHA1) {
            sha1 = i_SHA1;
        }

        public eFileType getFileType() {
            return type;
        }

        public void setFileType(eFileType i_FileType) {
            type = i_FileType;
        }

        public String getLastChanger() {
            return lastChanger;
        }

        public void setLastChanger(String i_LastChanger) {
            lastChanger = i_LastChanger;
        }

        public String getlastUpdate() {
            return lastUpdate;
        }

        public void setlastUpdate(String i_lastUpdate) {
            lastUpdate = i_lastUpdate;
        }

        public int compare(Data i_Data) {
            return name.compareTo(i_Data.getName());
        }

        public void setCreationTimeMillis(long i_Millis) {
            creationTimeMillis = i_Millis;
        }

        public long getCreationTimeMillis() { return creationTimeMillis; }

        public static Folder.Data Parse(File i_File, String i_Sha1) {
            Folder.Data data = new Folder.Data();
            boolean isFolder = i_File.isDirectory();

            data.setFileType(isFolder ? eFileType.FOLDER : eFileType.BLOB);
            data.setName(i_File.getName());
            data.setCreationTimeMillis(i_File.lastModified());
            data.setlastUpdate(new SimpleDateFormat(Engine.DATE_FORMAT).format(new Date(i_File.lastModified())));
            data.setLastChanger(Engine.Creator.getInstance().getCurrentUserName());
            data.setSHA1(i_Sha1);

            return data;
        }
    }
}

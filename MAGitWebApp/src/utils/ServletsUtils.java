package utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import data.structures.Folder;
import data.structures.Repository;
import data.structures.eFileType;
import magit.Engine;
import users.PullRequestsManagerServlet;
import users.UsersManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.io.File;
import java.util.Map;

public class ServletsUtils {
    private static final String PR_MANAGER_ATTRIBUTE_NAME = "prManager";
    private static final String USER_MANAGER_ATTRIBUTE_NAME = "usersManager";
    private static final Object userManagerLock = new Object();

    public static UsersManager getUsersManager(ServletContext i_ServletContext) {

        synchronized (userManagerLock) {
            if (i_ServletContext.getAttribute(USER_MANAGER_ATTRIBUTE_NAME) == null) {
                i_ServletContext.setAttribute(USER_MANAGER_ATTRIBUTE_NAME, new UsersManager());
            }
        }

        return (UsersManager) i_ServletContext.getAttribute(USER_MANAGER_ATTRIBUTE_NAME);
    }

    public static PullRequestsManagerServlet getPrManager(ServletContext i_ServletContext) {

        synchronized (userManagerLock) {
            if (i_ServletContext.getAttribute(PR_MANAGER_ATTRIBUTE_NAME) == null) {
                i_ServletContext.setAttribute(PR_MANAGER_ATTRIBUTE_NAME, new PullRequestsManagerServlet());
            }
        }

        return (PullRequestsManagerServlet) i_ServletContext.getAttribute(PR_MANAGER_ATTRIBUTE_NAME);
    }

    public static Folder.Data getFile(Folder i_Folder, String i_FileSha1) {
        Folder.Data res = null;

        for(Folder.Data file: i_Folder.getFiles()) {
            if(file.getSHA1().equals(i_FileSha1)) {
                res = file;
                break;
            }
        }

        return res;
    }

    public static List<String> getReqData(HttpServletRequest request) {
        Gson gson = new Gson();
        String prevFolders = request.getParameter("data");
        return gson.fromJson(prevFolders, new TypeToken<List<String>>(){}.getType());
    }

    // data: length - 1, file name: length - 2 file sha1: length - 3, parent folder sha1: length - 4
    public static boolean applyOnDbFile(Engine i_Engine, List<String> i_ReqData, IFileChanger i_ApplyOnFile) {
        boolean res;
        int reqDataSize             = i_ReqData.size();
        String filename             = i_ReqData.get(reqDataSize - 2);
        String filesha1             = i_ReqData.get(reqDataSize - 3);
        String parentFolderSha1     = i_ReqData.get(reqDataSize - 4);
        Repository repository       = i_Engine.getActiveRepository();
        Map<String, Folder> folders = repository.getFolders();


        String parentPath = getPathFromFolderNamesList(i_ReqData, i_Engine, reqDataSize - 4).toString();
        Folder parent = folders.get(parentFolderSha1);
        Folder.Data fileData;

        if(parent == null) {
            fileData = new Folder.Data();
            fileData.setName(filename);
            fileData.setLastChanger(i_Engine.getCurrentUserName());
            fileData.setlastUpdate(new SimpleDateFormat(Engine.DATE_FORMAT).format(System.currentTimeMillis()));
            fileData.setCreationTimeMillis(System.currentTimeMillis());
            fileData.setSHA1(filesha1);
            fileData.setFileType(new File(Paths.get(parentPath, filename).toString()).isDirectory() ? eFileType.FOLDER : eFileType.BLOB);
        } else {
            fileData = ServletsUtils.getFile(parent, filesha1);
        }

        File child = new File(Paths.get(parentPath, fileData.getName()).toString());
        res = i_ApplyOnFile.apply(i_Engine, child, fileData);

        return res;
    }

    public static Path getPathFromFolderNamesList(List<String> i_Names, Engine i_Engine, int i_CountOfNamesToIncludeInPath) {
        Path path = Paths.get("c:/magit-ex3", i_Engine.getCurrentUserName(), "repositories");

        for(int name = 0; name < i_CountOfNamesToIncludeInPath ; name++) {
            path = Paths.get(path.toString(), i_Names.get(name));
        }

        return path;
    }
}

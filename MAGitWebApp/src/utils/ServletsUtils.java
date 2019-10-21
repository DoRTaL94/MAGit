package utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import data.structures.Folder;
import data.structures.Repository;
import data.structures.eFileType;
import magit.Engine;
import users.UsersManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.nio.file.Paths;
import java.util.List;
import java.io.File;
import java.util.Map;

public class ServletsUtils {
    private static final String USER_MANAGER_ATTRIBUTE_NAME = "usersManager";
    private static final String REPOSITORY_MANAGER_ATTRIBUTE_NAME = "repositoryManager";
    private static final Object userManagerLock = new Object();
    private static final Object repositoryManagerLock = new Object();

    public static UsersManager getUsersManager(ServletContext i_ServletContext) {

        synchronized (userManagerLock) {
            if (i_ServletContext.getAttribute(USER_MANAGER_ATTRIBUTE_NAME) == null) {
                i_ServletContext.setAttribute(USER_MANAGER_ATTRIBUTE_NAME, new UsersManager());
            }
        }

        return (UsersManager) i_ServletContext.getAttribute(USER_MANAGER_ATTRIBUTE_NAME);
    }

    public static RepositoryManager getRepositoryManager(ServletContext i_ServletContext) {

        synchronized (repositoryManagerLock) {
            if (i_ServletContext.getAttribute(REPOSITORY_MANAGER_ATTRIBUTE_NAME) == null) {
                i_ServletContext.setAttribute(REPOSITORY_MANAGER_ATTRIBUTE_NAME, new RepositoryManager());
            }
        }

        return (RepositoryManager) i_ServletContext.getAttribute(REPOSITORY_MANAGER_ATTRIBUTE_NAME);
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

    public static boolean applyOnDbFile(String i_UserName, List<String> i_ReqData, IFileChanger i_ApplyOnFile) {
        boolean res = false;
        int reqDataSize = i_ReqData.size();

        Repository repository = Engine.Creator.getInstance().getActiveRepository();
        Map<String, Folder> folders = repository.getFolders();
        String path = Paths.get("c:/magit-ex3", i_UserName, "repositories", repository.getName()).toString();
        String rootSha1 = repository.getCommits().get(repository.getHeadBranch().getPointedCommitSha1()).getRootFolderSha1();
        Folder root = folders.get(rootSha1);

        if (i_ReqData.get(0).equals(rootSha1)) {
            for (int i = 1; i < reqDataSize - 1; i++) {
                Folder.Data fileData = ServletsUtils.getFile(root, i_ReqData.get(i));

                if (fileData != null) {
                    path = Paths.get(path, fileData.getName()).toString();

                    if (fileData.getFileType().equals(eFileType.FOLDER)) {
                        if(i + 1  == reqDataSize - 1) {
                            res = i_ApplyOnFile.apply(root, new File(path), fileData);
                        }

                        root = folders.get(fileData.getSHA1());
                    } else {
                        res = i_ApplyOnFile.apply(root, new File(path), fileData);
                    }
                }
            }
        }

        return res;
    }
}

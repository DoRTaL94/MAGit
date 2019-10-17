package servlets;

import IO.FileUtilities;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import data.structures.*;
import magit.Engine;
import utils.ServletsUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.io.File;

@WebServlet("/pages/repository/update-blob")
public class UpdateBlobServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        List<String> reqData = getReqData(request);
        int reqDataSize = reqData.size();

        Repository repository = Engine.Creator.getInstance().getActiveRepository();
        Map<String, Folder> folders = repository.getFolders();
        Map<String, Blob> blobs = repository.getBlobs();
        String changedBlobPath = Paths.get("c:/magit-ex3",
                ServletsUtils.getUsersManager(getServletContext()).getLoggedInUser().getName(),
                "repositories", repository.getName()).toString();
        String rootSha1 = repository.getCommits().get(repository.getHeadBranch().getPointedCommitSha1()).getRootFolderSha1();
        Folder root = folders.get(rootSha1);

        if(reqData.get(0).equals(rootSha1)) {
            for (int i = 1; i < reqDataSize - 1; i++) {
                Folder.Data file = getFile(root, reqData.get(i));

                if (file != null) {
                    changedBlobPath = Paths.get(changedBlobPath, file.getName()).toString();

                    if (file.getFileType().equals(eFileType.FOLDER)) {
                        root = folders.get(file.getSHA1());
                    } else {
                        if (new File(changedBlobPath).exists()) {
                            FileUtilities.WriteToFile(changedBlobPath, reqData.get(i + 1));
                            blobs.get(file.getSHA1()).setText(reqData.get(i + 1));
                        }
                    }
                }
            }
        }
    }

    private Folder.Data getFile(Folder i_Folder, String i_FileSha1) {
        Folder.Data res = null;

        for(Folder.Data file: i_Folder.getFiles()) {
            if(file.getSHA1().equals(i_FileSha1)) {
                res = file;
                break;
            }
        }

        return res;
    }

    private List<String> getReqData(HttpServletRequest request) {
        Gson gson = new Gson();
        String prevFolders = request.getParameter("prevFolders");
        return gson.fromJson(prevFolders, new TypeToken<List<String>>(){}.getType());
    }
}

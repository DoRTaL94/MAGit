package servlets;

import IO.FileUtilities;
import data.structures.Folder;
import magit.Engine;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;
import java.io.File;

@WebServlet("/pages/update-file-name")
public class UpdateFileNameServlet extends HttpServlet {
    private List<String> reqData = null;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = SessionUtils.getUsername(request);
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);
        reqData = ServletsUtils.getReqData(request);
        boolean isSuccess = ServletsUtils.applyOnDbFile(engine, reqData, this::changeFileName);

        if(!isSuccess) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("failed to update");
        } else {
            FileUtilities.WriteToFile(Paths.get(engine.getActiveRepository().getLocationPath(), ".magit", "oldCommit.txt").toString(),
                    engine.getActiveRepository().getHeadBranch().getPointedCommitSha1());
        }
    }

    private boolean changeFileName(Engine i_Engine, File i_File, Folder.Data i_Data) {
        boolean res = false;
        int reqDataSize = reqData.size();
        String newName = reqData.get(reqDataSize - 1);
        File fileWithNewName = new File(i_File.getParent(), newName);

        if (!fileWithNewName.exists()) {
            res = i_File.renameTo(fileWithNewName);
        }

        return res;
    }
}

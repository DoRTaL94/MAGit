package servlets;

import IO.FileUtilities;
import data.structures.*;
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

@WebServlet("/pages/update-blob")
public class UpdateBlobServlet extends HttpServlet {
    private List<String> reqData = null;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = SessionUtils.getUsername(request);
        String userToSendRepo = SessionUtils.getUserRepo(request);
        Engine engine = null;
        boolean isSuccess = false;

        if(username.equals(userToSendRepo)) {
            engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(userToSendRepo == null ? username : userToSendRepo);
            reqData = ServletsUtils.getReqData(request);
            isSuccess = ServletsUtils.applyOnDbFile(engine, reqData, this::changeBlobContent);
        }

        if(!isSuccess) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("failed to update");
        } else if(engine != null) {
            FileUtilities.WriteToFile(Paths.get(engine.getActiveRepository().getLocationPath(), ".magit", "oldCommit.txt").toString(),
                    engine.getActiveRepository().getHeadBranch().getPointedCommitSha1());
        } else {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("Not user's repository");
        }
    }

    private boolean changeBlobContent(Engine i_Engine, File i_Blob, Folder.Data i_Data) {
        boolean res =  false;

        if(i_Blob.exists()) {
            int reqDataSize = reqData.size();

            FileUtilities.WriteToFile(i_Blob.toPath().toString(), reqData.get(reqDataSize - 1));
            res = true;
        }

        return res;
    }
}

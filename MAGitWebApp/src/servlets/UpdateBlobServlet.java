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
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.io.File;

@WebServlet("/pages/update-blob")
public class UpdateBlobServlet extends HttpServlet {
    private List<String> reqData = null;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        reqData = ServletsUtils.getReqData(request);
        boolean isSuccess = ServletsUtils.applyOnDbFile(ServletsUtils.getUsersManager(getServletContext()).getLoggedInUser().getName(), reqData, this::changeBlobContent);

        if(!isSuccess) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("failed to update");
        }
    }

    private boolean changeBlobContent(Folder i_Parent, File i_Blob, Folder.Data i_Data) {
        boolean res =  false;

        if(i_Blob.exists()) {
            Repository repository = Engine.Creator.getInstance().getActiveRepository();
            Map<String, Blob> blobs = repository.getBlobs();
            int reqDataSize = reqData.size();

            FileUtilities.WriteToFile(i_Blob.toPath().toString(), reqData.get(reqDataSize - 1));
            blobs.get(reqData.get(reqDataSize - 2)).setText(reqData.get(reqDataSize - 1));
            res = true;
        }

        return res;
    }
}

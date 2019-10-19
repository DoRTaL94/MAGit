package servlets;

import data.structures.Folder;
import utils.ServletsUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.io.File;

@WebServlet("/pages/update-file-name")
public class UpdateFileNameServlet extends HttpServlet {
    private List<String> reqData = null;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        reqData = ServletsUtils.getReqData(request);
        boolean isSuccess = ServletsUtils.applyOnDbFile(ServletsUtils.getUsersManager(getServletContext()).getLoggedInUser().getName(), reqData, this::changeFileName);

        if(!isSuccess) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("failed to update");
        }
    }

    private boolean changeFileName(Folder i_Parent, File i_File, Folder.Data i_Data) {
        boolean res = false;
        int reqDataSize = reqData.size();
        String newName = reqData.get(reqDataSize - 1);
        File fileWithNewName = new File(i_File.getParent(), newName);

        if (!fileWithNewName.exists()) {
            i_File.renameTo(fileWithNewName);
            i_Data.setName(newName);
            res = true;
        }

        return res;
    }
}

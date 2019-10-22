package servlets;

import IO.FileUtilities;
import data.structures.Folder;
import magit.Engine;
import users.UsersManager;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.io.File;

@WebServlet("/pages/delete-file")
public class DeleteFileServlet extends HttpServlet {

    private List<String> reqData = null;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = SessionUtils.getUsername(request);
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);

        boolean isSuccess = ServletsUtils.applyOnDbFile(engine, ServletsUtils.getReqData(request), this::deleteFile);

        if(!isSuccess) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("failed to delete");
        }
    }

    private boolean deleteFile(Engine i_Engine, Folder i_Parent, File i_File, Folder.Data i_Data) {
        boolean isSuccess = false;

        if(i_File.exists()) {
            isSuccess = FileUtilities.removeFile(i_File);
        }

        return isSuccess;
    }
}

package servlets;

import IO.FileUtilities;
import data.structures.Blob;
import data.structures.Folder;
import data.structures.eFileType;
import magit.Engine;
import users.UsersManager;
import utils.RepositoryManager;
import utils.ServletsUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.io.File;
import java.util.Map;

@WebServlet("/pages/delete-file")
public class DeleteFileServlet extends HttpServlet {

    private List<String> reqData = null;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RepositoryManager repositoryManager = ServletsUtils.getRepositoryManager(getServletContext());
        UsersManager usersManager = ServletsUtils.getUsersManager(getServletContext());
        boolean isSuccess = ServletsUtils.applyOnDbFile(usersManager.getLoggedInUser().getName(),
                ServletsUtils.getReqData(request), repositoryManager::deleteFile);

        if(!isSuccess) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("failed to delete");
        }
    }
}

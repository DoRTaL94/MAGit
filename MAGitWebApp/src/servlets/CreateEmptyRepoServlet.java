package servlets;

import IO.FileUtilities;
import MagitExceptions.FolderInLocationAlreadyExistsException;
import MagitExceptions.RepositoryAlreadyExistsException;
import magit.Constants;
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

@WebServlet("/pages/create-empty-repo")
public class CreateEmptyRepoServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String username = SessionUtils.getUsername(request);
        String reponame = request.getParameter("reponame");
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);

        if(!engine.getRepositories().containsKey(reponame)) {
            try {
                FileUtilities.createFoldersInPath(Paths.get(Constants.DB_LOCATION, username, "repositories").toString());
                engine.createRepositoryAndFiles(reponame,
                        Paths.get(Constants.DB_LOCATION, username, "repositories", reponame).toString());
                out.print("Repository created successfully.");
            } catch (RepositoryAlreadyExistsException | FolderInLocationAlreadyExistsException e) {
                e.printStackTrace();
            }
        } else {
            out.print("Repository with the same name already exists.");
        }
    }
}

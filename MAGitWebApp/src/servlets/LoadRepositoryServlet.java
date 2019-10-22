package servlets;

import IO.FileUtilities;
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
import java.io.File;

@WebServlet("/pages/load-repository")
public class LoadRepositoryServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = SessionUtils.getUsername(request);
        String auth = request.getParameter("auth");
        String authFilePath = Paths.get("c:/magit-ex3", username, "auth.txt").toString();
        File authFile = new File(authFilePath);

        if(authFile.exists()) {
            String savedAuth = FileUtilities.ReadTextFromFile(authFilePath);

            if(savedAuth.equals(auth)) {
                String recentPath = Paths.get("c:/magit-ex3", username, "repositories", "recent.txt").toString();
                File recentRepoFile = new File(recentPath);

                if(recentRepoFile.exists()) {
                    Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);
                    String recentRepoName = FileUtilities.ReadTextFromFile(recentPath);
                    String repoPath = Paths.get("c:/magit-ex3", username, "repositories", recentRepoName).toString();
                    engine.loadDataFromRepository(repoPath);
                    engine.getActiveRepository().setOwner(username);
                    engine.setCurrentUserName(username);
                }

                response.setContentType("text/html");
                PrintWriter out = response.getWriter();
                out.print("success");
                out.flush();
            }
        }
    }
}

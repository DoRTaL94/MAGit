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
import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
                String repositoriesPath = Paths.get("c:/magit-ex3", username, "repositories").toString();
                File repositoriesDir = new File(repositoriesPath);

                if(repositoriesDir.exists()) {
                    List<String> repositories = Arrays.stream(Objects.requireNonNull(repositoriesDir.listFiles())).map(file -> file.toPath().toString()).collect(Collectors.toList());
                    Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);

                    for(String path: repositories) {
                        engine.loadDataFromRepository(path);
                        engine.getActiveRepository().setOwner(username);
                        engine.setCurrentUserName(username);
                    }
                }

                response.setContentType("text/html");
                PrintWriter out = response.getWriter();
                out.print("success");
                out.flush();
            }
        }
    }
}

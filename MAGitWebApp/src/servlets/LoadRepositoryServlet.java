package servlets;

import IO.FileUtilities;
import data.structures.Repository;
import magit.Constants;
import magit.Engine;
import string.StringUtilities;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@WebServlet("/pages/load-repository")
public class LoadRepositoryServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = SessionUtils.getUsername(request);
        request.getSession(true).setAttribute("userRepo", username);
        String auth = SessionUtils.getAuth(request);
        String authFilePath = Paths.get(Constants.DB_LOCATION, username, "auth.txt").toString();
        File authFile = new File(authFilePath);

        if(authFile.exists()) {
            String savedAuth = FileUtilities.ReadTextFromFile(authFilePath);

            if(savedAuth.equals(auth)) {
                String repositoriesPath = Paths.get(Constants.DB_LOCATION, username, "repositories").toString();
                File repositoriesDir = new File(repositoriesPath);

                if(repositoriesDir.exists()) {
                    List<String> repositories = Arrays.stream(Objects.requireNonNull(repositoriesDir.listFiles())).map(file -> file.toPath().toString()).collect(Collectors.toList());
                    Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);

                    for(String path: repositories) {
                        engine.loadDataFromRepository(path);
                        Repository activeRepository = engine.getActiveRepository();
                        activeRepository.setOwner(username);
                        engine.setCurrentUserName(username);
                        activeRepository.setForked(!engine.checkIfOwnRepo(engine.getActiveRepositoryName()));

                        if(activeRepository.isForked()) {
                            String detailsPath = Paths.get(activeRepository.getLocationPath(), ".magit", "details.txt").toString();
                            String details = FileUtilities.ReadTextFromFile(detailsPath);
                            String remotePath = StringUtilities.getLines(details).get(1);
                            File repoFile = new File(remotePath);
                            activeRepository.setRemoteRepositoryLocation(remotePath);
                            String userForkedFromName = new File(new File(repoFile.getParent()).getParent()).getName();
                            activeRepository.setUsernameForkedFrom(userForkedFromName);
                        }
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

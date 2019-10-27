package servlets;

import com.google.gson.Gson;
import data.structures.Repository;
import magit.Engine;
import utils.RepositoryDetails;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet("/pages/repositories")
public class RepositoriesServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = SessionUtils.getUsername(request);
        String userToSendRepos = SessionUtils.getUserRepo(request);
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(userToSendRepos == null ? username : userToSendRepos);

        Map<String, Repository> repositories = engine.getRepositories();
        Gson gson = new Gson();
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        if(repositories != null) {
            List<RepositoryDetails> details = new ArrayList<>();

            for(Map.Entry<String, Repository> entry: repositories.entrySet()) {
                details.add(new RepositoryDetails(entry.getValue()));
            }

            String toOut = gson.toJson(details);
            out.print(toOut);
        } else {
            Repository repository = new Repository();
            repository.setOwner(userToSendRepos == null ? username : userToSendRepos);
            String toOut = gson.toJson(new RepositoryDetails(repository));
            out.print(toOut);
        }
    }
}

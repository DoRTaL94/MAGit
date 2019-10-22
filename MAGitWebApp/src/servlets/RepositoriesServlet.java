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
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);
        Map<String, Repository> repositories = engine.getRepositories();

        if(repositories != null) {
            List<RepositoryDetails> details = new ArrayList<>();

            for(Map.Entry<String, Repository> entry: repositories.entrySet()) {
                details.add(new RepositoryDetails(entry.getValue()));
            }

            Gson gson = new Gson();
            String toOut = gson.toJson(details);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.print(toOut);
        }
    }
}

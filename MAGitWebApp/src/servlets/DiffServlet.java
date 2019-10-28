package servlets;

import com.google.gson.Gson;
import data.structures.Difference;
import data.structures.Repository;
import magit.Engine;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/pages/difference")
public class DiffServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = SessionUtils.getUsername(request);
        String userToSendRepo = SessionUtils.getUserRepo(request);

        if(username.equals(userToSendRepo)) {
            String commitSha1 = request.getParameter("commit");

            if(commitSha1 != null && !commitSha1.isEmpty()) {
                Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(userToSendRepo);
                Repository repository = engine.getActiveRepository();

                if (repository != null) {
                    Gson gson = new Gson();
                    String diff = gson.toJson(engine.getCommitDifference(engine.getActiveRepositoryName(), commitSha1));
                    response.setContentType("application/json;charset=UTF-8");
                    PrintWriter out = response.getWriter();
                    out.print(diff);
                } else {
                    response.setContentType("text/html");
                    PrintWriter out = response.getWriter();
                    out.print("User has no repositories.");
                }
            } else {
                response.setContentType("text/html");
                PrintWriter out = response.getWriter();
                out.print("Commit sha1 not valid");
            }
        } else {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("Not user's repository");
        }
    }
}

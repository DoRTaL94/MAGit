package servlets;

import com.google.gson.Gson;
import magit.Engine;
import users.UsersManager;
import utils.RepositoryUpdates;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/pages/update-repo")
public class UpdateRepoServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String username = SessionUtils.getUsername(request);
        String userToSendRepo = SessionUtils.getUserRepo(request);
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(userToSendRepo == null ? username : userToSendRepo);

        try {
            if (engine.getActiveRepository() == null) {
                out.print("User has no repositories.");
            } else {
                Gson gson = new Gson();
                UsersManager usersManager = ServletsUtils.getUsersManager(getServletContext());
                engine.checkout(engine.getActiveRepositoryName(), engine.getActiveRepository().getHeadBranch().getName(), true);
                RepositoryUpdates repositoryUpdates = new RepositoryUpdates(engine, usersManager.getUser(username));
                out.print(gson.toJson(repositoryUpdates));
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        out.flush();
    }
}

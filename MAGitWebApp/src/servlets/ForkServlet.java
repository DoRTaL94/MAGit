package servlets;

import MagitExceptions.CollaborationException;
import data.structures.Repository;
import magit.Engine;
import notifications.ForkNotification;
import users.User;
import users.UsersManager;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

@WebServlet("/pages/fork")
public class ForkServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String username = SessionUtils.getUsername(request);
        String userToSendRepo = SessionUtils.getUserRepo(request);
        Engine otherEngine = ServletsUtils.getUsersManager(getServletContext()).getEngine(userToSendRepo == null ? username : userToSendRepo);
        Engine userEngine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);

        String repoName = request.getParameter("repository");
        Repository repository = otherEngine.getRepository(repoName);

        if(repository != null) {
            String newName = repository.getName();
            String newPath = Paths.get("c:/magit-ex3", username, "repositories", newName).toString();
            try {
                userEngine.Clone(newName, newPath, repository.getLocationPath());
                userEngine.getRepository(newName).setOwner(username);
                userEngine.getRepository(newName).setForked(true);
                userEngine.getRepository(newName).setUsernameForkedFrom(userToSendRepo);
                userEngine.setCurrentUserName(username);

                addForkNotification(username, userToSendRepo, newName);

                request.setAttribute("user", "");
                request.setAttribute("current", true);
                RequestDispatcher requestDispatcher = request.getRequestDispatcher("set-user-repo");
                requestDispatcher.forward(request, response);
            } catch (IOException | CollaborationException e) {
                out.print(e.getMessage());
            }
        } else {
            out.print("repository named " + repoName + " not exists");
        }
    }

    private void addForkNotification(String i_ForkingUserName, String i_ForkedFromUserName, String i_RepositoryName) {
        UsersManager usersManager = ServletsUtils.getUsersManager(getServletContext());
        User forkedFromUser = usersManager.getUser(i_ForkedFromUserName);

        ForkNotification forkNotification = new ForkNotification(i_RepositoryName, i_ForkingUserName);
        forkedFromUser.getNotificationManager().addNotification(forkNotification);
    }
}

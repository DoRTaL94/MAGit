package servlets;

import MagitExceptions.PointedCommitEmptyException;
import data.structures.Branch;
import data.structures.Repository;
import magit.Engine;
import notifications.PullRequestNotification;
import users.PullRequest;
import users.User;
import users.UsersManager;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/pages/pull-request")
public class PullRequestServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        UsersManager usersManager = ServletsUtils.getUsersManager(getServletContext());

        String username = SessionUtils.getUsername(request);
        String otherUsername = request.getParameter("user");

        Engine userEngine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);
        Engine otherEngine = ServletsUtils.getUsersManager(getServletContext()).getEngine(otherUsername);
        PullRequest pullRequest = ServletsUtils.getPrManager(getServletContext()).createPullRequest(request, userEngine, otherEngine);

        usersManager.addPullRequest(pullRequest, userEngine, otherEngine);
    }
}

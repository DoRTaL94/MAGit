package servlets;

import magit.Engine;
import users.PullRequest;
import users.PullRequestsManagerServlet;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/pages/decline-pr")
public class DeclinePullRequestServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String username = SessionUtils.getUsername(request);
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);
        String prId = request.getParameter("id");

        PullRequestsManagerServlet prManager = ServletsUtils.getPrManager(getServletContext());
        PullRequest pullRequest = prManager.getPullRequest(Integer.parseInt(prId));

        pullRequest.setReferred(true);
        pullRequest.setApproved(false);
        pullRequest.setDeclineReason(request.getParameter("reason"));
    }
}

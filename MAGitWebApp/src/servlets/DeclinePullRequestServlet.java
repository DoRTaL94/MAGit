package servlets;

import magit.Engine;
import notifications.PrAnswerNotification;
import users.PullRequest;
import users.PullRequestsManagerServlet;
import users.User;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/pages/decline-pr")
public class DeclinePullRequestServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String prId = request.getParameter("id");

        PullRequestsManagerServlet prManager = ServletsUtils.getPrManager(getServletContext());
        PullRequest pullRequest = prManager.getPullRequest(Integer.parseInt(prId));

        pullRequest.setReferred(true);
        pullRequest.setApproved(false);
        pullRequest.setDeclineReason(request.getParameter("reason"));

        PrAnswerNotification prAnswerNotification = new PrAnswerNotification();
        prAnswerNotification.setPullRequest(pullRequest);
        User user = ServletsUtils.getUsersManager(getServletContext()).getUser(pullRequest.getRequestByUserName());
        user.getNotificationManager().addNotification(prAnswerNotification);
    }
}

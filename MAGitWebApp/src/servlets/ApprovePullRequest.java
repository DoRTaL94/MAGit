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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;

@WebServlet("/pages/approve-pr")
public class ApprovePullRequest extends HttpServlet {
    private String message = "";
    private PrintWriter out = null;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        out = response.getWriter();

        String username = SessionUtils.getUsername(request);
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);
        String prId = request.getParameter("id");

        PullRequestsManagerServlet prManager = ServletsUtils.getPrManager(getServletContext());
        PullRequest pullRequest = prManager.getPullRequest(Integer.parseInt(prId));

        pullRequest.setApproved(true);
        pullRequest.setReferred(true);
        message = pullRequest.getPullRequestMessage();
        String targetName = pullRequest.getTarget().getName();
        pullRequest.getTarget().setName(targetName + "-pr");

        engine.MergeBranches(pullRequest.getRelevantRepoName(),
                pullRequest.getBase(),
                pullRequest.getTarget(),
                this::getCommitDescriptionAction,
                ()->{},
                this::handleMergeException);

        pullRequest.getTarget().setName(targetName);

        PrAnswerNotification prAnswerNotification = new PrAnswerNotification();
        prAnswerNotification.setPullRequest(pullRequest);
        User user = ServletsUtils.getUsersManager(getServletContext()).getUser(pullRequest.getRequestByUserName());
        user.getNotificationManager().addNotification(prAnswerNotification);
    }

    private void getCommitDescriptionAction(Consumer<String> i_SetCommitDescriptionAction) {
        i_SetCommitDescriptionAction.accept(message);
    }

    private void handleMergeException(String i_Message) {
        out.print(message);
    }
}

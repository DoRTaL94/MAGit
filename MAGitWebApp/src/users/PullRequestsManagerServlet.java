package users;

import data.structures.Repository;
import magit.Engine;
import utils.SessionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class PullRequestsManagerServlet {
    private int id = 0;
    private Map<Integer, PullRequest> pullRequestMap = new HashMap<>();
    private Map<String, String> repoPathToForkedRepoPath = new HashMap<>();

    public String getForkedRepoPath(String i_RepoPath) {
        return repoPathToForkedRepoPath.get(i_RepoPath);
    }

    public PullRequest getPullRequest(int i_Id) {
        return pullRequestMap.get(i_Id);
    }

    public PullRequest createPullRequest(HttpServletRequest i_Request, Engine i_PushingUserEngine, Engine i_PullingUserEngine) {
        String username = SessionUtils.getUsername(i_Request);
        String otherUsername = i_Request.getParameter("user");
        String target = i_Request.getParameter("target");
        String base = i_Request.getParameter("base");
        String message = i_Request.getParameter("message");

        Repository userRepo = i_PushingUserEngine.getActiveRepository();
        Repository otherRepo = i_PullingUserEngine.getRepository(userRepo.getName());

        repoPathToForkedRepoPath.put(otherRepo.getLocationPath(), userRepo.getLocationPath());

        PullRequest pullRequest = new PullRequest();
        pullRequest.setBase(otherRepo.getBranches().get(base.split("/")[1]));
        pullRequest.setTarget(userRepo.getBranches().get(target));
        pullRequest.setRelevantRepoName(userRepo.getName());
        pullRequest.setPullRequestMessage(message);
        pullRequest.setRequestByUserName(username);
        pullRequest.setId(id);
        pullRequestMap.put(id++, pullRequest);

        return pullRequest;
    }
}

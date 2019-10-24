package servlets;

import IO.FileUtilities;
import MagitExceptions.CommitAlreadyExistsException;
import MagitExceptions.EmptyWcException;
import com.google.gson.Gson;
import data.structures.Commit;
import data.structures.Repository;
import magit.Engine;
import utils.RepositoryUpdates;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.nio.file.Paths;

@WebServlet("/pages/update-repo")
public class UpdateRepoServlet extends HttpServlet {

    private Engine engine;
    private String oldCommitSha1;
    private Commit oldCommit;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = SessionUtils.getUsername(request);
        engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);

        toResponse(() -> {
            try {
                response.setContentType("application/json;charset=UTF-8");
                PrintWriter out = response.getWriter();
                Gson gson = new Gson();
                RepositoryUpdates repositoryUpdates = new RepositoryUpdates(engine);

                if (repositoryUpdates.getRepository() == null) {
                    out.print("User has no repositories.");
                } else {
                    out.print(gson.toJson(repositoryUpdates));
                }

                out.flush();
            } catch (IOException e){
                e.printStackTrace();
            }
        });
    }

    private void toResponse(Runnable toExecute) throws IOException {
        updateRecentChanges();
        toExecute.run();
        restoreRecentChanges();
    }

    private void updateRecentChanges() throws IOException {
        Repository repository = engine.getActiveRepository();
        File oldCommitFile = new File(Paths.get(repository.getLocationPath(), ".magit", "oldCommit.txt").toString());

        if(oldCommitFile.exists()) {
            oldCommitSha1 = FileUtilities.ReadTextFromFile(oldCommitFile.getPath());
            oldCommit = repository.getCommits().get(oldCommitSha1);

            try {
                engine.commit(oldCommit.getMessage(), null);
                String newCommitSha1 = repository.getHeadBranch().getPointedCommitSha1();
                Commit newCommit = repository.getCommits().get(newCommitSha1);

                repository.getCommits().replace(oldCommitSha1, oldCommit, newCommit);
                repository.getCommits().remove(newCommitSha1);
                repository.getHeadBranch().setPointedCommitSha1(oldCommitSha1);
            } catch (EmptyWcException | CommitAlreadyExistsException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void restoreRecentChanges() {
        if(oldCommitSha1 != null) {
            Repository repository = engine.getActiveRepository();
            String commitSha1 = repository.getHeadBranch().getPointedCommitSha1();
            Commit newCommit = repository.getCommits().get(commitSha1);
            repository.getCommits().replace(commitSha1, newCommit, oldCommit);
        }
    }
}

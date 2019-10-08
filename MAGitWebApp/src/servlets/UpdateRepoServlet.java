package servlets;

import com.google.gson.Gson;
import data.structures.Commit;
import data.structures.Folder;
import data.structures.IRepositoryFile;
import data.structures.Repository;
import magit.Engine;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/pages/repository/update_repo")
public class UpdateRepoServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Engine engine = Engine.Creator.getInstance();
        Repository repository = engine.getActiveRepository();
        String currentCommitSha1 = repository.getHeadBranch().getPointedCommitSha1();
        Commit currCommit = repository.getCommits().get(currentCommitSha1);
        Folder root = repository.getFolders().get(currCommit.getRootFolderSHA1());

        response.setContentType("application/json;charset=UTF-8");
        Gson gson = new Gson();
        String toOut = gson.toJson(new IRepositoryFile[] { currCommit, root });
        PrintWriter out = response.getWriter();
        out.print(toOut);
        out.flush();
    }
}

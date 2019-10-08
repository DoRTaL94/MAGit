package servlets;

import com.google.gson.Gson;
import data.structures.Commit;
import data.structures.Folder;
import data.structures.IRepositoryFile;
import data.structures.Repository;
import magit.Engine;
import users.UsersManager;
import utils.ServletsUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/pages/repository/update_repo")
public class UpdateRepoServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        try {
            Repository repository = Engine.Creator.getInstance().getActiveRepository();
            Commit currCommit = repository.getCommits().get(repository.getHeadBranch().getPointedCommitSha1());

            currCommit.getSha1(); // updates the sha1
            repository.setOwner(ServletsUtils.getUsersManager(getServletContext()).getLoggedInUser().getName());

            out.print(gson.toJson(repository));
            out.flush();
        } catch (NullPointerException e) {
            out.print("User has no repositories.");
        }
    }
}

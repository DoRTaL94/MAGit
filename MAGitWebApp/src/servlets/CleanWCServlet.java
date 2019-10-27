package servlets;

import magit.Engine;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/pages/clean-open-changes")
public class CleanWCServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String username = SessionUtils.getUsername(request);
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);
        String headBranchName = engine.getActiveRepository().getHeadBranch().getName();

        try {
            engine.checkout(engine.getActiveRepositoryName(), headBranchName ,true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

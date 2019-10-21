package servlets;

import magit.Engine;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/pages/clean-open-changes")
public class CleanWCServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String headBranchName = Engine.Creator.getInstance().getActiveRepository().getHeadBranch().getName();

        try {
            Engine.Creator.getInstance().checkout(headBranchName ,true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

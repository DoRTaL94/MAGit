package servlets;

import MagitExceptions.PointedCommitEmptyException;
import com.google.gson.Gson;
import magit.Engine;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/pages/create-new-branch")
public class CreateNewBranchServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = SessionUtils.getUsername(request);
        String userToSendRepo = SessionUtils.getUserRepo(request);
        Engine engine;

        if(username.equals(userToSendRepo)) {
            engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(userToSendRepo == null ? username : userToSendRepo);
            String branchName = request.getParameter("branchname");

            try {
                engine.createNewBranch(branchName);
                response.setContentType("application/json;charset=UTF-8");
                PrintWriter out = response.getWriter();
                List<String> data = new ArrayList<>();
                data.add(branchName);
                data.add(engine.getActiveRepository().getHeadBranch().getPointedCommitSha1());
                out.print(new Gson().toJson(data));
            } catch (PointedCommitEmptyException e) {
                response.setContentType("text/html");
                PrintWriter out = response.getWriter();
                out.print(e.getMessage());
            }
        } else {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("Not user's repository");
        }
    }
}

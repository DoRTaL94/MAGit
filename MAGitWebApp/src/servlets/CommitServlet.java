package servlets;

import MagitExceptions.CommitAlreadyExistsException;
import MagitExceptions.EmptyWcException;
import magit.Engine;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/pages/commit")
public class CommitServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String username = SessionUtils.getUsername(request);
            Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);
            String description = request.getParameter("description");
           engine.commit(description, null);
        } catch (IOException | EmptyWcException | CommitAlreadyExistsException e) {
            e.printStackTrace();
        }
    }
}

package servlets;

import MagitExceptions.CommitAlreadyExistsException;
import MagitExceptions.EmptyWcException;
import magit.Engine;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/pages/commit")
public class CommitServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String description = request.getParameter("description");
            Engine.Creator.getInstance().commit(description, null);
        } catch (IOException | EmptyWcException | CommitAlreadyExistsException e) {
            e.printStackTrace();
        }
    }
}

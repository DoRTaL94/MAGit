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
import java.io.PrintWriter;

@WebServlet("/pages/commit")
public class CommitServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try {
            String username = SessionUtils.getUsername(request);
            String userToSendRepo = SessionUtils.getUserRepo(request);

            if(username.equals(userToSendRepo)) {
                Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(userToSendRepo == null ? username : userToSendRepo);
                String description = request.getParameter("description");
                engine.commit(description, null);
                out.print("success");
            } else {
                out.print("Not user's repository");
            }
        } catch (IOException | CommitAlreadyExistsException e) {
            e.printStackTrace();
        } catch (EmptyWcException e) {
            out.print("empty");
        }
    }
}

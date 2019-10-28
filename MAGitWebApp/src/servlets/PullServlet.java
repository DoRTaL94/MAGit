package servlets;

import MagitExceptions.CollaborationException;
import magit.Engine;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/pages/pull")
public class PullServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String username = SessionUtils.getUsername(request);
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);
        String remoteRepositoryLocation = engine.getActiveRepository().getRemoteRepositoryLocation();

        if(remoteRepositoryLocation != null) {
            try {
                engine.Pull(engine.getActiveRepositoryName(), remoteRepositoryLocation);
                out.print("Pull executed successfully");
            } catch (Exception e) {
                out.print(e.getMessage());
            }
        }
    }
}

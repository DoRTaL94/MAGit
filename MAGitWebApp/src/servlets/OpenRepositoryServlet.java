package servlets;

import data.structures.Repository;
import magit.Engine;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/pages/open-repository")
public class OpenRepositoryServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = SessionUtils.getUsername(request);
        String userToSendRepo = SessionUtils.getUserRepo(request);
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(userToSendRepo == null ? username : userToSendRepo);
        String repositoryName = request.getParameter("repositoryname");

        Repository repository = engine.getRepository(repositoryName);
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        if(repository != null) {
            engine.setActiveRepository(repository);
            out.print("success");
        } else {
            out.print("repository not exists");
        }


    }
}

package servlets;

import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/pages/set-user-repo")
public class SetUserRepoServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        String username;
        String current = request.getParameter("current");

        if(current == null) {
            current = request.getAttribute("current").toString();
        }

        try {
            if (current.equals("true")) {
                username = SessionUtils.getUsername(request);
            } else {
                username = request.getParameter("user");

                if (username == null) {
                    username = request.getAttribute("user").toString();
                }
            }

            request.getSession(true).setAttribute("userRepo", username);
            out.print("success");
        } catch (NullPointerException e) {
            out.print("failed to set user");
        }
    }
}

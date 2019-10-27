package servlets;

import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/pages/set-user-repo")
public class SetUserRepoServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String username;

        if(request.getParameter("current").equals("true")) {
            username = SessionUtils.getUsername(request);
        } else {
            username = request.getParameter("user");
        }

        request.getSession(true).setAttribute("userRepo", username);
    }
}

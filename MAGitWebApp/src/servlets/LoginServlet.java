package servlets;

import com.google.gson.Gson;
import users.UsersManager;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html;charset=UTF-8");
        String usernameFromSession = SessionUtils.getUsername(request);
        List<String> errors = new ArrayList<>();
        UsersManager userManager = ServletsUtils.getUsersManager(getServletContext());

        // user is not logged in yet.
        if (usernameFromSession == null) {
            String username = request.getParameter("username");
            String password = request.getParameter("password");

            if(username != null && password != null) {
                if (username.isEmpty()) {
                    errors.add("User name cannot be empty.");
                }

                if (password.isEmpty()) {
                    errors.add("Password cannot be empty.");
                }


                if (errors.size() == 0 && !userManager.isUserExists(username)) {
                    errors.add("User name or password is invalid.");
                }

                if (errors.size() != 0) {
                    response.setContentType("application/json;charset=UTF-8");
                    PrintWriter out = response.getWriter();
                    Gson gson = new Gson();
                    out.print(gson.toJson(errors));
                    out.flush();
                } else {
                    userManager.login(username, password);
                    String authString = userManager.getAuthString(username, password);
                    request.getSession(true).setAttribute("username", username);
                    request.getSession(true).setAttribute("auth", authString);
                    RequestDispatcher requestDispatcher = request.getRequestDispatcher("/pages/load-repository");
                    requestDispatcher.forward(request, response);
                }
            } else {
                PrintWriter out = response.getWriter();
                out.print("User didn't log in.");
            }
        }
    }

}

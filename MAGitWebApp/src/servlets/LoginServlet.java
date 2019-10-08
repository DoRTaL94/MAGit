package servlets;

import com.google.gson.Gson;
import users.UsersManager;
import utils.ServletsUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/pages/signin/login")
public class LoginServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        UsersManager userManager = ServletsUtils.getUsersManager(getServletContext());
        List<String> errors = new ArrayList<>();

        if (username.isEmpty()) {
            errors.add("User name cannot be empty.");
        }

        if (password.isEmpty()) {
            errors.add("Password cannot be empty.");
        }

        if(errors.size() == 0 && (!userManager.isUserExists(username) ||
                !userManager.getUser(username).getPassword().equals(password))) {
            errors.add("User name or password is invalid.");
        }

        if(errors.size() != 0) {
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            Gson gson = new Gson();
            out.print(gson.toJson(errors));
            out.flush();
        } else {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("success");
            userManager.setLoggedInUser(userManager.getUser(username));
            out.flush();
        }
    }

}

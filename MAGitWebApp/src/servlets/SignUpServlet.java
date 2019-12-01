package servlets;

import IO.FileUtilities;
import com.google.gson.Gson;
import magit.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import string.StringUtilities;
import users.UsersManager;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/signup")
public class SignUpServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String password1 = request.getParameter("password");
        String password2 = request.getParameter("password2");
        UsersManager userManager = ServletsUtils.getUsersManager(getServletContext());
        List<String> errors = new ArrayList<>();

        if (username.isEmpty()) {
            errors.add("User name cannot be empty.");
        }

        if (password1.isEmpty() && password2.isEmpty()) {
            errors.add("Password cannot be empty.");
        }
        else if(password1.isEmpty() || password2.isEmpty() || !password1.equals(password2)) {
            errors.add("Passwords are not matched.");
        }

        String usernameFromSession = SessionUtils.getUsername(request);

        if(userManager.isUserExists(username) ||
                (usernameFromSession != null && usernameFromSession.equals(username))) {
            errors.add("User name is taken.");
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
            userManager.addUser(username, password1);
            out.print("success");
            out.flush();
        }
    }

}
package servlets;

import com.google.gson.Gson;
import magit.Engine;
import notifications.ForkNotification;
import notifications.INotification;
import users.User;
import users.UsersManager;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/pages/active-repo")
public class NotificationsServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        UsersManager usersManager = ServletsUtils.getUsersManager(getServletContext());
        Gson gson = new Gson();
        String toOut;
        String username = SessionUtils.getUsername(request);
        User user = usersManager.getUser(username);
        user.getNotificationManager().addNotification(new ForkNotification("dor", "repo"));
        List<INotification> notifications = user.getNotificationManager().getNotifications();
        if(usersManager.isUserLoggedIn(username)) {
            toOut = gson.toJson(notifications);
        } else {
            toOut = gson.toJson(new ArrayList<INotification>());
        }

        out.print(toOut);
        out.flush();
    }
}

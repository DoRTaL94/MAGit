package servlets;

import com.google.gson.Gson;
import data.structures.Repository;
import notifications.ForkNotification;
import notifications.INotification;
import notifications.NotificationManager;
import users.User;
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

@WebServlet("/pages/repository/active_repo")
public class NotificationsServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        UsersManager usersManager = ServletsUtils.getUsersManager(getServletContext());
        Gson gson = new Gson();
        String toOut;
        usersManager.getLoggedInUser().getNotificationManager().addNotification(new ForkNotification("dor", "repo"));
        List<INotification> notifications = usersManager.getLoggedInUser().getNotificationManager().getNotifications();
        if(usersManager.isUserLoggedIn()) {
            toOut = gson.toJson(notifications);
        } else {
            toOut = gson.toJson(new ArrayList<INotification>());
        }

        out.print(toOut);
    }
}

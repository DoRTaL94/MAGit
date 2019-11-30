package servlets;

import com.google.gson.Gson;
import magit.Engine;
import notifications.INotification;
import notifications.NotificationManager;
import users.User;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/pages/notifications")
public class NotificationsServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String username = SessionUtils.getUsername(request);
        User user = ServletsUtils.getUsersManager(getServletContext()).getUser(username);

        try {
            NotificationManager notificationManager = user.getNotificationManager();
            List<INotification> notifications = notificationManager.getNotifications();

            Gson gson = new Gson();
            String toOut = gson.toJson(notifications);
            out.print(toOut);
        } catch (NullPointerException ignored) {}
    }
}

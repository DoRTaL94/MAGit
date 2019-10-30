package servlets;

import magit.Engine;
import notifications.INotification;
import users.User;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/pages/notifications-read")
public class NotificationsReadServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String username = SessionUtils.getUsername(request);
        User user = ServletsUtils.getUsersManager(getServletContext()).getUser(username);
        List<INotification> notifications = user.getNotificationManager().getNotifications();

        for(INotification notification: notifications) {
            notification.setReadByUser(true);
        }
    }
}

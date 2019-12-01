package servlets;

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
import java.util.List;

@WebServlet("/pages/logout")
public class LogoutServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String username = SessionUtils.getUsername(request);
        User user = ServletsUtils.getUsersManager(getServletContext()).getUser(username);
        List<INotification> notifications = user.getNotificationManager().getNotifications();

        for(INotification notification: notifications) {
            notification.setNotShowNotification(true);
        }

        SessionUtils.clearSession(request);
        ServletsUtils.getUsersManager(getServletContext()).logout(username);
        out.print("logged out");
    }
}

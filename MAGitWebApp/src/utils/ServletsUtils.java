package utils;

import users.UsersManager;

import javax.servlet.ServletContext;

public class ServletsUtils {
    private static final String USER_MANAGER_ATTRIBUTE_NAME = "usersManager";
    private static final Object userManagerLock = new Object();

    public static UsersManager getUsersManager(ServletContext i_ServletContext) {

        synchronized (userManagerLock) {
            if (i_ServletContext.getAttribute(USER_MANAGER_ATTRIBUTE_NAME) == null) {
                i_ServletContext.setAttribute(USER_MANAGER_ATTRIBUTE_NAME, new UsersManager());
            }
        }

        return (UsersManager) i_ServletContext.getAttribute(USER_MANAGER_ATTRIBUTE_NAME);
    }
}

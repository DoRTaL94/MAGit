package servlets;

import MagitExceptions.OpenChangesInWcException;
import com.google.gson.Gson;
import magit.Engine;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/pages/checkout")
public class CheckoutServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String username = SessionUtils.getUsername(request);
            String branchName = request.getParameter("branchname");
            boolean isCheckWc = request.getParameter("checkwc").equals("false");

            ServletsUtils.getUsersManager(getServletContext()).getEngine(username).checkout(branchName, isCheckWc);
        } catch (OpenChangesInWcException e) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("There are open changes in working directory.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

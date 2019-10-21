package servlets;

import com.google.gson.Gson;
import data.structures.Repository;
import magit.Engine;
import utils.RepositoryManager;
import utils.RepositoryUpdates;
import utils.ServletsUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/pages/update-repo")
public class UpdateRepoServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        RepositoryManager repositoryManager = ServletsUtils.getRepositoryManager(getServletContext());
        RepositoryUpdates repositoryUpdates = repositoryManager.getRepositoryUpdates();

        if(repositoryUpdates.getRepository() == null) {
            out.print("User has no repositories.");
        } else {
            out.print(gson.toJson(repositoryUpdates));
        }

        out.flush();
    }
}

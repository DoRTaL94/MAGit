package servlets;

import IO.FileUtilities;
import MagitExceptions.FolderInLocationAlreadyExistsException;
import MagitExceptions.RepositoryAlreadyExistsException;
import MagitExceptions.xmlErrorsException;
import com.google.gson.Gson;
import javafx.beans.property.SimpleStringProperty;
import magit.Engine;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

@WebServlet("/pages/import")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 5, maxRequestSize = 1024 * 1024 * 5 * 5)
public class ImportRepoServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String username             = SessionUtils.getUsername(request);
        Engine engine               = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);
        Collection<Part> parts      = request.getParts(); // Uploading files sends them in parts to the server. Parts size could be configured in '@MultipartConfig' above.
        StringBuilder fileContent   = new StringBuilder(); // We will append each part and make one string out of them.
        List<String> errors         = new ArrayList<>();

        for (Part part : parts) {
            fileContent.append(readFromInputStream(part.getInputStream()));
        }

        try {
            // In order to load the uploaded xml we will convert it into input stream so 'JAXB' could convert it to an object.
            InputStream xmlStream = new ByteArrayInputStream(fileContent.toString().getBytes());
            // This is a slighting different 'LoadRepositoryFromXml' function we will pass to it the input stream we've created and the username logged in to
            // the server. We're passing the username because inside the xml file there is a username parameter that it might no be the current user whom logged in.
            engine.LoadRepositoryFromXml(xmlStream, username, new SimpleStringProperty());
            engine.setCurrentUserName(username);
        } catch (RepositoryAlreadyExistsException e) {
            errors.add(e.getMessage());
        } catch (xmlErrorsException e) {
            errors.addAll(e.getErrors());
        } catch (FolderInLocationAlreadyExistsException ignored) {
        } catch (JAXBException e) {
            errors.add("File is not xml file");
        }

        if (errors.size() > 0) {
            response.setContentType("application/json;charset=UTF-8");
            Gson gson = new Gson();
            String toOut = gson.toJson(errors);
            PrintWriter out = response.getWriter();
            out.print(toOut);
            out.flush();
        } else {
            engine.getActiveRepository().setOwner(username);
            // לעדכן את הרפוזיטורי הנוכחי של המשתמש
            // להוסיף לרשימת הרפוזיטוריס של המשתמש
        }
    }

    private String readFromInputStream(InputStream inputStream) {
        return new Scanner(inputStream).useDelimiter("\\Z").next();
    }
}

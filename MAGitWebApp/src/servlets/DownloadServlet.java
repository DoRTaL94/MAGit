package servlets;

import MagitExceptions.RepositoryNotLoadedException;
import MagitExceptions.xmlErrorsException;
import data.structures.Repository;
import magit.Constants;
import magit.Engine;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Paths;

@WebServlet("/pages/download")
public class DownloadServlet extends HttpServlet {
    private static final String XML_DIR_NAME = "xml files";
    private static final String PARAMETER_NAME = "repository";
    private static final String DEFAULT_MIME = "application/octet-stream";
    private static final int ARBITARY_SIZE = 4096;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String username = SessionUtils.getUsername(request);
            String userToSendRepo = SessionUtils.getUserRepo(request);
            Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(userToSendRepo == null ? username : userToSendRepo);

            String xmlPath = getXmlPath(request);
            engine.exportRepositoryToXml(engine.getActiveRepositoryName(), xmlPath);
            File downloadFile = new File(Paths.get(xmlPath).toString());

            try(InputStream in = new FileInputStream(xmlPath);
                OutputStream out = response.getOutputStream()) {
                String mimeType = getServletContext().getMimeType(xmlPath);

                if (mimeType == null) {
                    mimeType = DEFAULT_MIME;
                }

                String headerKey = "Content-Disposition";
                String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());
                response.setHeader(headerKey, headerValue);
                response.setContentType(mimeType);
                response.setContentLength((int) downloadFile.length());

                byte[] buffer = new byte[ARBITARY_SIZE];
                int numBytesRead;

                while ((numBytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, numBytesRead);
                }
            }
        } catch (xmlErrorsException | RepositoryNotLoadedException ignored) {}
        catch (NullPointerException e) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("Repository not exists");
        }
    }

    private String getXmlPath(HttpServletRequest request) {
        String username = SessionUtils.getUsername(request);
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);

        Repository repo = engine.getRepository(request.getParameter(PARAMETER_NAME));
        String xmlFilesLocation = Paths.get(Constants.DB_LOCATION, repo.getOwner(), XML_DIR_NAME).toString();
        File xmlFilesDir = new File(xmlFilesLocation);

        if(!xmlFilesDir.exists()) {
            xmlFilesDir.mkdir();
        }

        return Paths.get(xmlFilesLocation, repo.getName() + ".xml").toString();
    }
}

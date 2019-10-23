package servlets;

import IO.FileUtilities;
import com.google.gson.Gson;
import data.structures.Blob;
import data.structures.Folder;
import data.structures.Repository;
import data.structures.eFileType;
import magit.Engine;
import org.apache.commons.codec.digest.DigestUtils;
import utils.AddedFileDetails;
import utils.ServletsUtils;
import utils.SessionUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.io.File;

@WebServlet("/pages/create-file")
public class CreateFileServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = SessionUtils.getUsername(request);
        Engine engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(username);
        List<String> reqData = ServletsUtils.getReqData(request);
        String filename = request.getParameter("filename");
        String extension = request.getParameter("extension");

        if(!extension.equals("Extension...")) {
            String fullname = filename + (extension.equals("Directory") ? "" : extension);
            Folder folder = engine.getActiveRepository().getFolders().get(reqData.get(reqData.size() - 1));

            try {
                Path path = getPathFromData(engine, reqData);

                if (folder != null) {
                    String sha1;
                    String filePath = Paths.get(path.toString(), fullname).toString();
                    File file = new File(filePath);

                    if(!file.exists()) {
                        response.setContentType("application/json;charset=UTF-8");
                        PrintWriter out = response.getWriter();

                        if (extension.equals("Directory")) {
                            Folder newFolder = new Folder();
                            newFolder.setIsRoot(false);
                            sha1 = DigestUtils.sha1Hex(newFolder.toStringForSha1(path));
                            file.mkdir();
                            Gson gson = new Gson();
                            String toOut = gson.toJson(new AddedFileDetails(reqData, newFolder, sha1, "folder"));
                            out.print(toOut);
                        } else {
                            Blob newBlob = new Blob();
                            newBlob.setText("");
                            newBlob.setName(fullname);
                            sha1 = DigestUtils.sha1Hex(newBlob.toStringForSha1());
                            FileUtilities.WriteToFile(filePath, "");

                            Gson gson = new Gson();
                            String toOut = gson.toJson(new AddedFileDetails(reqData, newBlob, sha1, "blob"));
                            out.print(toOut);
                        }

                    } else {
                        response.setContentType("text/html");
                        PrintWriter out = response.getWriter();
                        out.print("File already exists.");
                    }
                } else {
                    response.setContentType("text/html");
                    PrintWriter out = response.getWriter();
                    out.print("Parent folder doesn't exist.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("Please choose extension.");
        }
    }

    private Path getPathFromData(Engine i_Engine, List<String> i_Data) throws Exception {
        Repository repository = i_Engine.getActiveRepository();
        String currCommitSha1 = repository.getHeadBranch().getPointedCommitSha1();
        Folder root = repository.getFolders().get(repository.getCommits().get(currCommitSha1).getRootFolderSha1());

        Path path = Paths.get("c:/magit-ex3/", i_Engine.getCurrentUserName(), "repositories", repository.getName());

        for(int i = 1; i < i_Data.size(); i++) {
            Folder.Data folder = ServletsUtils.getFile(root, i_Data.get(i));

            if (folder != null) {
                path = Paths.get(path.toString(), folder.getName());
            } else {
                throw new Exception(String.format("The sha1 key %s not exists in folders map.", i_Data.get(i)));
            }
        }

        return path;
    }
}

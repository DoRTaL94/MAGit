package servlets;

import IO.FileUtilities;
import com.google.gson.Gson;
import data.structures.*;
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

    private Engine engine;
    private List<String> folderNamesStack;


    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = SessionUtils.getUsername(request);
        String userToSendRepo = SessionUtils.getUserRepo(request);

        if(username.equals(userToSendRepo)) {
            engine = ServletsUtils.getUsersManager(getServletContext()).getEngine(userToSendRepo == null ? username : userToSendRepo);
            folderNamesStack = ServletsUtils.getReqData(request);
            String filename = request.getParameter("filename");
            String extension = request.getParameter("extension");

            if (!extension.equals("Extension...")) {
                String fullname = filename + (extension.equals("Directory") ? "" : extension);

                try {
                    String parentFolderPath = ServletsUtils.getPathFromFolderNamesList(folderNamesStack, engine, folderNamesStack.size() - 1).toString();
                    File parentFolder = new File(parentFolderPath);
                    String filePath = Paths.get(parentFolderPath, fullname).toString();

                    if (parentFolder.exists()) {
                        File file = new File(filePath);

                        if (!file.exists()) {
                            response.setContentType("application/json;charset=UTF-8");
                            PrintWriter out = response.getWriter();
                            Gson gson = new Gson();
                            String toOut = gson.toJson(getDataForResponse(file, fullname, extension));
                            out.print(toOut);

                            FileUtilities.WriteToFile(Paths.get(engine.getActiveRepository().getLocationPath(), ".magit", "oldCommit.txt").toString(),
                                    engine.getActiveRepository().getHeadBranch().getPointedCommitSha1());
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
        } else {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("Not user's repository");
        }
    }

    private AddedFileDetails getDataForResponse(File i_AddedFile, String i_FileFullName, String i_FileExtension) {
        String fileSha1;
        Path path = i_AddedFile.toPath();
        String parentFolderSha1 = folderNamesStack.get(folderNamesStack.size() - 1);
        IRepositoryFile file;

        if (i_FileExtension.equals("Directory")) {
            file = new Folder();
            ((Folder) file).setIsRoot(false);
            fileSha1 = DigestUtils.sha1Hex(((Folder) file).toStringForSha1(path));
            i_AddedFile.mkdir();
        } else {
            file = new Blob();
            ((Blob) file).setText("");
            ((Blob) file).setName(i_FileFullName);
            fileSha1 = DigestUtils.sha1Hex( ((Blob) file).toStringForSha1());
            FileUtilities.WriteToFile(path.toString(), "");
        }

        Folder.Data addedFileData = new Folder.Data();
        addedFileData.setName(i_FileFullName);
        addedFileData.setLastChanger(engine.getCurrentUserName());
        addedFileData.setlastUpdate(new SimpleDateFormat(Engine.DATE_FORMAT).format(new Date(i_AddedFile.lastModified())));
        addedFileData.setFileType(i_FileExtension.equals("Directory") ? eFileType.FOLDER : eFileType.BLOB);
        addedFileData.setCreationTimeMillis(i_AddedFile.lastModified());
        addedFileData.setSHA1(fileSha1);

        return new AddedFileDetails(parentFolderSha1, addedFileData, file);
    }
}

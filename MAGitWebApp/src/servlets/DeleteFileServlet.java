package servlets;

import IO.FileUtilities;
import data.structures.Blob;
import data.structures.Folder;
import data.structures.eFileType;
import magit.Engine;
import utils.ServletsUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.io.File;
import java.util.Map;

@WebServlet("/pages/delete-file")
public class DeleteFileServlet extends HttpServlet {

    private List<String> reqData = null;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean isSuccess = ServletsUtils.applyOnDbFile(ServletsUtils.getUsersManager(getServletContext()).getLoggedInUser().getName(), ServletsUtils.getReqData(request), this::deleteFile);

        if(!isSuccess) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("failed to delete");
        }
    }

    private boolean deleteFile(Folder i_Parent, File i_File, Folder.Data i_Data) {
        boolean isSuccess = false;

        if(i_File.exists()) {
            isSuccess = FileUtilities.removeFile(i_File);

            if(isSuccess) {
                removeFileFromMem(i_Data);
                i_Parent.getFiles().remove(i_Data);
            }
        }

        return isSuccess;
    }

    private static void removeFileFromMem(Folder.Data i_Data) {
        if(i_Data.getFileType().equals(eFileType.FOLDER)) {
            Map<String, Folder> folders = Engine.Creator.getInstance().getActiveRepository().getFolders();
            Folder folder = folders.get(i_Data.getSHA1());
            List<Folder.Data> files = folder.getFiles();

            for(Folder.Data file: files) {
                removeFileFromMem(file);
            }

            folders.remove(i_Data.getSHA1());
        } else {
            Map<String, Blob> blobs = Engine.Creator.getInstance().getActiveRepository().getBlobs();
            blobs.remove(i_Data.getSHA1());
        }
    }
}

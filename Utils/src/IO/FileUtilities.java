package IO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileUtilities {
    public static void WriteToFile(String i_FilePath, String i_Content) {
        try(BufferedWriter writer = new BufferedWriter(Files.newBufferedWriter(Paths.get(i_FilePath)))) {
            writer.write(i_Content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createFoldersInPath(String i_Path) {
        if(i_Path == null) {
            return;
        }

        File folder = new File(i_Path);
        String parentPath = folder.getParent();

        createFoldersInPath(parentPath);

        int lastIndexOfDot = i_Path.lastIndexOf('.');;
        int lastIndexOfBackSlash = i_Path.lastIndexOf('\\');
        int lastIndexOfForwardSlash = i_Path.lastIndexOf('/');

        boolean test1 = lastIndexOfBackSlash == -1 || lastIndexOfDot == -1 || lastIndexOfDot == lastIndexOfBackSlash + 1;
        boolean test2 = lastIndexOfForwardSlash == -1 || lastIndexOfDot == -1 || lastIndexOfDot == lastIndexOfForwardSlash + 1;

        if(!folder.exists() && test1 && test2) {
            folder.mkdir();
        }
    }

    public static void ZipFile(String i_FileToZipName, String i_Content, String i_ZipPath) throws IOException {
        File zipFile = new File(i_ZipPath);

        try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry zipEntry = new ZipEntry(i_FileToZipName);
            out.putNextEntry(zipEntry);

            byte[] data = i_Content.getBytes(StandardCharsets.UTF_8);
            out.write(data, 0, data.length);
            out.closeEntry();
        }
    }

    public static String UnzipFile(String i_ZipPath) throws IOException {
        String content = null;

        try(ZipFile zip = new ZipFile(i_ZipPath)) {
            Enumeration e = zip.entries();

            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();

                if (!entry.isDirectory()) {
                    content = getText(zip.getInputStream(entry));
                }
            }
        }

        return content;
    }

    public static String getZippedFileName(String i_ZipPath) throws IOException {
        String name = null;

        try(ZipFile zip = new ZipFile(i_ZipPath)) {
            Enumeration e = zip.entries();

            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                name = entry.getName();
            }
        }

        return name;
    }

    private static String getText(InputStream in)  {
        StringBuilder sb = new StringBuilder();
        String content = null;
        String line;

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }

            content = sb.toString();
            int lastIndexOfLineSeparator = content.lastIndexOf(System.lineSeparator());
            content = lastIndexOfLineSeparator == -1 ? "" : content.substring(0, lastIndexOfLineSeparator);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }

    public static String ReadTextFromFile(String i_TextFilePath) throws IOException {
        String content = null;

        try(FileInputStream inputStream = new FileInputStream(i_TextFilePath)) {
            content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        return content;
    }

    public static boolean removeFile(File i_File) {
        return FileUtils.deleteQuietly(i_File);
    }
}

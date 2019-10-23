package data.structures;

import java.io.File;
import java.io.IOException;
import IO.FileUtilities;
import resources.jaxb.schema.generated.MagitBlob;

public class Blob implements IRepositoryFile {
    private String text;
    private String name;

    public Blob() {
        text = null;
    }

    public static String getSha1FromContent(String blobContent) {
        return blobContent.replaceAll("\\s", "");
    }

    public String getText() {
        return text;
    }

    public void setText(String i_Text) {
        text = i_Text;
    }

    public static Blob parse(MagitBlob i_MagitBlob){
        Blob newBlob = new Blob();
        newBlob.setName(i_MagitBlob.getName());
        newBlob.setText(i_MagitBlob.getContent());
        return newBlob;
    }

    public void setName(String i_Name) {
        name = i_Name;
    }

    public static Blob parse(File i_BlobZippedFile) throws IOException {
        Blob newBlob = new Blob();
        newBlob.setName(FileUtilities.getZippedFileName(i_BlobZippedFile.getPath()));
        newBlob.setText(FileUtilities.UnzipFile(i_BlobZippedFile.getPath()));
        return newBlob;
    }

    @Override
    public String toString() {
        return text;
    }

    public String toStringForSha1() { return text.replaceAll("\\s", "") + name; }

    public Blob clone() {
        Blob clone = null;

        try {
            clone = (Blob) super.clone();
            clone.text = this.text;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return clone;
    }
}

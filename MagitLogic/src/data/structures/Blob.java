package data.structures;

import java.io.File;
import java.io.IOException;
import IO.FileUtilities;
import resources.jaxb.schema.generated.MagitBlob;

public class Blob implements IRepositoryFile {
    private String text;

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
        newBlob.setText(i_MagitBlob.getContent());
        return newBlob;
    }

    public static Blob parse(File i_BlobZippedFile) throws IOException {
        Blob newBlob = new Blob();
        String blobContent = null;

        blobContent = FileUtilities.UnzipFile(i_BlobZippedFile.getPath());

        newBlob.setText(blobContent);
        return newBlob;
    }

    @Override
    public String toString() {
        return text;
    }

    public String toStringForSha1() { return text.replaceAll("\\s", ""); }
}

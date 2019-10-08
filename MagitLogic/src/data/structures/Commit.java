package data.structures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import IO.FileUtilities;
import org.apache.commons.codec.digest.DigestUtils;
import puk.team.course.magit.ancestor.finder.CommitRepresentative;
import resources.jaxb.schema.generated.MagitSingleCommit;
import resources.jaxb.schema.generated.PrecedingCommits;

public class Commit implements IRepositoryFile, CommitRepresentative {
    private String firstPrecedingCommitsSHA1 = "";
    private String secondPrecedingCommitsSHA1 = "";
    private String rootFolderSHA1 = null;
    private String message = null;
    private String lastChanger = null;
    private String lastUpdate = null;
    private String sha1 = null;

    @Override
    public String toString() {
        return String.format("%s;%s;%s;%s;%s;%s", rootFolderSHA1, firstPrecedingCommitsSHA1, secondPrecedingCommitsSHA1, message, lastChanger, lastUpdate);
    }

    public String toStringForSha1() {
        return String.format("%s;%s;%s;%s", rootFolderSHA1, firstPrecedingCommitsSHA1, secondPrecedingCommitsSHA1, message);
    }

    public List<String> getPrecedingCommits() {
        List<String> precedings = new ArrayList<>();

        if(!firstPrecedingCommitsSHA1.equals("")) {
            precedings.add(firstPrecedingCommitsSHA1);

            if (!secondPrecedingCommitsSHA1.equals("")) {
                precedings.add(secondPrecedingCommitsSHA1);
            }
        }

        return precedings;
    }

    public String getRootFolderSHA1() {
        return rootFolderSHA1;
    }

    public void setRootFolderSHA1(String i_MainFolderSHA1) {
        this.rootFolderSHA1 = i_MainFolderSHA1;
    }

    public void setFirstPrecedingCommitSha1(String i_PrecedingCommitSHA1) {
        firstPrecedingCommitsSHA1 = i_PrecedingCommitSHA1;
    }

    public void setSecondPrecedingCommitSha1(String i_PrecedingCommitSHA1) {
        secondPrecedingCommitsSHA1 = i_PrecedingCommitSHA1;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String i_Message) {
        this.message = i_Message;
    }

    public String getLastChanger() {
        return lastChanger;
    }

    public void setLastChanger(String i_LastChanger) {
        lastChanger = i_LastChanger;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String i_LastUpdate) {
        lastUpdate = i_LastUpdate;
    }

    public static Commit parse(MagitSingleCommit i_MagitCommit) {
        Commit newCommit = new Commit();
        newCommit.setMessage(i_MagitCommit.getMessage());
        newCommit.setLastChanger(i_MagitCommit.getAuthor());
        newCommit.setLastUpdate(i_MagitCommit.getDateOfCreation());
        newCommit.setRootFolderSHA1(i_MagitCommit.getRootFolder().getId());

        if(i_MagitCommit.getPrecedingCommits() != null) {
            List<PrecedingCommits.PrecedingCommit> precedings = i_MagitCommit.getPrecedingCommits().getPrecedingCommit();

            if (precedings.size() != 0) {
                newCommit.setFirstPrecedingCommitSha1(precedings.get(0).getId());

                if (precedings.size() == 2) {
                    newCommit.setSecondPrecedingCommitSha1(precedings.get(1).getId());
                }
            }
        }

        return newCommit;
    }

    public static Commit parse(File i_ZippedCommitFile) throws IOException {
        Commit newCommit = new Commit();

        try {
            String commitContent = FileUtilities.UnzipFile(i_ZippedCommitFile.getPath());
            String[] parts = commitContent.split(";");
            newCommit.setRootFolderSHA1(parts[0]);

            if (!parts[1].equals("")) {
                newCommit.setFirstPrecedingCommitSha1(parts[1]);

                if(!parts[2].equals("")) {
                    newCommit.setSecondPrecedingCommitSha1(parts[2]);
                }
            }

            newCommit.setMessage(parts[3]);
            newCommit.setLastChanger(parts[4]);
            newCommit.setLastUpdate(parts[5]);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("Not a commit file.");
        }

        return newCommit;
    }

    public String getSha1() {
        if(sha1 == null) {
            sha1 = DigestUtils.sha1Hex(this.toStringForSha1());
        }

        return sha1;
    }

    public String getFirstPrecedingSha1() {
        return firstPrecedingCommitsSHA1;
    }

    public String getSecondPrecedingSha1() {
        return secondPrecedingCommitsSHA1;
    }
}

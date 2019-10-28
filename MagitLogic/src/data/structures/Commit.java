package data.structures;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import IO.FileUtilities;
import magit.Engine;
import org.apache.commons.codec.digest.DigestUtils;
import puk.team.course.magit.ancestor.finder.CommitRepresentative;
import resources.jaxb.schema.generated.MagitSingleCommit;
import resources.jaxb.schema.generated.PrecedingCommits;

public class Commit implements IRepositoryFile, CommitRepresentative {
    private String firstPrecedingCommitsSHA1 = "";
    private String secondPrecedingCommitsSHA1 = "";
    private String rootFolderSha1 = null;
    private String message = null;
    private String lastChanger = null;
    private String lastUpdate = null;
    private String sha1 = null;
    private long creationTimeMillis;
    private boolean isPullRequested = false;

    public boolean isPullRequested() {
        return isPullRequested;
    }

    public void setPullRequested(boolean i_PullRequested) {
        isPullRequested = i_PullRequested;
    }

    @Override
    public String toString() {
        return String.format("%s;%s;%s;%s;%s;%s", rootFolderSha1, firstPrecedingCommitsSHA1, secondPrecedingCommitsSHA1, message, lastChanger, lastUpdate);
    }

    public String toStringForSha1() {
        return String.format("%s;%s;%s;%s", rootFolderSha1, firstPrecedingCommitsSHA1, secondPrecedingCommitsSHA1, message);
    }

    public void setSha1(String i_Sha1) {
        sha1 = i_Sha1;
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

    public String getRootFolderSha1() {
        return rootFolderSha1;
    }

    public void setRootFolderSha1(String i_MainFolderSHA1) {
        this.rootFolderSha1 = i_MainFolderSHA1;
        updateSha1();
    }

    public void setFirstPrecedingCommitSha1(String i_PrecedingCommitSHA1) {
        firstPrecedingCommitsSHA1 = i_PrecedingCommitSHA1;
        updateSha1();
    }

    public void setSecondPrecedingCommitSha1(String i_PrecedingCommitSHA1) {
        secondPrecedingCommitsSHA1 = i_PrecedingCommitSHA1;
        updateSha1();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String i_Message) {
        this.message = i_Message;
        updateSha1();
    }

    public String getLastChanger() {
        return lastChanger;
    }

    public void setLastChanger(String i_LastChanger) {
        lastChanger = i_LastChanger;
        updateSha1();
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String i_LastUpdate) {
        try {
            creationTimeMillis = new SimpleDateFormat(Engine.DATE_FORMAT).parse(i_LastUpdate).getTime();
        } catch (ParseException e) {
            creationTimeMillis = 0;
        }
        lastUpdate = i_LastUpdate;
        updateSha1();
    }

    public static Commit parse(MagitSingleCommit i_MagitCommit) {
        Commit newCommit = new Commit();
        newCommit.setMessage(i_MagitCommit.getMessage());
        newCommit.setLastChanger(i_MagitCommit.getAuthor());
        newCommit.setLastUpdate(i_MagitCommit.getDateOfCreation());
        newCommit.setRootFolderSha1(i_MagitCommit.getRootFolder().getId());

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
            newCommit.setRootFolderSha1(parts[0]);

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
        return sha1;
    }

    public String getFirstPrecedingSha1() {
        return firstPrecedingCommitsSHA1;
    }

    public String getSecondPrecedingSha1() {
        return secondPrecedingCommitsSHA1;
    }

    private void updateSha1() {
        sha1 = DigestUtils.sha1Hex(this.toStringForSha1());
    }
}

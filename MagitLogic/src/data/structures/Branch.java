package data.structures;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import resources.jaxb.schema.generated.MagitSingleBranch;

public class Branch {
    private static final String TXT_FILE_SUFFIX = ".txt";
    private String pointedCommitSha1 = "";
    private String name = null;
    private boolean isHead = false;
    private boolean isRemote = false;
    private boolean isTracking = false;
    private String trackingAfter = "";
    private boolean isMerged = false;

    public void setIsRemote(boolean i_IsRemote) {
        isRemote = i_IsRemote;
    }

    public boolean isTracking() {
        return isTracking;
    }

    public void setIsTracking(boolean i_IsTracking) {
        isTracking = i_IsTracking;
    }

    public String getTrakingAfter() {
        return trackingAfter;
    }

    public void setTrakingAfter(String i_TrakingAfter) {
        trackingAfter = i_TrakingAfter;
    }

    public boolean isRemote() { return isRemote; }

    public String getPointedCommitSha1() {
        return pointedCommitSha1;
    }

    public void setPointedCommitSha1(String i_PointedCommitSha1) {
        pointedCommitSha1 = i_PointedCommitSha1;
    }

    public String getName() {
        return name;
    }

    public void setName(String i_Name) {
        name = i_Name;
    }

    public boolean isHead() {
        return isHead;
    }

    public void setIsHead(boolean i_IsHead){
        isHead = i_IsHead;
    }

    public static Branch parse(MagitSingleBranch i_MagitBranch) {
        Branch newBranch = new Branch();
        newBranch.setName(i_MagitBranch.getName());
        newBranch.setPointedCommitSha1(i_MagitBranch.getPointedCommit().getId());
        return newBranch;
    }

    public static Branch parse(File i_BranchFile) {
        Branch newBranch = new Branch();

        try(Scanner scanner = new Scanner(i_BranchFile)) {
            String pointedCommitSha1 = "";

            if(scanner.hasNextLine()) {
                pointedCommitSha1 = scanner.nextLine();
            }

            newBranch.setPointedCommitSha1(pointedCommitSha1);
            newBranch.setName(i_BranchFile.getName().substring(0,
                    i_BranchFile.getName().length() - TXT_FILE_SUFFIX.length()));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return newBranch;
    }
}
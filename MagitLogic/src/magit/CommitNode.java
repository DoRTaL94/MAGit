package magit;

import data.structures.Branch;
import data.structures.Commit;
import org.apache.commons.codec.digest.DigestUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CommitNode implements Comparable {
    private final CommitNode firstParent;
    private final CommitNode secondParent;
    private final List<CommitNode> children;
    private Commit commit;
    private List<Branch> pointingBranches;
    private List<Branch> onBranches;

    public CommitNode() {
        onBranches = new ArrayList<>();
        firstParent = null;
        secondParent = null;
        children = new ArrayList<>();
        pointingBranches = new ArrayList<>();
        commit = null;
    }

    public CommitNode(CommitNode i_FirstParent, CommitNode i_SecondParent) {
        onBranches = new ArrayList<>();
        firstParent = i_FirstParent;
        secondParent = i_SecondParent;
        children = new ArrayList<>();
        pointingBranches = new ArrayList<>();
        commit = null;
    }

    public String getSha1() {
        String sha1 = "";

        if(commit != null) {
            return DigestUtils.sha1Hex(commit.toStringForSha1());
        }

        return sha1;
    }

    public void addChildren(CommitNode i_Child) {
        children.add(i_Child);
        children.sort(this::compareCommitNodes);
    }

    private int compareCommitNodes(CommitNode i_N1, CommitNode I_N2) {
        return i_N1.compareTo(i_N1);
    }

    public CommitNode getFirstParent() {
        return firstParent;
    }

    public CommitNode getSecondParent() {
        return secondParent;
    }

    public List<CommitNode> getChildren() {
        return children;
    }

    public Commit getCommit() {
        return commit;
    }

    public void setCommit(Commit i_Commit) {
        commit = i_Commit;
    }

    public List<Branch> getPointingBranches() {
        return pointingBranches;
    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual;

        if(this == o) {
            isEqual = true;
        }
        else if(!(o instanceof CommitNode)) {
            isEqual = false;
        }
        else {
            CommitNode nodeToCompare = (CommitNode) o;
            isEqual = this.getSha1().equals(nodeToCompare.getSha1());
        }

        return isEqual;
    }

    @Override
    public int compareTo(Object o) {
        CommitNode nodeToCompare = (CommitNode) o;

        long nodeToCompareTime = 0;
        long thisNodeTime = 0;

        try {
            nodeToCompareTime = new SimpleDateFormat(Engine.DATE_FORMAT)
                    .parse(nodeToCompare.getCommit().getLastUpdate()).getTime();
            thisNodeTime = new SimpleDateFormat(Engine.DATE_FORMAT)
                    .parse(this.getCommit().getLastUpdate()).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return (int)(thisNodeTime - nodeToCompareTime);
    }

    public List<Branch> getOnBranches() {
        return onBranches;
    }
}

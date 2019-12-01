package utils;

import data.structures.Commit;
import data.structures.Repository;

public class RepositoryDetails {
    private String name = "-",
            activeBranchName = "master",
            commitLastUpdate = "-",
            commitMessage = "-",
            owner = "-";
    int branchesCount = 0;
    boolean isForked = false;
    String usernameForkedFrom = null;

    public RepositoryDetails(Repository i_Repository) {
        if(i_Repository != null) {
            name = i_Repository.getName();
            owner = i_Repository.getOwner();
            isForked = i_Repository.isForked();
            usernameForkedFrom = i_Repository.getUsernameForkedFrom();

            if(i_Repository.getHeadBranch() != null) {
                activeBranchName = i_Repository.getHeadBranch().getName();

                if(i_Repository.getCommits() != null && !i_Repository.getHeadBranch().getPointedCommitSha1().isEmpty()) {
                    Commit commit = i_Repository.getCommits().get(i_Repository.getHeadBranch().getPointedCommitSha1());
                    commitLastUpdate = commit.getLastUpdate();
                    commitMessage = commit.getMessage();
                }
            }

            if(i_Repository.getBranches() != null) {
                branchesCount = i_Repository.getBranches().size();
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActiveBranchName() {
        return activeBranchName;
    }

    public void setActiveBranchName(String activeBranchName) {
        this.activeBranchName = activeBranchName;
    }

    public String getCommitLastUpdate() {
        return commitLastUpdate;
    }

    public void setCommitLastUpdate(String commitLastUpdate) {
        this.commitLastUpdate = commitLastUpdate;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getBranchesCount() {
        return branchesCount;
    }

    public void setBranchesCount(int branchesCount) {
        this.branchesCount = branchesCount;
    }

    public boolean isForked() {
        return isForked;
    }

    public void setForked(boolean forked) {
        isForked = forked;
    }

    public String getUsernameForkedFrom() {
        return usernameForkedFrom;
    }

    public void setUsernameForkedFrom(String usernameForkedFrom) {
        this.usernameForkedFrom = usernameForkedFrom;
    }
}

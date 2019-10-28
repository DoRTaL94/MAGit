package utils;

import data.structures.Commit;
import data.structures.Repository;

public class RepositoryDetails {
    private String name = "N/A",
            activeBranchName = "master",
            commitLastUpdate = "N/A",
            commitMessage = "N/A",
            owner = "N/A";
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
}

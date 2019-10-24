import { getFileDataInCurrentDir, getPrevFoldersStack, getRecentFolderSha1, repository} from "./active-repo.js";
export { getMapSize, isFunction, sortFiles, getSortedCommitsSha1s, getParentsFoldersNames, getRootFolderSha1 };

function getMapSize(object, filter) {
    let count = 0;
    let isFunc = isFunction(filter);

    for(let key in object) {
        if(isFunc && filter(key)) {
            count++;
        } else if(!isFunc) {
            count++;
        }
    }

    return count;
}

function isFunction(functionToCheck) {
    return functionToCheck && {}.toString.call(functionToCheck) === '[object Function]';
}

function getSortedCommitsSha1s(repository) {
    let commits = repository.commits;
    let commitsSha1s = [];

    for(let commitSha1 in commits) {
        if(commitSha1.length === 40) {
            commitsSha1s.push(commitSha1);
        }
    }

    commitsSha1s.sort(function (first, second) {
        return commits[second].creationTimeMillis - commits[first].creationTimeMillis;
    });

    return commitsSha1s;
}

function sortFiles(files) {
    files.sort(function (file1, file2) {
        if(file1.type === file2.type) {
            return file2.creationTimeMillis - file1.creationTimeMillis;
        } else if(file1.type === 'FOLDER' && file2.type !== 'FOLDER') {
            return -1;
        } else if(file1.type !== 'FOLDER' && file2.type === 'FOLDER') {
            return 1;
        }
    });
}

function getParentsFoldersNames() {
    let prevFoldersStack = getPrevFoldersStack();
    let recentFolderSha1 = getRecentFolderSha1();

    prevFoldersStack.push(recentFolderSha1);

    let prevFoldersStackLength = prevFoldersStack.length;
    let parentFoldersNames = [ repository.repoName ];
    let parentFolderSha1 = getRootFolderSha1();

    for(let sha1 = 1; sha1 < prevFoldersStackLength; sha1++) {
        let childFolderSha1 = prevFoldersStack[sha1];
        let childData = getFileDataInCurrentDir(childFolderSha1, parentFolderSha1);
        parentFoldersNames.push(childData.name);
        parentFolderSha1 = childData.sha1;
    }

    prevFoldersStack.pop();
    parentFoldersNames.push(recentFolderSha1);

    return parentFoldersNames;
}

function getRootFolderSha1() {
    let rootFolderSha1;

    if(repository !== null) {
        if(repository.headBranch !== null) {
            let commitSha1 = repository.headBranch.pointedCommitSha1;

            if(commitSha1 !== null && repository.commits !== null && commitSha1 in repository.commits) {
                rootFolderSha1 = repository.commits[commitSha1].rootFolderSha1;
            }
        }
    }

    return rootFolderSha1;
}
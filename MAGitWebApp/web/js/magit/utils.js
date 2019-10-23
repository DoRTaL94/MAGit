export { getMapSize, isFunction, sortFiles, getSortedCommitsSha1s };

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
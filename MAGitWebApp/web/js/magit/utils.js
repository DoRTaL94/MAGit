export { getMapSize, isFunction, sortFiles };

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
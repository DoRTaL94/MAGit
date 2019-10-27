import { getMapSize, sortFiles, getSortedCommitsSha1s, getParentsFoldersNames } from './utils.js';
import * as popups from "./popups.js";
export { ASSETS_LOCATION, getIsOpenChanges, setOpenChanges, getPrevFoldersStack, addBlob, addFolder, getFileDataInCurrentDir, wc, repository, getRecentFolderSha1, HOME }

window.uploadFile           = uploadFile;
window.updateRepo           = updateRepo;
window.openPrevFolder       = openPrevFolder;
window.editBlob             = editBlob;
window.saveBlob             = saveBlob;
window.openFolder           = openFolder;
window.saveFileName         = saveFileName;
window.deleteFile           = deleteFile;
window.enableSaveButton     = enableSaveButton;

const HEADER_LIST_ITEM      = "#header-drop-downs > nav > ul > li > ul > li";
const SERVER_ERROR_MESSAGE  = '<li>' + "Failed to send data to the server..." + '</li>';
const HOME                  = "profile.html";
const ASSETS_LOCATION       = "assets";
const lastCommitContent =
`<nav class="navbar navbar-expand-lg navbar-light bg-light">
    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarNavDropdown">
        <ul class="navbar-nav">
            <li class="nav-item dropdown active">
                <a class="Link nav-link dropdown-toggle" id="branch-drop-down" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false"></a>
                <div class="dropdown-menu Branches" aria-labelledby="navbarDropdownMenuLink"></div>
            </li>
            <li class="nav-item dropdown">
                <a class="Link nav-link dropdown-toggle" id="download-or-clone-dropdown" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    Download or Fork
                </a>
                <div class="dropdown-menu" aria-labelledby="navbarDropdownMenuLink">
                    <a class="Link dropdown-item" id="download">Download</a>
                    <a class="Link dropdown-item" id="fork">Fork</a>
                </div>
            </li>
        </ul>
    </div>
    <button id="btn-pull-request" type="button">Make pull request</button>
    <button id="btn-create-new-branch" type="button">Create new branch</button>
    <button id="btn-checkout" type="button">Checkout</button>
</nav>
<div class="list-group Commits-list">
    <a id="commit-list-header" class="list-group-item disabled">
        <div class="Table">
            <div class="Table-row">
                <div class="Table-cell">Pointed commit:</div>
                <div class="Table-cell"><img id="commiter-icon" src="../assets/user-icon.png" alt="commiter icon"/><span id="commiter-name"></span></div>
                <div class="Table-cell commit-description"></div>
                <div class="Table-cell commit-sha1"></div>
                <div class="Table-cell file-last-update"></div>
            </div>
        </div>
    </a>
    <div class="Separator Blue"></div>
    <div id="root-folder-files"></div>
</div>`;
const uploadForm =
`<form id="uploadForm" action="import" enctype="multipart/form-data" method="POST">
    <input class="File-chooser" accept=".xml" type="file" name="repoXml">
    <input class="Btn-file-upload" type="submit">
</form>`;

const btnUpload =
`<button id="btn-upload-file" class="btn btn-primary" type="button" onclick="uploadFile()">
    <div id="loading-container">
        <div class="lds-ring"><div></div><div></div><div></div><div></div></div>
    </div>
    <span id="import-btn-text">Import repository</span>
</button>`;

export let isOpenChanges = false;
let messagesForm ='<ul id="form-messages"></ul>';
let wc = null;
let repository = null;
let isActiveBranch = null;
let recentFolderSha1 = null;
let prevFoldersStack = [];
let isOwnRepo;

$(onLoad);

function onLoad() {
    setListsItemsOnClick();
    updateActiveRepo();
    setOnCommitsClick();
    setOnTabsClick();
}

function setOnTabsClick() {
    $('#code-editor-tab').on('click', function () {
        prevFoldersStack = [];
        recentFolderSha1 = wc.rootSha1;
        updateRootFolder(wc, recentFolderSha1, '#wc-files-list', prevFoldersStack);
    });

    $('#code-tab').on('click', function () {
        prevFoldersStack = [];
        recentFolderSha1 = repository.commits[repository.headBranch.pointedCommitSha1].rootFolderSha1;
        updateRootFolder(repository, recentFolderSha1, '#root-folder-files', prevFoldersStack);
    });

    $('#diff-tab').on('click', function () {
        $.ajax({
            method:'POST',
            data: 'commit=' + repository.headBranch.pointedCommitSha1,
            url: 'difference',
            timeout: 4000,
            error: function (response) {
                console.log(response);
            },
            success: updateDiff
        });
    })
}

function updateDiff(response) {
    let diff = response[0];
    let newFiles = diff.newFiles;
    let changedFiles = diff.changedFiles;
    let deletedFiles = diff.deletedFiles;

    if(newFiles.length > 0) {
        sortFiles(newFiles);
        updateDiffContainer(diff, newFiles, '#new-files-diff > .diff-items-container');
    }

    if(changedFiles.length > 0) {
        sortFiles(changedFiles);
        updateDiffContainer(diff, changedFiles, '#changed-files-diff > .diff-items-container');
    }

    if(deletedFiles.length > 0) {
        sortFiles(deletedFiles);
        updateDiffContainer(diff, deletedFiles, '#deleted-files-diff > .diff-items-container');
    }

}

function updateDiffContainer(dataBase, diffList, containerId) {
    let length = diffList.length;
    let container = $(containerId);
    container.empty();

    for(let diff = 0; diff < length; diff++) {
        container.append(buildDiffItem(dataBase, diffList[diff], containerId));
    }
}

function buildDiffItem(dataBase, item, containerId) {
    let icon = item.type === 'FOLDER' ? 'folder-icon' : 'file-icon';

    return `<div class="Commit-list-item">
    <a ${ buildAtrributes(item) } id="file-${ item.sha1 }" role="button" class="Link list-group-item list-group-item-action">
        <div class="Table">
            <div class="Table-row">
                <div class="Table-cell file-name"><img id="${ icon }" src="../${ASSETS_LOCATION}/${ icon }.svg"/> <span class="${ getFileExtension(item) }" id="file-name-${ item.sha1 }">${ item.name }</span></div>
                <div class="Table-cell file-last-changer">${ item.lastChanger }</div>
                <div class="Table-cell file-sha1">${ item.sha1 }</div>
                <div class="Table-cell file-last-update">${ item.lastUpdate }</div>  
            </div>
        </div>
    </a>
    ${ buildContentExpander(dataBase, item, containerId) }
</div>`;
}

function setOnCommitsClick() {
    $('#commits-count').on('click', onCommitsClick)
}

function onCommitsClick() {
    let lastCommitContentContainer = $('#last-commit-content');
    let codeContainer = $('#code');

    if($('#last-commit-back').length === 0) {
        codeContainer.append(`<div id="last-commit-back">Last commit <span class="Back-caret"></span></div>`);
    }

    $('#last-commit-back').on('click', onLastCommitBackClick);

    if($('#commits-header-title > #title').length === 0) {
        $('#commits-header-title').append('<span id="title">Commits</span>');
    }

    lastCommitContentContainer.empty();
    lastCommitContentContainer.append(`<div class="list-group" id="commit-list-items"></div>`);

    buildCommitsList();
}

function buildCommitsList() {
    let commitsSha1s = getSortedCommitsSha1s(repository);
    let sha1ToBranchMap = getCommitSha1ToBranchMap();

    commitsSha1s.forEach(function (sha1) {
        let commit = repository.commits[sha1];
        let pointingBranches = '';

        if(sha1ToBranchMap.has(sha1)) {
            sha1ToBranchMap.get(sha1).forEach(function (branch) {
                pointingBranches += `<div class="Table-cell"><span class="Pointing-branch">${ branch.name }</span></div>`;
            })
        }

        $('.list-group').append(
`<a class="list-group-item Commit-list-item" href="#commit-${ commit.sha1 }" data-toggle="collapse" aria-expanded="false" aria-controls="commit-${ commit.sha1 }">
    <div class="Table">
        <div class="Table-row">
            <div class="Table-cell"><img id="commiter-icon" src="../assets/user-icon.png" alt="commiter icon"/> ${ commit.lastChanger }</span></div>
            ${ pointingBranches }
            <div class="Table-cell Commit-sha1">${ commit.sha1 }</div>
            <div class="Table-cell commit-description">${ commit.message }</div>
            <div class="Table-cell file-last-update">${ commit.lastUpdate }</div>
        </div>
    </div>
</a>
<div class="collapse" id="commit-${ commit.sha1 }"><div class="commit-files" id="commit-files-${ commit.sha1 }"></div></div>`);

        prevFoldersStack = [];
        updateRootFolder(repository, commit.rootFolderSha1, `#commit-files-${ commit.sha1 }`, prevFoldersStack);
    });

    $('.list-group > a').on('click', function () {
        let commitSha1 = $(this).find('.Commit-sha1').text();

        $('.list-group').find('a').each(function () {
            let otherCommitSha1 = $(this).find('.Commit-sha1').text();

            if(otherCommitSha1 !== commitSha1) {
                if($(this).attr('aria-expanded') === 'true') {
                    $(this).trigger("click");
                }

                $(this).removeClass('active');
            }
        });

        prevFoldersStack = [];
        recentFolderSha1 = repository.commits[commitSha1].rootFolderSha1;

        updateRootFolder(repository, recentFolderSha1, `#commit-files-${ commitSha1 }`, prevFoldersStack);

        $(this).addClass('active');
    });

}

function getCommitSha1ToBranchMap() {
    let sha1ToBranchMap = new Map();

    for(let sha1 in repository.commits) {
        sha1ToBranchMap.set(sha1, []);
    }

    for(let branchName in repository.branches) {
        let branch = repository.branches[branchName];
        let pointedCommitSha1 = branch.pointedCommitSha1;
        sha1ToBranchMap.get(pointedCommitSha1).push(branch);
    }

    return sha1ToBranchMap;
}

function onLastCommitBackClick() {
    $('#last-commit-back').remove();
    $('#commits-header-title > span').remove();
    updateRepo(repository.headBranch.name);
    $('#download').on('click', () => window.location.href = `download?repository=${ repository.repoName }`);
    $('#fork').on('click', fork);
}

function fork() {
    $.ajax({
        method:'POST',
        data: 'repository=' + repository.repoName,
        url: 'fork',
        timeout: 4000,
        error: function (response) {
            console.log(response);
        },
        success: function (response) {
            if(response === 'success') {
                window.location.href = 'profile.html';
            }
        }
    });
}

function getIsOpenChanges() {
    return isOpenChanges;
}

function setOpenChanges(state) {
    isOpenChanges = state;
}

function setListsItemsOnClick() {
    setListItemImportXmlOnClick();
}

function setListItemImportXmlOnClick() {
    let importXml = $(HEADER_LIST_ITEM).find("#import-repo-xml");
    importXml.on('click', importXmlOnClick);
}

// Sends ajax request to the server with the xml file.
// We're using 'formData' which is a javascript class that wraps the data that we want to send.
// 'formData' attaches the data with a parameter name - in this code the parameter name
// of the xml file is 'repoXml'.
// 'ImportRepoServlet' is the servlet that listens to this ajax call.
// Further explanation could be found in 'ImportRepoServlet'.
function importXmlOnClick() {
    showImportXml();

    $("#uploadForm").submit(function () {
        let xmlFile = this[0].files[0];
        let formData = new FormData();

        formData.append("repoXml", xmlFile);

        $.ajax({
            method:'POST',
            data: formData,
            url: this.action,
            processData: false, // Don't process the files
            contentType: false, // Set content type to false as jQuery will tell the server its a query string request
            timeout: 4000,
            error: importXmlErrorFunc,
            success: importXmlSuccessFunc
        });

        return false;
    })
}

function showImportXml() {
    let container = $("div.Content");
    container.empty();
    container.addClass("Content-relative");

    let importContainer = $("<div>").addClass("Import-container");
    let importHiddenForm = $(uploadForm);
    let fileChooserContainer = $("<div>").addClass("File-chooser-container");
    let uploadBtnContainer = $("<div>").addClass("Btn-upload-container");
    let btnChooseFile = $("<button>").addClass("Btn-choose-file").text("Choose file");
    let btnUploadFile = $(btnUpload);
    let filePath = $("<span>").addClass("File-path").text("No file chosen");
    let messages = $(messagesForm);

    btnChooseFile.click(chooseFile);
    importContainer.append(messages);
    fileChooserContainer.append(btnChooseFile);
    fileChooserContainer.append(filePath);
    uploadBtnContainer.append(btnUploadFile);
    importContainer.append(fileChooserContainer);
    importContainer.append(uploadBtnContainer);
    importContainer.append(importHiddenForm);
    container.append(importContainer);
}

function chooseFile() {
    let fileChooser = $(".File-chooser");
    fileChooser.click();
    fileChooser.change(function() {
        if(fileChooser[0].files.length > 0) {
            let filename = fileChooser[0].files[0];
            $('.File-path').text(filename.name);
        } else {
            $('.File-path').text("No file chosen");
        }
    });
}

function uploadFile() {
    let messages = $('#form-messages');

    if($(".File-chooser")[0].files.length === 0) {
        messages.css('display', 'block');
        messages.empty();
        messages.append('<li>' + "Please choose xml file." + '</li>');
    } else {
        messages.css('display', 'none');
        $("#import-btn-text").text("Uploading...");
        $(".lds-ring div").css("display", "block");
        $("#btn-upload-file").prop('disabled',true);
        $(".Btn-choose-file").prop('disabled',true);
        $(".Btn-file-upload").click();
    }
}

// Function that called when an error response sent from the server.
// When it called we would print to 'form-messages' ul an error message.
function importXmlErrorFunc() {
    let messages = $('#form-messages');
    messages.css('display', 'block');
    messages.empty();
    messages.append(SERVER_ERROR_MESSAGE);
    $("#import-btn-text").text("Import repository");
    $(".lds-ring div").css("display", "none");
    $("#btn-upload-file").prop('disabled',false);
    $(".Btn-choose-file").prop('disabled',false);
}

// Function that called when a success response is sent from the server.
// Success response is being sent if there are some logic errors or if the xml file is upload successfully.
// If there will be logic errors they will be printed inside 'form-messages' ul.
// If the upload was executed successfully we will redirect the user to the main page,
// where he could see the current repository.
function importXmlSuccessFunc(response) {
    if($.isArray(response)) {
        let messages = $('#form-messages');
        messages.empty();

        if (response.length > 0) {
            messages.css('display', 'block');

            for (let i = 0; i < response.length; i++) {
                messages.append('<li>' + response[i] + '</li>');
            }

            $("#import-btn-text").text("Import repository");
            $(".lds-ring div").css("display", "none");
            $("#btn-upload-file").prop('disabled',false);
            $(".Btn-choose-file").prop('disabled',false);
        } else {
            messages.css('display', 'none');
        }
    } else {
        window.location.href = HOME;
    }
}

function updateActiveRepo() {
    $.ajax({
        method: 'POST',
        data: '',
        url: 'update-repo',
        timeout: 3000,
        error: function (response) {
            console.log(response);
        },
        success: onUpdateRepoSuccess
    });
}

function onUpdateRepoSuccess(response) {
    if(response !== 'User has no repositories') {
        repository      = response['repository'];
        isOpenChanges   = response['isOpenChanges'];
        wc              = response['wc'];
        isOwnRepo       = response['isOwnRepo'];

        updateRepo(repository.headBranch.name);
        $('#download').on('click', () => window.location.href = `download?repository=${ repository.repoName }`);
        $('#fork').on('click', fork);
    }
}

function updateRepo(branchName) {
    let currCommit = repository.commits[repository.branches[branchName].pointedCommitSha1];
    let lastCommitContentContainer = $('#last-commit-content');
    lastCommitContentContainer.empty();
    lastCommitContentContainer.append(lastCommitContent);

    initButtons(branchName);
    initHeaderOfLastCommitFilesList(branchName);
    initPullRequests();
    updateRootFolder(repository, currCommit.rootFolderSha1, '#root-folder-files', prevFoldersStack);
    handleNotOwnRepo();
}

function initPullRequests() {
    let prBranches = repository.prBranches;

    if(prBranches != null) {
        $('#pull-requests-list').empty();

        for(let branch in prBranches) {
            buildPullRequest(branch);
        }
    }
}

function buildPullRequest(branch) {
    return `<div class="Commit-list-item">
    <a href="#idofdiffexpand" data-toggle="collapse" aria-expanded="false" aria-controls="idofdiffexpand" id="" role="button" class="Link list-group-item list-group-item-action">
        <div class="Table">
            <div class="Table-row">
                <div class="Table-cell"></div>
                <div class="Table-cell"></div>
                <div class="Table-cell"></div>
                <div class="Table-cell"></div>  
            </div>
        </div>
    </a>
    // need to get from server the diff of this branch
    ${ buildContentExpander(repository, branch, '#pull-requests-list') }
</div>`
}

function initHeaderOfLastCommitFilesList(branchName) {
    let branch = repository.branches[branchName];
    let commitListHeader = $('#commit-list-header');
    let currCommit = repository.commits[branch.pointedCommitSha1];
    let branchesList = $('.Branches');

    branchesList.empty();

    commitListHeader.find('span#commiter-name').empty().text(currCommit.lastChanger);
    commitListHeader.find('div.commit-description').empty().text(currCommit.message);
    commitListHeader.find('div.commit-sha1').empty().text(currCommit.sha1);
    commitListHeader.find('div.file-last-update').empty().text(currCommit.lastUpdate);

    $('.Repo-name').empty().text(`${ repository.owner } / ${ repository.repoName + (repository.isForked === true ? ' (forked from user: ' + repository.usernameForkedFrom +')' : '') }`);
    $('#branch-drop-down').empty().text(`Branch: ${ branch.name }`);
    $('#branches-count').text(`${ getMapSize(repository.branches, key => true) } branches`);
    $('#commits-count').text(`${ getMapSize(repository.commits, key => key.length === 40) } commits`);

    let branches = repository.branches;

    for(let name in branches) {
        if(!branches[name].isPullRequested) {
            let headBranchMark = name === repository.headBranch.name ? `<img id="head-branch-mark" src="../${ ASSETS_LOCATION }/head-branch.svg"/>` : '';
            branchesList.append(`<a class="Link dropdown-item" id="${ name }" onclick="updateRepo(this.id)">${ headBranchMark } ${ name }</a>`)
        }
    }
}

function handleNotOwnRepo() {
    if(isOwnRepo === false) {
        $('#code-editor-tab').remove();
        $('#diff-tab').remove();
        $('#btn-commit').remove();
        $('#btn-create-file').remove();
        $('#btn-create-new-branch').remove();
        $('#wc-files-list').remove();
        $('#btn-checkout').remove();
        $('#btn-pull-request').remove();
    }

    if(repository.isForked) {
        $('#pull-requests-tab').remove();
    }
}

function initButtons(branchName) {
    let btnPullReq = $('#btn-pull-request');

    isActiveBranch = repository.headBranch.name === branchName;
    btnPullReq.prop('disabled', true);

    if(!isActiveBranch && isOwnRepo) {
        let btnCheckout = $('#btn-checkout');

        btnCheckout.prop('disabled', false);
        $('#btn-create-new-branch').prop('disabled', true);
        $('#btn-commit').prop('disabled', true);
        btnCheckout.on('click', onBtnCheckoutClick)
    } else if(isOwnRepo) {
        let btnCreateNewBranch = $('#btn-create-new-branch');
        let btnCommit = $('#btn-commit');

        btnCreateNewBranch.prop('disabled', false);
        btnCreateNewBranch.on('click', popups.showCreateNewBranchPopup);

        $('#btn-checkout').prop('disabled', true);
        btnCommit.prop('disabled', !isOpenChanges);
        btnCommit.on('click', popups.showCommitPopup);

        if(repository.isForked) {
            btnPullReq.prop('disabled', false);
            btnPullReq.on('click', popups.showPullRequestPopup);
        }
    }

    if(isOwnRepo) {
        $('#btn-create-file').on('click', popups.showCreateFilePopup);
    }
}

function onBtnCheckoutClick() {
    if(isOpenChanges) {
        popups.showOpenChangesPopup(checkout);
    } else {
        checkout();
    }
}

function checkout() {
    isOpenChanges = false;
    let branchName = $('#branch-drop-down').text();
    branchName = branchName.substring(8);

    $.ajax({
        method: 'POST',
        data: 'branchname=' + branchName + '&checkwc=false',
        url: 'checkout',
        timeout: 2000,
        error: function(response) {
            console.log(response);
        },
        success: function () {
            window.location.href = 'active-repo.html';
        }
    });
}

function updateRootFolder(dataBase, folderSha1, containerId, foldersStack) {
    let folder = dataBase.folders[folderSha1];
    let container = $(containerId);
    container.empty();

    if(folder.isRoot === false && foldersStack.length > 0) {
        container.append(
`<a id="${ foldersStack[foldersStack.length - 1] }" class="prev-folder Link Commit-list-item list-group-item list-group-item-action">
    <div class="Table">
        <div class="Table-row">
            <div class="Table-cell">
                <span class="Back-caret-left"/> Back
            </div>
        </div>
    </div>
</a>`);

        $('.prev-folder').on('click', function () {
            openPrevFolder(dataBase, containerId, foldersStack);
        });
    }

    sortFiles(folder.files);
    folder.files.forEach(file => addFileToList(dataBase, file, containerId, foldersStack));
    recentFolderSha1 = folderSha1;
}

function addFileToList(dataBase, file, containerId, foldersStack) {
    let container = $(containerId);

    if(file !== 'undefined') {
        let id = `#file-${file.sha1}`;
        container.append(buildListItem(dataBase, file, containerId, foldersStack));

        if(containerId === '#wc-files-list' && isOwnRepo) {
            let editMode = `#edit-mode-${file.sha1}`;
            $(editMode).hover(onEditModeMouseEnter, onEditModeMouseLeave);
            $(editMode).click(onEditModeClicked);
        }

        container.find(id).on('click', function () {
            container.find('a').each(function () {
                $(this).removeClass('active');
            });
            $(this).addClass('active');

            if(file.type === 'FOLDER') {
                openFolder(dataBase, file.sha1, containerId, foldersStack);
            }
        });
    }
}

function onEditModeClicked() {
    if($(this).attr('alt')==='edit-mode') {
        $(this).attr('alt', 'edit-mode-selected');
        $(this).attr('src', `../${ ASSETS_LOCATION }/edit-mode-pencil-selected.svg`);
    } else {
        $(this).attr('alt', 'edit-mode');
        $(this).attr('src', `../${ ASSETS_LOCATION }/edit-mode-pencil.svg`);
    }
}

function onEditModeMouseEnter() {
    $(this).attr('src', `../${ ASSETS_LOCATION }/edit-mode-pencil-selected.svg`);
}

function onEditModeMouseLeave() {
    if($(this).attr('alt') !== 'edit-mode-selected') {
        $(this).attr('src', `../${ ASSETS_LOCATION }/edit-mode-pencil.svg`);
    }
}

function buildListItem(dataBase, file, containerId, foldersStack) {
    let icon = file.type === 'FOLDER' ? 'folder-icon' : 'file-icon';
    let editModeIcon = buildEditModeIcon(file, containerId);
    let editModeExpander = buildEditModeExpander(file, containerId);

    return `<div class="Commit-list-item">
    ${ editModeIcon }
    <a ${ buildAtrributes(file) } id="file-${ file.sha1 }" role="button" class="Link list-group-item list-group-item-action">
        <div class="Table">
            <div class="Table-row">
                <div class="Table-cell file-name"><img id="${ icon }" src="../${ASSETS_LOCATION}/${ icon }.svg"/> <span class="${ getFileExtension(file) }" id="file-name-${ file.sha1 }">${ file.name }</span></div>
                <div class="Table-cell file-last-changer">${ file.lastChanger }</div>
                <div class="Table-cell file-sha1">${ file.sha1 }</div>
                <div class="Table-cell file-last-update">${ file.lastUpdate }</div>  
            </div>
        </div>
    </a>
    ${ editModeExpander }
    ${ buildContentExpander(dataBase, file, containerId) }
</div>`;
}

function buildEditModeIcon(file, containerId) {
    return containerId === '#wc-files-list' && isOwnRepo ?
`<a href="#edit-mode-content-${ file.sha1 }" data-toggle="collapse" aria-expanded="false" aria-controls="edit-mode-content-${ file.sha1 }">
    <img id="edit-mode-${ file.sha1 }" class="edit-mode-pencil" src="../${ASSETS_LOCATION}/edit-mode-pencil.svg" alt="edit-mode"/>
</a>` : '';
}

function buildAtrributes(file) {
    return file.type === 'BLOB' ? `href="#content-${ file.sha1 }" data-toggle="collapse" aria-expanded="false" aria-controls="content-${ file.sha1 }"` : '';
}

function getFileExtension(file) {
    let fileExtension = '';

    if(file.type !== 'FOLDER') {
        let period = file.name.lastIndexOf('.');
        fileExtension = file.name.substring(period);
    }

    return fileExtension;
}

function buildEditModeExpander(file, containerId) {
    return containerId === '#wc-files-list' && isOwnRepo ?
        `<div class="collapse" id="edit-mode-content-${ file.sha1 }">
    <div class="Table edit-mode-content card card-body">
        <div class="Table-row">
            <div class="Table-cell">
                <label>
                    Edit file name <input type="text" onkeydown="enableSaveButton('${ file.sha1 }')" onkeyup="enableSaveButton('${ file.sha1 }')" id="file-new-name-${ file.sha1 }" class="New-name-input" name="newname" spellcheck="false">
                </label>
            </div>
            <div class="Table-cell">
                <button id="btn-save-${ file.sha1 }" type="button" onclick="saveFileName('${ file.sha1 }')" class="Save" disabled>Save</button>
            </div>
            <div class="Table-cell">
                <button type="button" onclick="deleteFile('${ file.sha1 }', '${ file.name }')" class="Delete">Delete file</button>
            </div>
        </div>  
    </div>
</div>` : '';
}

function enableSaveButton(sha1) {
    let inputText = $('#file-new-name-' + sha1).val();
    let btnSave = $('#btn-save-' + sha1);

    if(inputText !== '' && btnSave.prop('disabled') === true) {
        btnSave.prop('disabled', false);
    } else if(inputText === '') {
        btnSave.prop('disabled', true);
    }
}

function buildContentExpander(dataBase, file, containerId) {
    let contentEditor = '';
    let blobText = '';

    if(file.type === 'BLOB') {
        contentEditor = containerId === '#wc-files-list' && isOwnRepo ? `<div class="Editor"><button type="button" id="${file.sha1}-edit" class="Edit" onclick="editBlob('${file.sha1}')">Edit</button><button type="button" id="${file.sha1}-save" onclick="saveBlob('${file.sha1}')" class="Save" disabled>Save</button></div>` : '';
        blobText = dataBase.blobs[file.sha1].text;
    }

    return file.type === 'FOLDER' ? '' :
`<div class="collapse" id="content-${ file.sha1 }">
    <div class="blob-content card card-body">
        ${ contentEditor }
        <div class="Editable">${ blobText }</div>
    </div>
</div>`;
}

function saveFileName(sha1) {
    let fileName = $('#file-name-' + sha1);
    let oldName = fileName.text();
    let newName = $('#file-new-name-' + sha1).val() + fileName.attr('class');
    fileName.text(newName);

    if(oldName !== newName) {
        isOpenChanges = true;
        $('#btn-commit').prop('disabled', !isOpenChanges);
    }

    updateFileData(sha1, oldName, 'POST', newName, 'update-file-name', null, null);
    let file = getFileDataInCurrentDir(sha1, recentFolderSha1);
    file.name = newName;
}

function getFileDataInCurrentDir(sha1, currFolderSha1) {
    let files = wc.folders[currFolderSha1].files;
    let length = files.length;
    let file = null;

    for(let i = 0; i < length; i++) {
        if(files[i].sha1 === sha1) {
            file = files[i];
            break;
        }
    }

    return file;
}

function deleteFile(sha1, name) {
    let files = wc.folders[recentFolderSha1].files;
    let length = files.length;

    $('#wc-files-list').on('click', '.Delete', function() {
        $(this).closest('.Commit-list-item').remove();
    });

    updateFileData(sha1, name, 'POST', '', 'delete-file', null, null);

    for(let i = 0; i < length; i++) {
        if(sha1 === files[i].sha1) {
            delete files[i];
        }
    }

    isOpenChanges = true;
    $('#btn-commit').prop('disabled', !isOpenChanges);
}

function saveBlob(sha1) {
    setSavedStyle(sha1);

    let editor = $(`#wc-files-list > .Commit-list-item > #content-${sha1} > .blob-content > .Editable`);
    let blob = wc.blobs[sha1];

    if(blob.text !== editor.text()) {
        isOpenChanges = true;
        $('#btn-commit').prop('disabled', !isOpenChanges);
        blob.text = editor.text();
        updateFileData(sha1, blob.name, 'POST', editor.text(), 'update-blob', null, null);
    }
}

function getPrevFoldersStack() {
    return prevFoldersStack;
}

function getRecentFolderSha1() {
    return recentFolderSha1;
}

function updateFileData(fileSha1, fileName, method, data, url, onSuccess, onError) {
    let parentsFoldersNames = getParentsFoldersNames();
    parentsFoldersNames.push(fileSha1);
    parentsFoldersNames.push(fileName);
    parentsFoldersNames.push(data);

    let dataToSend = 'data=' + JSON.stringify(parentsFoldersNames);

    $.ajax({
        method: method,
        data: dataToSend,
        url: url,
        timeout: 3000,
        error: onError,
        success: onSuccess
    });
}

function editBlob(sha1) {
    setEditStyle(sha1);
}

function setSavedStyle(sha1) {
    let btnEdit = $(`#${ sha1 }-edit`);
    let blobContent = $(`#wc-files-list > .Commit-list-item > #content-${sha1} > .blob-content`);

    $(`#${ sha1 }-save`).prop('disabled', true);

    if(btnEdit.hasClass('Edit-selected')) {
        btnEdit.removeClass('Edit-selected');
    }

    if(blobContent.hasClass('blob-content-edit-mode')) {
        blobContent.removeClass('blob-content-edit-mode');
        setNotEditable($(`#wc-files-list > .Commit-list-item > #content-${sha1} > .blob-content > .Editable`));
    }
}

function setEditStyle(sha1) {
    let editor = $(`#wc-files-list > .Commit-list-item > #content-${sha1} > .blob-content > div.Editable`);
    let isEditMode = editor.is('div');

    if(isEditMode) {
        let editableText = $("<textarea>").addClass('Editable');
        editableText.val(editor.text());
        editor.replaceWith(editableText);
        $(`#${sha1}-save`).prop('disabled', false);
        $(document).delegate('.Editable', 'keydown', onTabPressed);
    } else {
        setNotEditable(editor);
    }

    $(`#${sha1}-edit`).toggleClass('Edit-selected');
    $(`#wc-files-list > .Commit-list-item > #content-${sha1} > .blob-content`).toggleClass('blob-content-edit-mode');
}

function setNotEditable(editor) {
    let notEditable = $("<div>").addClass('Editable');
    notEditable.text(editor.val());
    editor.replaceWith(notEditable);
}

function onTabPressed(e) {
    let keyCode = e.keyCode || e.which;

    if (e.keyCode === 9 && e.shiftKey) {
        e.preventDefault();
        let start = this.selectionStart;
        let content = $(this).val();
        let i;

        for(i = start; i >= 0; i--) {
            if(i !== start && content[i] === '\n') {
                break;
            }

            if(content[i] === '\t') {
               $(this).val(content.substring(0, i) + content.substring(i + 1));
                this.selectionStart = this.selectionEnd = i;
                break;
            }
        }
    } else if (keyCode === 9) {
        e.preventDefault();
        let start = this.selectionStart;
        let end = this.selectionEnd;

        $(this).val($(this).val().substring(0, start)
            + "\t"
            + $(this).val().substring(end));

        this.selectionStart = this.selectionEnd = start + 1;
    }
}

function openPrevFolder(dataBase, containerId, foldersStack) {
    updateRootFolder(dataBase, foldersStack.pop(), containerId, foldersStack);
}

function openFolder(dataBase, sha1, containerId, foldersStack) {
    foldersStack.push(recentFolderSha1);
    updateRootFolder(dataBase, sha1, containerId, foldersStack);
}

function addBlob(parentFolderSha1, fileData, file) {
    addFile(wc.blobs, parentFolderSha1, fileData, file);
}

function addFolder(parentFolderSha1, fileData, file) {
    addFile(wc.folders, parentFolderSha1, fileData, file);
}

function addFile(dataBaseToPushFile, parentFolderSha1, fileData, file) {
    dataBaseToPushFile[`${fileData.sha1}`] = file;
    wc.folders[parentFolderSha1].files.push(fileData);
    addFileToList(wc, fileData, '#wc-files-list', prevFoldersStack);

    isOpenChanges = true;
    $('#btn-commit').prop('disabled', !isOpenChanges);
}
import { getMapSize, sortFiles, getSortedCommitsSha1s, getParentsFoldersNames } from './utils.js';
import * as popups from "./popups.js";
export { getIsOpenChanges, setOpenChanges, getPrevFoldersStack, addBlob, addFolder, getFileDataInCurrentDir, repository, getRecentFolderSha1, HOME }

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
                    Download or clone
                </a>
                <div class="dropdown-menu" aria-labelledby="navbarDropdownMenuLink">
                    <a class="Link dropdown-item" id="download">Download</a>
                    <a class="Link dropdown-item" id="clone">Clone</a>
                </div>
            </li>
        </ul>
    </div>
    <button id="btn-create-file" type="button">Create File</button>
    <button id="btn-commit" type="button">Commit</button>
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

let messagesForm ='<ul id="form-messages"></ul>';
let repository = null;
let isActiveBranch = null;
export let isOpenChanges = false;
let recentFolderSha1 = null;
let prevFoldersStack = [];

$(onLoad);

function onLoad() {
    setListsItemsOnClick();
    updateActiveRepo();
    setOnCommitsClick();
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

        updateRootFolder(commit.rootFolderSha1, `#commit-files-${ commit.sha1 }`, false);
    });

    $('.list-group > a').on('click', function () {
        let commitSha1 = $(this).find('.Commit-sha1').text();
        prevFoldersStack = [];
        recentFolderSha1 = repository.commits[commitSha1].rootFolderSha1;

        updateRootFolder(recentFolderSha1, `#commit-files-${ commitSha1 }`, false);

        $('.list-group').find('a').each(function () {
            let otherCommitSha1 = $(this).find('.Commit-sha1').text();

            if(otherCommitSha1 !== commitSha1) {
                if($(this).attr('aria-expanded') === 'true') {
                    $(this).trigger("click");
                }

                $(this).removeClass('active');
            }
        });

        $(this).addClass('active');
    });

}

function getCommitSha1ToBranchMap() {
    let sha1ToBranchMap = new Map();

    for(let branchName in repository.branches) {
        let branch = repository.branches[branchName];
        let pointedCommitSha1 = branch.pointedCommitSha1;

        if(pointedCommitSha1 in sha1ToBranchMap) {
            sha1ToBranchMap.get(pointedCommitSha1).push(branch);
        } else {
            sha1ToBranchMap.set(pointedCommitSha1, [ branch ]);
        }
    }

    return sha1ToBranchMap;
}

function onLastCommitBackClick() {
    $('#last-commit-back').remove();
    $('#commits-header-title > span').remove();
    updateRepo(repository.headBranch.name);
    $('#download').on('click', () => window.location.href = `download?repository=${ repository.repoName }`);
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
        error: onUpdateRepoError,
        success: onUpdateRepoSuccess
    });
}

function onUpdateRepoError(response) {

}

function onUpdateRepoSuccess(response) {
    if(response !== 'User has no repositories') {
        repository = response['repository'];
        isOpenChanges = response['isOpenChanges'];

        updateRepo(repository.headBranch.name);
        $('#download').on('click', () => window.location.href = `download?repository=${ repository.repoName }`);
    }
}

function updateRepo(branchName) {
    let lastCommitContentContainer = $('#last-commit-content');
    lastCommitContentContainer.empty();
    lastCommitContentContainer.append(lastCommitContent);

    let branch = repository.branches[branchName];
    let commitListHeader = $('#commit-list-header');
    let currCommit = repository.commits[branch.pointedCommitSha1];
    let branchesList = $('.Branches');

    isActiveBranch = repository.headBranch.name === branchName;
    branchesList.empty();

    if(!isActiveBranch) {
        let btnCheckout = $('#btn-checkout');

        btnCheckout.prop('disabled', false);
        $('#btn-commit').prop('disabled', true);

        btnCheckout.on('click', onBtnCheckoutClick)
    } else {
        let btnCommit = $('#btn-commit');

        $('#btn-checkout').prop('disabled', true);
        btnCommit.prop('disabled', !isOpenChanges);
        btnCommit.on('click', popups.showCommitPopup);
    }

    $('#btn-create-file').on('click', popups.showCreateFilePopup);

    commitListHeader.find('span#commiter-name').empty().text(currCommit.lastChanger);
    commitListHeader.find('div.commit-description').empty().text(currCommit.message);
    commitListHeader.find('div.commit-sha1').empty().text(currCommit.sha1);
    commitListHeader.find('div.file-last-update').empty().text(currCommit.lastUpdate);

    $('.Repo-name').empty().text(`${ repository.owner } / ${ repository.repoName }`);
    $('#branch-drop-down').empty().text(`Branch: ${ branch.name }`);
    $('#branches-count').text(`${ getMapSize(repository.branches, key => true) } branches`);
    $('#commits-count').text(`${ getMapSize(repository.commits, key => key.length === 40) } commits`);

    for(let key in repository.branches) {
        if(repository.branches.hasOwnProperty(key)) {
            let headBranchMark = key === repository.headBranch.name ? `<img id="head-branch-mark" src="../${ ASSETS_LOCATION }/head-branch.svg"/>` : '';
            branchesList.append(`<a class="Link dropdown-item" id="${ key }" onclick="updateRepo(this.id)">${ headBranchMark } ${ key }</a>`)
        }
    }

    updateRootFolder(currCommit.rootFolderSha1, '#root-folder-files', true);
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

function updateRootFolder(folderSha1, containerId, isEnableEditModeIfActiveBranch) {
    let folder = repository.folders[folderSha1];
    let container = $(containerId);
    container.empty();

    if(folder.isRoot === false && prevFoldersStack.length > 0) {
        container.append(
`<a id="${ prevFoldersStack[prevFoldersStack.length - 1] }" onclick="openPrevFolder('${ containerId }', ${ isEnableEditModeIfActiveBranch })" class="Link Commit-list-item list-group-item list-group-item-action">
    <div class="Table">
        <div class="Table-row">
            <div class="Table-cell prev-folder">
                <span class="Back-caret-left"/> Back
            </div>
        </div>
    </div>
</a>`);
    }

    sortFiles(folder.files);
    folder.files.forEach(file => addFileToList(file, containerId, isEnableEditModeIfActiveBranch));
    recentFolderSha1 = folderSha1;
}

function addFileToList(file, containerId, isEnableEditModeIfActiveBranch) {
    let container = $(containerId);

    if(file !== 'undefined') {
        let editMode = `#edit-mode-${file.sha1}`;
        let id = `#file-${file.sha1}`;

        container.append(buildListItem(file, isActiveBranch, containerId, isEnableEditModeIfActiveBranch));

        $(editMode).hover(onEditModeMouseEnter, onEditModeMouseLeave);
        $(editMode).click(onEditModeClicked);
        $(id).on('click', function () {
            container.find('a').each(function () {
                $(this).removeClass('active');
            });
            $(this).addClass('active')
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

function buildListItem(file, isActiveBranch, containerId, isEnableEditModeIfActiveBranch) {
    let icon = file.type === 'FOLDER' ? 'folder-icon' : 'file-icon';
    let editModeIcon = isEnableEditModeIfActiveBranch ? buildEditModeIcon(file, isActiveBranch) : '';
    let editModeExpander = isEnableEditModeIfActiveBranch ? buildEditModeExpander(file, isActiveBranch) : '';

    return `<div class="Commit-list-item">
    ${ editModeIcon }
    <a ${ buildAtrributes(file, containerId, isEnableEditModeIfActiveBranch) } id="file-${ file.sha1 }" role="button" class="Link list-group-item list-group-item-action">
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
    ${ buildContentExpander(file, isActiveBranch, isEnableEditModeIfActiveBranch) }
</div>`;
}

function buildEditModeIcon(file, isActiveBranch) {
    return isActiveBranch ?
`<a href="#edit-mode-content-${ file.sha1 }" data-toggle="collapse" aria-expanded="false" aria-controls="edit-mode-content-${ file.sha1 }">
    <img id="edit-mode-${ file.sha1 }" class="edit-mode-pencil" src="../${ASSETS_LOCATION}/edit-mode-pencil.svg" alt="edit-mode"/>
</a>` : '';
}

function buildAtrributes(file, containerId, isEnableEditModeIfActiveBranch) {
    return file.type === 'FOLDER' ?
        `onclick="openFolder('${ file.sha1}', '${ containerId }', ${ isEnableEditModeIfActiveBranch })"`
        : `href="#content-${ file.sha1 }" data-toggle="collapse" aria-expanded="false" aria-controls="content-${ file.sha1 }"`;
}

function getFileExtension(file) {
    let fileExtension = '';

    if(file.type !== 'FOLDER') {
        let period = file.name.lastIndexOf('.');
        fileExtension = file.name.substring(period);
    }

    return fileExtension;
}

function buildEditModeExpander(file, isActiveBranch) {
    return isActiveBranch ?
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

function buildContentExpander(file, isActiveBranch, isEnableEditModeIfActiveBranch) {
    let contentEditor = isActiveBranch && isEnableEditModeIfActiveBranch ? `<div class="Editor"><button type="button" id="${ file.sha1 }-edit" class="Edit" onclick="editBlob('${ file.sha1 }')">Edit</button><button type="button" id="${ file.sha1 }-save" onclick="saveBlob('${ file.sha1 }')" class="Save" disabled>Save</button></div>` : '';

    return file.type === 'FOLDER' ? '' :
`<div class="collapse" id="content-${ file.sha1 }">
    <div class="blob-content card card-body">
        ${ contentEditor }
        <div class="Editable">${ repository.blobs[file.sha1].text }</div>
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

    updateFileData(sha1, newName, 'POST', newName, 'update-file-name', null, null);
    let file = getFileDataInCurrentDir(sha1, recentFolderSha1);
    file.name = newName;
}

function getFileDataInCurrentDir(sha1, currFolderSha1) {
    let files = repository.folders[currFolderSha1].files;
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
    let files = repository.folders[recentFolderSha1].files;
    let length = files.length;

    $('#root-folder-files').on('click', '.Delete', function() {
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

    let editor = $(`#content-${sha1} > .blob-content > .Editable`);
    let blob = repository.blobs[sha1];

    if(blob.text !== editor.text()) {
        isOpenChanges = true;
        $('#btn-commit').prop('disabled', !isOpenChanges);
        blob.text = editor.text();
        updateFileData(sha1, blob.name, 'POST', editor.text(), 'update-blob', onUpdateBlobSuccess, onUpdateBlobError);
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

//TO-DO
function onUpdateBlobError() {

}

function onUpdateBlobSuccess() {

}

function setSavedStyle(sha1) {
    let btnEdit = $(`#${ sha1 }-edit`);
    let blobContent = $(`#content-${sha1} > .blob-content`);

    $(`#${ sha1 }-save`).prop('disabled', true);

    if(btnEdit.hasClass('Edit-selected')) {
        btnEdit.removeClass('Edit-selected');
    }

    if(blobContent.hasClass('blob-content-edit-mode')) {
        blobContent.removeClass('blob-content-edit-mode');
        setNotEditable($(`#content-${sha1} > .blob-content > .Editable`));
    }
}

function setEditStyle(sha1) {
    let editor = $(`#content-${sha1} > .blob-content > .Editable`);
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
    $(`#content-${sha1} > .blob-content`).toggleClass('blob-content-edit-mode');
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

function openPrevFolder(containerId, isEnableEditModeIfActiveBranch) {
    updateRootFolder(prevFoldersStack.pop(), containerId, isEnableEditModeIfActiveBranch);
}

function openFolder(sha1, containerId, isEnableEditModeIfActiveBranch) {
    prevFoldersStack.push(recentFolderSha1);
    updateRootFolder(sha1, containerId, isEnableEditModeIfActiveBranch);
}

function addBlob(parentFolderSha1, fileData, file) {
    addFile(repository.blobs, parentFolderSha1, fileData, file);
}

function addFolder(parentFolderSha1, fileData, file) {
    addFile(repository.folders, parentFolderSha1, fileData, file);
}

function addFile(dataBaseToPushFile, parentFolderSha1, fileData, file) {
    dataBaseToPushFile[`${fileData.sha1}`] = file;
    repository.folders[parentFolderSha1].files.push(fileData);
    addFileToList(fileData, '#root-folder-files', true);

    isOpenChanges = true;
    $('#btn-commit').prop('disabled', !isOpenChanges);
}
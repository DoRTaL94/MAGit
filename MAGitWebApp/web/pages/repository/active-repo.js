const HEADER_ITEM = "#header-drop-downs > nav > ul > li";
const HEADER_LIST_ITEM = "#header-drop-downs > nav > ul > li > ul > li";
const SERVER_ERROR_MESSAGE = '<li>' + "Failed to send data to the server..." + '</li>';
const HOME = "../repository/active-repo.html";

let uploadForm =
    `<form id="uploadForm" action="import" enctype="multipart/form-data" method="POST">
        <input class="File-chooser" accept=".xml" type="file" name="repoXml">
        <input class="Btn-file-upload" type="submit">
    </form>`;
let btnUpload =
    `<button id="btn-upload-file" class="btn btn-primary" type="button" onclick="uploadFile()">
        <div id="loading-container">
            <div class="lds-ring"><div></div><div></div><div></div><div></div></div>
        </div>
        <span id="import-btn-text">Import repository</span>
    </button>`;
let messagesForm ='<ul id="form-messages"></ul>';
let repository = null;
let recentFolderSha1 = null;
let prevFoldersStack = [];
let isActiveBranch = null;

$(onLoad);

function onLoad() {
    setHeaderItemsOnClick();
    setListsItemsOnClick();
    updateActiveRepo();
}

function setHeaderItemsOnClick() {
    $(HEADER_ITEM).on("click", function() {
        let notification = $(this).find("img.Notification-icon");

        if(notification.length === 0) {
            handleDropdown($(this));
        } else {
            handleNotificationClicked();
        }
    });
}

function handleDropdown(clicked) {
    let isOpen = clicked.find("ul").css("display") === "block";

    $(HEADER_ITEM).each(function () {
        $(this).find("ul").css("display", "none");
        $(this).find("span.Dropdown-caret").css("border-width", "0 2px 2px 0");
    });

    if (!isOpen) {
        clicked.find("ul").toggle();
        clicked.find("ul").css("z-index", 2);
        clicked.find("span.Dropdown-caret").css("border-width", "2px 0 0 2px");
    }
}

function handleNotificationClicked() {
    showNotifications();

    $.ajax({
        method: 'GET',
        data: "",
        url: "active-repo",
        processData: false,
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 2000,
        error: function (e) {
            console.log("Failed to submit");
            $("#result").text("Failed to get result from server " + e);
        },
        success: function (notifications) {
            $.each(notifications || [], addNotification);
        }
    });
}

function showNotifications() {
    let container = $("div.Content");
    container.empty();
    container.addClass("Content-relative");

    let notificationsContainer = $("<div>").addClass("Notifications-container");
    let notifications = $("<div>").addClass("Notifications Magit-body");
    let NotificationsListContainer = $("<div>").addClass("Notifications-list-container");
    let notificationsTitle = $("<div>").addClass("Notifications-list-title");
    let notificationsList = $("<div>").addClass("Notifications-list");
    let notificationsContentContainer = $("<div>").addClass("Notifications-content-container");
    let notificationsContent = $("<div>").addClass("Notifications-content");
    let notificationsContentTitle = $("<div>").addClass("Notifications-content-title");

    notificationsTitle.text("Notifications");
    notificationsContentTitle.text("Notification Details");
    NotificationsListContainer.append(notificationsTitle);
    NotificationsListContainer.append(notificationsList);
    notificationsContentContainer.append(notificationsContentTitle);
    notificationsContentContainer.append(notificationsContent);
    notifications.append(NotificationsListContainer);
    notifications.append(notificationsContentContainer);
    notificationsContainer.append(notifications);
    container.append(notificationsContainer);
}

function setListsItemsOnClick() {
    setListItemImportXmlOnClick();
    setListItemMyRepositoriesOnClick();
}

function setListItemMyRepositoriesOnClick() {
    let myRepos = $(HEADER_LIST_ITEM).find('#my-repositories');
    myRepos.on('click', myRepositoriesOnClick)
}

function myRepositoriesOnClick() {
    let content = $('.Content');

    $('head').append('<script type="text/javascript" src="my-repositories.js"></script>');
    content.empty();

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

// TO-DO
function addNotification(index, dataJson) {

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
        repository = response;
        updateRepo(repository.headBranch.name);
        $('#download').on('click', () => window.location.href = `download?repository=${ repository.repoName }`);
    }
}

function updateRepo(branchName) {
    let branch = repository.branches[branchName];
    let commitListHeader = $('#commit-list-header');
    let currCommit = repository.commits[branch.pointedCommitSha1];
    let branchesList = $('.Branches');
    isActiveBranch = repository.headBranch.name === branchName;
    branchesList.empty();

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
            let headBranchMark = key === repository.headBranch.name ? '<img id="head-branch-mark" src="../../common/head-branch.svg"/>' : '';
            branchesList.append(`<a class="dropdown-item" id="${ key }" onclick="updateRepo(this.id)" href="#">${ headBranchMark } ${ key }</a>`)
        }
    }

    updateRootFolder(currCommit.rootFolderSha1);
}

function updateRootFolder(folderSha1) {
    let folder = repository.folders[folderSha1];
    let folderFiles = $('#root-folder-files');
    folderFiles.empty();

    if(folder.isRoot === false && prevFoldersStack.length > 0) {
        folderFiles.append(
            `<a href="#" id="${ prevFoldersStack[prevFoldersStack.length - 1] }" onclick="openPrevFolder()" class="Commit-list-item list-group-item list-group-item-action">
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

    folder.files.forEach(function (file) {
        folderFiles.append(buildListItem(file, isActiveBranch));
        let editMode = `#edit-mode-${ file.sha1 }`;
        let id = `#${ file.sha1 }`;
        $(editMode).hover(onEditModeMouseEnter, onEditModeMouseLeave);
        $(editMode).click(onEditModeClicked);
        $(id).on('click', function () {
            folderFiles.find('a').each(function () {
                $(this).removeClass('active');
            });
            $(this).addClass('active')
        })
    });

    recentFolderSha1 = folderSha1;
}

function onEditModeClicked() {
    if($(this).attr('alt')==='edit-mode') {
        $(this).attr('alt', 'edit-mode-selected');
        $(this).attr('src', '../../common/edit-mode-pencil-selected.svg');
    } else {
        $(this).attr('alt', 'edit-mode');
        $(this).attr('src', '../../common/edit-mode-pencil.svg');
    }
}

function onEditModeMouseEnter() {
    $(this).attr('src', '../../common/edit-mode-pencil-selected.svg');
}

function onEditModeMouseLeave() {
    if($(this).attr('alt') !== 'edit-mode-selected') {
        $(this).attr('src', '../../common/edit-mode-pencil.svg');
    }
}

function buildListItem(file, isActiveBranch) {
    let icon = file.type === 'FOLDER' ? 'folder-icon' : 'file-icon';
    let attributes = file.type === 'FOLDER' ? `href="#" onclick="openFolder('${ file.sha1 }')"` : `href="#content-${ file.sha1 }" data-toggle="collapse" aria-expanded="false" aria-controls="content-${ file.sha1 }"`;
    let contentEditor = isActiveBranch ? `<div class="Editor"><button type="button" id="${ file.sha1 }-edit" class="Edit" onclick="editBlob('${ file.sha1 }')">Edit</button><button type="button" id="${ file.sha1 }-save" onclick="saveBlob('${ file.sha1 }')" class="Save" disabled>Save</button></div>` : '';
    let editModeIcon = isActiveBranch ? `<a href="#edit-mode-content-${ file.sha1 }" data-toggle="collapse" aria-expanded="false" aria-controls="edit-mode-content-${ file.sha1 }"><img id="edit-mode-${ file.sha1 }" class="edit-mode-pencil" src="../../common/edit-mode-pencil.svg" alt="edit-mode"/></a>` : '';
    let editModeExpander = isActiveBranch ? `
    <div class="collapse" id="edit-mode-content-${ file.sha1 }">
        <div class="Table edit-mode-content card card-body">
            <div class="Table-row">
                <div class="Table-cell">
                    <label>
                        Edit file name <input type="text" class="New-name-input" name="newname" spellcheck="false">
                    </label>
                </div>
                <div class="Table-cell">
                    <button type="button" class="Save">Save</button>
                </div>
                <div class="Table-cell">
                    <button type="button" class="Delete">Delete file</button>
                </div>
            </div>  
        </div>
    </div> 
    
    ` : '';
    let contentExpander = file.type === 'FOLDER' ? '' : `<div class="collapse" id="content-${ file.sha1 }"><div class="blob-content card card-body">${ contentEditor }<div class="Editable" contenteditable="false">${ repository.blobs[file.sha1].text }</div></div></div>`;

    return `<div class="Commit-list-item">
    ${ editModeIcon }
    <a ${ attributes } id="${ file.sha1 }" role="button" class="Item-link list-group-item list-group-item-action">
        <div class="Table">
            <div class="Table-row">
                <div class="Table-cell file-name"><img id="${ icon }" src="../../common/${ icon }.svg"/> ${ file.name }</div>
                <div class="Table-cell file-last-changer">${ file.lastChanger }</div>
                <div class="Table-cell file-sha1">${ file.sha1 }</div>
                <div class="Table-cell file-last-update">${ file.lastUpdate }</div>  
            </div>
        </div>
    </a>
</div>
${ editModeExpander }
${ contentExpander }`;
}

function saveBlob(sha1) {
    setSavedStyle(sha1);
    let editor = $(`#content-${sha1} > .blob-content > .Editable`);

    prevFoldersStack.push(recentFolderSha1);
    prevFoldersStack.push(sha1); //adds the changed blob to the end of the stack.
    prevFoldersStack.push(editor.text()); //adds the changed blob to the end of the stack.
    let prevFolderStack = JSON.stringify(prevFoldersStack);
    prevFoldersStack.pop();
    prevFoldersStack.pop();

    let data = 'prevFolders=' + prevFolderStack;
    repository.blobs[sha1].text = editor.text();

    $.ajax({
        method: 'POST',
        data: data,
        url: 'update-blob',
        timeout: 3000,
        error: onUpdateBlobError,
        success: onUpdateBlobSuccess
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
    $(`#content-${sha1} > .blob-content > .Editable`).prop('contenteditable', false);

    if(btnEdit.hasClass('Edit-selected')) {
        btnEdit.removeClass('Edit-selected');
    }

    if(blobContent.hasClass('blob-content-edit-mode')) {
        blobContent.removeClass('blob-content-edit-mode');
    }
}

function setEditStyle(sha1) {
    let editor = $(`#content-${sha1} > .blob-content > .Editable`);
    let isEditMode = editor.attr('contenteditable') === 'false';

    if(isEditMode) {
        $(`#${sha1}-save`).prop('disabled', false);
        editor.prop('contenteditable', true);
        editor.on('keydown .editable', onTabPressed);
    } else {
        editor.prop('contenteditable', false);
    }

    $(`#${sha1}-edit`).toggleClass('Edit-selected');
    $(`#content-${sha1} > .blob-content`).toggleClass('blob-content-edit-mode')
}

function onTabPressed(e) {
    if (e.keyCode == 9 && e.shiftKey) {
        document.execCommand('styleWithCSS', true, null);
        document.execCommand('outdent', true, null);
        e.preventDefault();
    } else if (e.keyCode == 9) {
        console.log("key", e.keyCode);
        document.execCommand('styleWithCSS', true, null);
        document.execCommand('indent', true, null);
        e.preventDefault();
    }
}

function openPrevFolder() {
    updateRootFolder(prevFoldersStack.pop());
}

function openFolder(sha1) {
    prevFoldersStack.push(recentFolderSha1);
    updateRootFolder(sha1);
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
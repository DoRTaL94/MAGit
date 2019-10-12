const HEADER_ITEM = "#header-drop-downs > nav > ul > li";
const HEADER_LIST_ITEM = "#header-drop-downs > nav > ul > li > ul > li";
const SERVER_ERROR_MESSAGE = '<li>' + "Failed to send data to the server..." + '</li>';
const HOME = "/magit/pages/repository/active_repo.html";

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
        url: "active_repo",
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
}

function setListItemImportXmlOnClick() {
    let importXml = $(HEADER_LIST_ITEM).find("#import-repo-xml");
    importXml.on("click", importXmlOnClick);
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

// TO-DO
function updateActiveRepo() {
    $.ajax({
        method: 'POST',
        data: '',
        url: 'update_repo',
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
        updateRepo(repository.headBranch.name)
    }
}

function updateRepo(branchName) {
    let branch = repository.branches[branchName];
    let commitListHeader = $('#commit-list-header');
    let currCommit = repository.commits[branch.pointedCommitSha1];
    let rootFolder = repository.folders[currCommit.rootFolderSha1];
    let branchesList = $('.Branches');
    let rootFolderFiles = $('#root-folder-files');

    branchesList.empty();
    rootFolderFiles.empty();

    commitListHeader.find('div#commiter-name').empty().text(currCommit.lastChanger);
    commitListHeader.find('td#commit-description').empty().text(currCommit.message);
    commitListHeader.find('td#commit-sha1').empty().text(currCommit.sha1);
    commitListHeader.find('td#commit-date').empty().text(currCommit.lastUpdate);

    $('.Repo-name').empty().text(`${ repository.owner } / ${ repository.repoName }`);
    $('#branch-drop-down').empty().text(`Branch: ${ branch.name }`);

    for(let key in repository.branches) {
        if(repository.branches.hasOwnProperty(key)) {
            branchesList.append(`<a class="dropdown-item" id="${ key }" onclick="updateRepo(this.id)" href="#">${ key }</a>`)
        }
    }

    rootFolder.files.forEach(function (file) {
        rootFolderFiles.append(buildListItem(file));
        id = `#${ file.sha1 }`;
        $(id).on('click', function () {
            rootFolderFiles.find('a').each(function () {
                $(this).removeClass('active');
            });
            $(this).addClass('active')
        })
    })
}

function buildListItem(file) {
    let icon = file.type === 'FOLDER' ? 'folder-icon' : 'file-icon';
    return `<a href="#" id="${ file.sha1 }" class="Commit-list-item list-group-item list-group-item-action">
    <div id="table" class="table">
        <div class="table-row">
            <div id="file-name" class="table-cell"><img id="${ icon }" src="../../common/${ icon }.svg"/> ${ file.name }</div>
            <div id="file-last-changer" class="table-cell">${ file.lastChanger }</div>
            <div id="file-sha1" class="table-cell">${ file.sha1 }</div>
            <div id="file-last-update" class="table-cell">${ file.lastUpdate }</div>
        </div>
    </div>
</a>`;
}
export { showErrorMessage, showDeclinePrPopup, showSuccessPopup, showPushPopup, showPullPopup, showCommitPopup, showWarningPopup, showOpenChangesPopup, showCreateFilePopup, showCreateNewBranchPopup };
import { setOpenChanges, addBlob, addFolder, repository } from './active-repo.js';
import { getParentsFoldersNames } from './utils.js';

function showPopup(headerText, content, buttonOk) {
    let background = $('<div>').addClass('Popup-background');
    let popup = $('<div>').addClass('Popup');
    let header = $('<div>').addClass('Popup-header');
    let contentContainer = $('<div>').addClass('Popup-content');
    let buttons = $('<div>').addClass('Popup-buttons');
    let cancel = $('<button>').attr('type', 'button');

    header.text(headerText);
    cancel.text('Cancel');
    cancel.on('click', onCancelClick);
    cancel.addClass('Btn-cancel');
    buttonOk.addClass('Btn-ok');

    popup.append(header);
    contentContainer.append(content);
    popup.append(contentContainer);
    buttons.append(buttonOk);
    buttons.append(cancel);
    popup.append(buttons);
    background.append(popup);

    $('.Container').append(background);
}

function showOpenChangesPopup(onProceed) {
    let content = $('<div>').addClass('Popup-warning-text');
    content.text('Do you wish to proceed?');
    let btnOk = $('<button>')
        .attr('type', 'button')
        .text('Yes')
        .on('click', onProceed);

    showPopup('There are open changes in working directory', content, btnOk)
}

function showCommitPopup() {
    let textarea = $('<textarea>').addClass('Popup-textarea');
    let commit = $('<button>')
        .attr('type', 'button')
        .text('Commit')
        .on('click', onCommitClick);

    showPopup('Please enter commit description', textarea, commit);
}

function onCommitClick() {
    let description = $('.Popup-textarea').val();

    $.ajax({
        method:'POST',
        data: 'description=' + description,
        url: 'commit',
        timeout: 4000,
        error: onCommitError,
        success: onCommitSuccess
    });
}

function onCancelClick() {
    $('.Popup-background').remove();
}

// TO-DO
function onCommitError(e) {

}

// TO-DO
function onCommitSuccess(response) {
    onCancelClick();

    if(response === 'empty') {
        showErrorMessage('Something went wrong...','Working directory cannot be empty', true)
    } else {
        setOpenChanges(false);
        window.location.href = "active-repo.html";
    }
}

function showWarningPopup(message, onOkClicked) {
    let popupMessage = $('<div>').addClass('Popup-message');
    popupMessage.text(message);
    let buttonOk = $('<button>').attr('type', 'button');
    buttonOk.text('Yes');
    buttonOk.on('click', onOkClicked);

    showPopup('Warning', popupMessage, buttonOk);
}

function showCreateNewBranchPopup() {
    let createBranchForm = `<div class="Branch-name"><input type="text" placeholder="Branch name" id="input-branch-name" name="branchName" spellcheck="false"></div>`;
    let btnCreate = $('<button>')
        .attr('type', 'button')
        .text('Create')
        .on('click', onBranchClick);

    showPopup('Create branch', createBranchForm, btnCreate);
}

function onBranchClick() {
    if(repository !== null) {
        let branchName = $('#input-branch-name').val();
        onCancelClick();

        $.ajax({
            method: 'POST',
            data: 'branchname=' + branchName,
            url: 'create-new-branch',
            timeout: 4000,
            success: onCreateNewBranchSuccess
        });
    }
}

function onCreateNewBranchSuccess(response) {
    let newBranch = {
        name: response[0],
        pointedCommitSha1: response[1],
        isHead: true,
        isRemote: false,
        isTracking: false,
        isMerged: false
    };

    repository.branches[newBranch.name] = newBranch;
    showSuccessPopup('Create New Branch', 'Branch was created successfully. Click on the commits counter above to view all the branches pointing the current commit.', true);
}

function showCreateFilePopup() {
    let createFileForm =
    `<div class="input-group">
        <div class="File-name input-group-prepend"><input type="text" placeholder="File name" id="input-file-name" name="filename" spellcheck="false"></div>
        <select class="custom-select" id="extension-select">
            <option value="1" selected>Directory</option>
            <option value="2">.txt</option>
            <option value="3">.java</option>
            <option value="4">.js</option>
            <option value="5">.css</option>
            <option value="6">.html</option>
            <option value="7">.c</option>
            <option value="8">.cpp</option>
            <option value="9">.h</option>
            <option value="10">.json</option>
            <option value="11">.xsd</option>
            <option value="12">.class</option>
            <option value="13">.jsp</option>
            <option value="14">.py</option>
            <option value="15">.sql</option>
            <option value="16">.aspx</option>
        </select>
    </div>
</div>`;
    let btnCreate = $('<button>')
        .attr('type', 'button')
        .text('Create')
        .on('click', onCreateFileClick);

    showPopup('Create file', createFileForm, btnCreate);
}

function onCreateFileClick() {
    if(repository !== null) {
        let filename = $('#input-file-name').val();
        let extension = $('#extension-select').find(':selected').text();
        let data = 'data=' + JSON.stringify(getParentsFoldersNames()) + '&filename=' + filename + '&extension=' + extension;
        onCancelClick();

        let emptyRepoCheck = $('#wc-files-list').find('#empty-repository');
        if(emptyRepoCheck.length === 1) {
            emptyRepoCheck.remove();
        }

        $.ajax({
            method: 'POST',
            data: data,
            url: 'create-file',
            timeout: 4000,
            error: function (response) {
                console.log(response);
            },
            success: onCreateFileSuccess
        });
    }
}

function onCreateFileSuccess(response) {
    if(typeof response === 'object') {
        let parentFolderSha1 = response.parentFolderSha1;
        let fileData = response.fileData;

        fileData.type === 'FOLDER' ? addFolder(parentFolderSha1, fileData, response.folder) : addBlob(parentFolderSha1, fileData, response.blob);
    } else {
        showErrorMessage('File creation error', response, false);
    }
}

function showErrorMessage(headerText, message, isRefresh) {
    let background = $('<div>').addClass('Popup-background');
    let content = $('<div>').addClass('Popup-error-content').text(message);
    let popup = $('<div>').addClass('Popup');
    let header = $('<div>').addClass('Popup-header');
    let contentContainer = $('<div>').addClass('Popup-content');
    let buttons = $('<div>').addClass('Popup-buttons');
    let btnOk = $('<button>').attr('type', 'button');

    header.text(headerText);
    btnOk.text('OK');
    btnOk.on('click', function() {
        onCancelClick();

        if(isRefresh) {
            window.location.href = "active-repo.html";
        }
    });
    btnOk.addClass('Btn-error-ok');

    popup.append(header);
    contentContainer.append(content);
    popup.append(contentContainer);
    buttons.append(btnOk);
    popup.append(buttons);
    background.append(popup);

    $('.Container').append(background);
}

function showPushPopup() {
    let textarea =
`<div class="input-group mb-3">
    <div class="input-group-prepend">
        <label class="input-group-text" for="target-branch">Target Branch</label>
    </div>
    <select class="custom-select" id="target-branch">
        ${ buildOptions(branch => !branch.isRemote) }
    </select>
</div>
<div class="input-group mb-3">
    <div class="input-group-prepend">
        <label class="input-group-text" for="base-branch">Base Branch</label>
    </div>
    <select class="custom-select" id="base-branch">
        ${ buildOptions(branch => branch.isRemote) }
    </select>
</div>
<textarea placeholder="Write your description here" class="Popup-textarea"></textarea>`;

    let request = $('<button>')
        .attr('type', 'button')
        .text('Push')
        .on('click', onPushClick);

    showPopup('Make Pull Request', textarea, request);
}

function buildOptions(test) {
    let branches = repository.branches;
    let res = '';
    let counter = 1;
    for(let name in branches) {
        let branch = branches[name];

        if(test(branch)) {
            res += `<option value="${ counter }" ${ counter === 1 ? 'selected' : '' }>${ branch.name }</option>`;
            counter++;
        }
    }

    return res;
}

function onPushClick() {
    let target = $('#target-branch').find(':selected').text();
    let base = $('#base-branch').find(':selected').text();
    let message = $('.Popup-textarea').val();

    onCancelClick();

    $.ajax({
        method:'POST',
        data: 'user=' + repository.usernameForkedFrom + '&target=' + target + '&base=' + base + '&message=' + message,
        url: 'pull-request',
        timeout: 4000,
        error: function (response) {
            console.log(response);
        },
        success: onPullRequestSuccess
    });
}

function onPullRequestSuccess() {
    showSuccessPopup('Pull Request', 'Request was sent successfully', false)
}

function showSuccessPopup(title, message, isRefresh) {
    showErrorMessage(title, message, isRefresh);
}

function showDeclinePrPopup(pullRequest) {
    let textarea = `<textarea placeholder="Write your reason of decline here" class="Popup-textarea"></textarea>`;

    let request = $('<button>')
        .attr('type', 'button')
        .text('Decline')
        .on('click', function () {
            let reason = $('.Popup-textarea').val();

            $.ajax({
                method: 'POST',
                data: 'id=' + pullRequest.id + '&reason=' + reason,
                url: 'decline-pr',
                timeout: 3000,
                error: function(response) {
                    console.log(response);
                },
                success: function () {
                    window.location.href = 'active-repo.html';
                }
            });
        });

    showPopup('Decline Pull Request', textarea, request);
}

function showPullPopup(message) {
    showSuccessPopup('Pull Message', message, true)
}
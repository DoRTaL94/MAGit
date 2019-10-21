export { showCommitPopup, showWarningPopup, showOpenChangesPopup };
import { setOpenChanges } from './active-repo.js';

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
function onCommitSuccess(e) {
    setOpenChanges(false);
    window.location.href = "active-repo.html";
}

function showWarningPopup(message, onOkClicked) {
    let popupMessage = $('<div>').addClass('Popup-message');
    popupMessage.text(message);
    let buttonOk = $('<button>').attr('type', 'button');
    buttonOk.text('Yes');
    buttonOk.on('click', onOkClicked);

    showPopup('Warning', popupMessage, buttonOk);
}
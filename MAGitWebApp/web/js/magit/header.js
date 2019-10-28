import { getIsOpenChanges, HOME, ASSETS_LOCATION } from './active-repo.js';
import { showOpenChangesPopup } from './popups.js';

window.openUserRepos = openUserRepos;

const HEADER_ITEM = "#header-drop-downs > nav > ul > li";

$(setHeaderItemsOnClick);

function setHeaderItemsOnClick() {
    $('#home-link').on('click', onHomeClick);
    $('#explore-link').on('click', onExploreClick);
    $('#my-repositories').on('click', onHomeClick);
    $('#logout').on('click', onLogoutClick);

    $(HEADER_ITEM).on("click", function() {
        let notification = $(this).find("img.Notification-icon");

        if(notification.length === 0) {
            handleDropdown($(this));
        } else {
            handleNotificationClicked();
        }
    });
}

function onHomeClick() {
    $.ajax({
        method: 'POST',
        data: 'user=""&current=true',
        url: "set-user-repo",
        timeout: 2000,
        error: function(response) {
            console.log(response);
        },
        success: function () {
            window.location.href = 'profile.html';
        }
    });
}

function onExploreClick() {
    let content = $('.Content');
    content.empty();

    let usersContainer = $('<div>').addClass('users-container Magit-body');
    let title = $('<div>').addClass('users-title').text('Magit Users');
    let users = $('<div>').addClass('users');
    let empty = $('<div>').addClass('empty-users').text('You are the only user in MAGit :( ...');

    users.append(empty);
    usersContainer.append(title);
    usersContainer.append(users);
    content.append(usersContainer);

    $.ajax({
        method: 'POST',
        url: "update-users",
        timeout: 3000,
        error: function (response) {
            console.log(response);
        },
        success: updateUsers
    });
}

function updateUsers(response) {
    let container = $('.users');
    let count = response.length;

    if(count > 0) {
        container.empty();

        for (let user = 0; user < count; user++) {
            container.append(addUser(response[user]));
        }
    }
}

function addUser(user) {
    return `<div class="users-list">
    <a id="user-${ user }" onclick="openUserRepos('${ user }')" role="button" class="Link list-group-item list-group-item-action">
        <div class="Table">
            <div class="Table-row">
                <div class="Table-cell user-icon"><img  class="icon" src="../${ASSETS_LOCATION}/user-icon.png"/></div>
                <div class="Table-cell username">${ user }</div>
            </div>
        </div>
    </a>
</div>`
}

function openUserRepos(user) {
    $.ajax({
        method: 'POST',
        data: 'user=' + user + '&current=false',
        url: "set-user-repo",
        timeout: 2000,
        error: function(response) {
            console.log(response);
        },
        success: function () {
            window.location.href = 'profile.html';
        }
    });
}

function onLogoutClick() {
    if(getIsOpenChanges()) {
        showOpenChangesPopup(logout)
    } else {
        logout();
    }
}

function logout() {
    $.ajax({
        method: 'POST',
        url: "logout",
        timeout: 2000,
        success: function () {
            window.location.href = 'login.html';
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

// TO-DO
function addNotification(index, dataJson) {

}

function showNotifications() {
    let container = $("div.Content");
    container.empty();
    container.addClass("Content-absolute");

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
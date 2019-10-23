import { getIsOpenChanges, HOME } from './active-repo.js';

const HEADER_ITEM = "#header-drop-downs > nav > ul > li";

$(setHeaderItemsOnClick);

function setHeaderItemsOnClick() {
    window.addEventListener("beforeunload", function(event) {
        if(getIsOpenChanges()) {
            event.returnValue = "Changes you made would not be saved.";

            $.ajax({
                method: 'POST',
                url: "clean-open-changes",
                timeout: 2000
            });
        }
    });

    $('#home-link').on('click', () => window.location.href = HOME);

    $('#logout').on('click', function () {
        $.ajax({
            method: 'POST',
            url: "logout",
            timeout: 2000,
            success: function () {
                window.location.href = 'login.html';
            }
        });
    });

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
import { getIsOpenChanges, HOME, ASSETS_LOCATION } from './active-repo.js';
import { showOpenChangesPopup } from './popups.js';
export { onNotificationsClick };

window.openUserRepos = openUserRepos;

const HEADER_ITEM = "#header-drop-downs > nav > ul > li";
let isNotificationsClicked = false;
let notReadByUser = 0;

$(setHeaderItemsOnClick);

function setHeaderItemsOnClick() {
    $('#home-link').on('click', onHomeClick);
    $('#explore-link').on('click', onExploreClick);
    $('#my-repositories').on('click', onHomeClick);
    $('#logout').on('click', onLogoutClick);

    $(HEADER_ITEM).on("click", function() {
        if($(this).find('ul > li > div#notifications-container').length > 0) {
            onNotificationsClick();
        }

        handleDropdown($(this));
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
        clicked.find("ul").css("z-index", 4);
        clicked.find("span.Dropdown-caret").css("border-width", "2px 0 0 2px");
    }
}

function onNotificationsClick() {
    $.ajax({
        method: 'POST',
        url: 'notifications',
        timeout: 2000,
        error: function(response) {
            console.log(response);
        },
        success: onNotificationsSuccess
    });
}

function onNotificationsSuccess(response) {
    if($.isArray(response)) {
        let container = $('#notifications-container');
        container.empty();
        notReadByUser = 0;
        response.forEach(notification => addNotification(notification));

        if(!isNotificationsClicked) {
            if(notReadByUser > 0) {
                $('#notifications-counter-container').append(`<div id="notifications-counter">${ notReadByUser }</div>`);
            }

            isNotificationsClicked = true;
        } else {
            $('#notifications-counter-container').empty();
            onNotificationsRead();
        }
    }
}

function onNotificationsRead() {
    $.ajax({
        method: 'POST',
        url: 'notifications-read',
        timeout: 2000
    });
}

//שם ה repository, שם המשתמש, הודעת ה PR, שם branch המטרה ושם branch הבסיס.
function addNotification(notification) {
    if(!notification.isNotShow) {
        let notificationContent;

        switch (notification.type) {
            case 'fork':
                notificationContent =
                    `<div class="notification">
    <span class="time-stamp">${notification.timeStamp}</span>
    <span class="notification-text">${notification.forkedRepoName} was forked by ${notification.forkingUser}</span>
</div>`;
                break;
            case 'pullRequest':
                notificationContent =
                    `<div class="notification">
    <span class="time-stamp">${notification.pullRequest.timeStamp}</span>
    <span class="notification-text">Pull request was sent to you.</br>
    Relevant repository: ${notification.pullRequest.relevantRepoName}</br>
    Requesting user: ${notification.pullRequest.requestByUserName}</br>
    Message: ${notification.pullRequest.pullRequestMessage}</br>
    Target branch name: ${notification.pullRequest.target.name}</br>
    Base branch name: ${notification.pullRequest.base.name}
    </span>
</div>`;
                break;
            case 'prAnswer':
                let answerText = notification.pullRequest.isApproved ? 'A pull request you sent has been approved.' : 'A pull request you sent has been declined.';

                notificationContent =
                    `<div class="notification">
    <span class="time-stamp">${notification.pullRequest.timeStamp}</span>
    <span class="notification-text">${answerText}</br>
    Relevant repository: ${notification.pullRequest.relevantRepoName}</br>
    Requesting user: ${notification.pullRequest.requestByUserName}</br>
    Message: ${notification.pullRequest.pullRequestMessage}</br>
    Target branch name: ${notification.pullRequest.target.name}</br>
    Base branch name: ${notification.pullRequest.base.name}
    </span>
</div>`;
                break;
        }

        $('#notifications-container').append(notificationContent);

        if(!notification.isReadByUser) {
            notReadByUser++;
        }
    }
}
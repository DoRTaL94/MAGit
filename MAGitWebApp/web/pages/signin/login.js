const HOME = '../repository/active_repo.html';
const REGISTER_LINK_TXT = "Don't have an account yet? Register";

$(setupLogin);

function setupLogin() {
    let container = $('.Container');

    container.empty();
    showForm(true);
    $('#signup').on('click', setupSignUp);

    $('.Login-or-signup-form').submit(function () {
        $.ajax({
            method: 'GET',
            data: $(this).serialize(),
            url: this.action,
            processData: false, // Don't process the files
            contentType: false, // Set content type to false as jQuery will tell the server its a query string request
            timeout: 4000,
            error: onErrorAjaxResponse,
            success: onLoginSuccessAjaxResponse
        });

        return false;
    })
}

function showForm(isLogin) {
    let container = $('.Container');
    let form = createLoginForm(isLogin);
    let welcome = createWelcome(isLogin);
    let inputs = createInputs(isLogin);

    form += welcome;
    form += inputs;

    if(isLogin) {
        form += `<div id="signup"><a href="#">${REGISTER_LINK_TXT}</a></div>
`;
    }

    form += '</form>';
    container.append(form);
    container.find('input.Username').focus();
}

function createLoginForm(isLogin) {
    return `<form class="Login-or-signup-form" action="${isLogin ? 'login' : 'signup'}" method="POST">
`;
}

function createWelcome(isLogin) {
    let welcomeDiv;

    if(isLogin) {
        welcomeDiv = `<div class="Welcome"><h1>Welcome to the MAGit</h1></div>
`;
    } else {
        welcomeDiv = `<div class="Welcome"><h1>Sign Up</h1><div class="Back">Login <span class="Back-caret"></span></div></div>`
    }

    return welcomeDiv;
}

function createInputs(isLogin) {
    let formIo = `<ul id="form-messages"></ul>
            <label>
                Username
                <input type="text" class="Username" name="username" spellcheck="false">
            </label>
            <label>
                Password
                <input type="password" class="Password" name="password">
            </label>
`;

    if(!isLogin) {
        formIo += `<label>
                Re-enter password
                <input type="password" class="Password" name="password2">
            </label>
`;
    }

    formIo += `<input type="submit" id="btn-login-or-signup" value="${isLogin ? 'Login' : 'Sign Up'}"><br/><br/>`;

    return formIo;
}

function onErrorAjaxResponse(response) {
    $("#result").text("Failed to get result from server " + response);
}

function onLoginSuccessAjaxResponse(response) {
    if(response === "success") {
        window.location.href = HOME;
    } else {
        updateMessages(response);
    }
}

function updateMessages(response) {
    let messages = $('#form-messages');
    messages.empty();

    if (response.length > 0) {
        messages.css('display', 'block');

        for (let i = 0; i < response.length; i++) {
            messages.append('<li>' + response[i] + '</li>');
        }
    } else {
        messages.css('display', 'none');
    }
}

function setupSignUp() {
    let container = $('.Container');
    container.empty();
    showForm(false);
    container.find('.Back').on('click', setupLogin);

    $('.Login-or-signup-form').submit(function () {
        $.ajax({
            method: 'GET',
            data: $(this).serialize(),
            url: this.action,
            processData: false, // Don't process the files
            contentType: false, // Set content type to false as jQuery will tell the server its a query string request
            timeout: 4000,
            error: onErrorAjaxResponse,
            success: onSignUpSuccessAjaxResponse
        });

        return false;
    })
}

function onSignUpSuccessAjaxResponse(response) {
    if(response === "success") {
        setupLogin();
    } else {
        updateMessages(response);
    }
}
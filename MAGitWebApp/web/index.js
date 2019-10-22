$(function (e) {
    $.ajax({
        method: 'GET',
        url: 'pages/login',
        timeout: 4000,
        success: function (response) {
            if(response === "success") {
                window.location.href = 'pages/active-repo.html';
            } else {
                window.location.href = 'pages/login.html';
            }
        }
    });
});

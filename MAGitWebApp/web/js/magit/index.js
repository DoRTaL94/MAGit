$(function (e) {
    $.ajax({
        method: 'GET',
        url: 'login',
        timeout: 4000,
        success: function (response) {
            if(response === "success") {
                window.location.href = '/pages/profile.html';
            } else {
                window.location.href = '/login.html';
            }
        }
    });
});

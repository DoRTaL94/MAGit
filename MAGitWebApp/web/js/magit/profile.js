const noAvailableRepository = '<div class="Empty-repo">No available repositories.</br> To import repository please click on the <span id="import-sign">+</span> above and choose xml file.</div>';
export { initRepositoriesList };

$(onLoad);

function onLoad() {
    initRepositoriesList();
}

function initRepositoriesList() {
    $.ajax({
        method: 'GET',
        data: 'user=""&current=true',
        url: 'repositories',
        timeout: 2000,
        error: function(response) {
            console.log(response);
        },
        success: onUpdateRepositoriesSuccess
    });
}

function onUpdateRepositoriesSuccess(response) {
    let repositoriesDetails = response;
    let repositoriesList = $('#repositories-list-items');
    repositoriesList.empty();

    if($.isArray(response)) {
        if(response.length > 0) {
            initRepositoriesAppearance(response);
        }

        repositoriesDetails.forEach(addRepoDetails);
    } else {
        $('#account-name').text('Account name: ' + response.owner);
        repositoriesList.append(noAvailableRepository);
    }
}

function addRepoDetails(repoDetails) {
    let repositoriesList = $('#repositories-list-items');
    let id = `#${repoDetails.name.replace(/\s+/g, '-')}`;
    repositoriesList.append(buildListItem(repoDetails));

    $(id).on('click', function () {
        repositoriesList.find('a').each(function () {
            $(this).removeClass('active');
        });
        $(this).addClass('active');
        openRepository(repoDetails.name);
    });
}

function initRepositoriesAppearance(repositories) {
    $('#account-name').text('Account name: ' + repositories[0].owner);
    $('.Repositories-list').prepend(
        `<a id="repos-list-columns-headers" class="list-group-item disabled">
    <div class="Table">
        <div class="Table-row">
            <div class="Table-cell Repository-name">Repository name</div>
            <div class="Table-cell Active-branch-name">Active Branch</div>
            <div class="Table-cell Branches-count">Branches Count</div>
            <div class="Table-cell commit-last-update">Last Commit Date</div>
            <div class="Table-cell commit-message">Commit Message</div>
        </div>
    </div>
</a>`);
}

function openRepository(name) {
    $.ajax({
        method: 'POST',
        url: 'open-repository',
        data: 'repositoryname=' + name,
        timeout: 2000,
        success: function (response) {
            if(response === "success") {
                window.location.href = 'active-repo.html';
            }
        }
    });
}

function buildListItem(repoDetails) {
    return `<a id="${ repoDetails.name.replace(/\s+/g, '-') }" role="button" class="list-group-item list-group-item-action">
    <div class="Table">
        <div class="Table-row">
            <div class="Table-cell Repository-name">${ repoDetails.name + (repoDetails.isForked === true ? ' (forked from user: ' + repoDetails.usernameForkedFrom +')' : '') }</div>
            <div class="Table-cell Active-branch-name">${ repoDetails.activeBranchName }</div>
            <div class="Table-cell Branches-count">${ repoDetails.branchesCount }</div>
            <div class="Table-cell commit-last-update">${ repoDetails.commitLastUpdate }</div>
            <div class="Table-cell commit-message">${ repoDetails.commitMessage }</div>  
        </div>
    </div>
</a>`;
}
import { HOME } from './active-repo.js';

$(onLoad);

function onLoad() {
    updateRepositoriesList();
}

function updateRepositoriesList() {
    $.ajax({
        method: 'GET',
        url: 'repositories',
        timeout: 2000,
        error: function(response) {
            console.log(response);
        },
        success: onUpdateRepositoriesSuccess
    });
}

function onUpdateRepositoriesSuccess(response) {
    let repositoriesList = $('#repositories-list-items');
    repositoriesList.empty();
    let repositoriesDetails = response;

    repositoriesDetails.forEach(function (repoDetails) {
        let id = `#${ repoDetails.name.replace(/\s+/g, '-') }`;
        repositoriesList.append(buildListItem(repoDetails));

        $(id).on('click', function () {
            repositoriesList.find('a').each(function () {
                $(this).removeClass('active');
            });
            $(this).addClass('active');
            openRepository(repoDetails.name);
        });
    });
}

function openRepository(name) {
    $.ajax({
        method: 'POST',
        url: 'open-repository',
        data: 'repositoryname=' + name,
        timeout: 2000,
        success: function (response) {
            if(response === "success") {
                window.location.href = HOME;
            }
        }
    });
}

function buildListItem(repoDetails) {
    return `<a id="${ repoDetails.name.replace(/\s+/g, '-') }" role="button" class="list-group-item list-group-item-action">
    <div class="Table">
        <div class="Table-row">
            <div class="Table-cell Repository-name">${ repoDetails.name }</div>
            <div class="Table-cell Active-branch-name">${ repoDetails.activeBranchName }</div>
            <div class="Table-cell Branches-count">${ repoDetails.branchesCount }</div>
            <div class="Table-cell commit-last-update">${ repoDetails.commitLastUpdate }</div>
            <div class="Table-cell commit-message">${ repoDetails.commitMessage }</div>  
        </div>
    </div>
</a>`;
}
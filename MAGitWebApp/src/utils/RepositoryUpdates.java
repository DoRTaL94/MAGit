package utils;

import data.structures.Repository;
import magit.Engine;

public class RepositoryUpdates {
    private Repository repository = null;
    private boolean isOpenChanges = false;

    public void checkForUpdates() {
        repository = Engine.Creator.getInstance().getActiveRepository();

        if(repository != null) {
            isOpenChanges = !Engine.Creator.getInstance().isWcClean();
        }
    }

    public boolean getIsOpenChanges() {
        return isOpenChanges;
    }

    public Repository getRepository() {
        return repository;
    }
}

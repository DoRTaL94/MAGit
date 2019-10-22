package utils;

import data.structures.Repository;
import magit.Engine;

public class RepositoryUpdates {
    private Repository repository;
    private boolean isOpenChanges;

    public RepositoryUpdates(Engine i_Engine) {
        repository = i_Engine.getActiveRepository();
        isOpenChanges = !i_Engine.isWcClean();
    }

    public boolean getIsOpenChanges() {
        return isOpenChanges;
    }

    public Repository getRepository() {
        return repository;
    }
}

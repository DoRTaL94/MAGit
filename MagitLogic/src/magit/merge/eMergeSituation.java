package magit.merge;

import data.structures.Folder;
import data.structures.IRepositoryFile;
import magit.Engine;

public enum eMergeSituation {
    NEW_FILE_IN_THEIRS {
        public void Solve(Engine i_Engine, String i_FullPath, IRepositoryFile i_Solution) {
            i_Engine.CreateNewFileOnSystem(i_Solution, i_FullPath);
        }
    },

    // CONFLICT
    OURS_DELETED_THEIRS_CHANGED {
        public void Solve(Engine i_Engine, String i_FullPath, IRepositoryFile i_Solution) {
            if(i_Solution != null) {
                i_Engine.CreateNewFileOnSystem(i_Solution, i_FullPath);
            }
        }
    },

    //OURS_DELETED_THEIRS_SAME {
    //    void Solve(String i_FullPath, IRepositoryFile i_Solution) {}
    //},

    //NEW_FILE_IN_OURS {
    //    void Solve(String i_FullPath, IRepositoryFile i_Solution) {}
    //},

    // CONFLICT
    SAME_NAME_EQU_SHA1 {
        public void Solve(Engine i_Engine, String i_FullPath, IRepositoryFile i_Solution) {
            i_Engine.DeleteFile(i_Solution, i_FullPath, true);
        }
    },

    // CONFLICT
    SAME_NAME_DIFF_SHA1 {
        public void Solve(Engine i_Engine, String i_FullPath, IRepositoryFile i_Solution) {
            i_Engine.DeleteFile(i_Solution, i_FullPath, true);
        }
    },

    // CONFLICT
    OURS_CHANGED_THEIRS_DELETED {
        public void Solve(Engine i_Engine, String i_FullPath, IRepositoryFile i_Solution) {
            if(i_Solution == null) {
                i_Engine.DeleteFile(i_Solution, i_FullPath, false);
            }
        }
    },

    OURS_SAME_THEIRS_DELETED {
        public void Solve(Engine i_Engine, String i_FullPath, IRepositoryFile i_Solution) {
            i_Engine.DeleteFile(i_Solution, i_FullPath, false);
        }
    },

    // CONFLICT
    CHANGED_TO_DIFF_IN_BOTH {
        public void Solve(Engine i_Engine, String i_FullPath, IRepositoryFile i_Solution) {
            i_Engine.DeleteFile(i_Solution, i_FullPath, true);
        }
    },

    //OURS_CHANGED_THEIRS_SAME {
    //    void Solve(String i_FullPath, IRepositoryFile i_Solution) {}
    //},

    // CONFLICT
    CHANGED_TO_SAME_IN_BOTH {
        public void Solve(Engine i_Engine, String i_FullPath, IRepositoryFile i_Solution) {
            if(!(i_Solution instanceof Folder)) {
                i_Engine.DeleteFile(i_Solution, i_FullPath, true);
            }
        }
    },

    //SAME_FILE_IN_ALL {
    //    void Solve(String i_FullPath, IRepositoryFile i_Solution) {}
    //},

    OURS_SAME_THEIR_CHANGED {
        public void Solve(Engine i_Engine, String i_FullPath, IRepositoryFile i_Solution) {
            i_Engine.DeleteFile(i_Solution, i_FullPath, true);
        }
    },

    KEEP_STATE {
        public void Solve(Engine i_Engine, String i_FullPath, IRepositoryFile i_Solution) {}
    };

    public abstract void Solve(Engine i_Engine, String i_FullPath, IRepositoryFile i_Solution);
}

package magit;

import MagitExceptions.*;
import data.structures.Branch;
import data.structures.Repository;
import javafx.beans.property.StringProperty;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface IEngine {
    void loadRepositoryFromXml(String i_XmlPath, StringProperty i_ProgressProperty) throws FileNotFoundException, RepositoryAlreadyExistsException, xmlErrorsException, FolderInLocationAlreadyExistsException;
    void loadDataFromRepository(String i_RepositoryFullPath) throws IOException;
    void changeActiveRepository(String i_RepositoryFullPath) throws NotRepositoryFolderException, IOException;
    List<String> showCurrentCommitFiles();
    List<List<String>> getWorkingCopyDelta();
    boolean commit(String i_Description, Branch i_SecondPrecedingIfMerge) throws IOException, EmptyWcException, CommitAlreadyExistsException;
    List<String> showAllBranches();
    void createNewBranch(String i_BranchName) throws PointedCommitEmptyException;
    void deleteBranch(String i_BranchName) throws IOException;
    void checkout(String i_BranchName, boolean i_IsSkipWcCheck) throws Exception;
    List<String> showActiveBranchHistory();
    String getCurrentUserName();
    void setCurrentUserName(String i_CurrentUserName);
    String getRepositoryPath();
    void setActiveRepository(Repository i_Repository);
    Repository getActiveRepository();
    void createRepositoryAndFiles(String i_RepositoryName, String i_RepositoryLocation) throws RepositoryAlreadyExistsException, FolderInLocationAlreadyExistsException;
    void setRepositoryPath(String i_RepositoryPath);
    void resetHeadBranch(String i_PointedCommitSha1) throws IOException, Sha1LengthException;
    void exportRepositoryToXml(String i_XmlPath) throws xmlErrorsException, RepositoryNotLoadedException;
    boolean isBranchNameExists(String branchName);
    String getRemoteRepositoryLocation();
    String replaceRootPath(String i_OriginalPath, String i_RootPath, int i_FromIndex);
}

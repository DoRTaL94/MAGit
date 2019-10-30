package magit;

import IO.FileUtilities;
import MagitExceptions.*;
import data.structures.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import magit.merge.Conflict;
import magit.merge.ConflictsManager;
import magit.merge.IMergeTask;
import magit.merge.eMergeSituation;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import data.structures.eFileType;
import puk.team.course.magit.ancestor.finder.AncestorFinder;
import resources.jaxb.schema.generated.*;
import string.StringUtilities;

import javax.xml.bind.JAXBException;

public class Engine {
    public static final String DATE_FORMAT  = "dd.MM.yyyy-HH:mm:ss:SSS";
    public final StringProperty currentNameProperty;
    public final BooleanProperty loadedProperty;

    private final Factory factory;
    private Map<String, Repository> repositories = null;
    private String activeRepositoryName = null;
    private String activeRepositoryPath = null;

    // Engine is a singleton class.
    public Engine() {
        currentNameProperty = new SimpleStringProperty("Administrator");
        loadedProperty = new SimpleBooleanProperty(false);
        factory = new Factory(this);
    }

    public Map<String, Repository> getRepositories() {
        return repositories;
    }

    public String getActiveRepositoryName() {
        return activeRepositoryName;
    }

    public boolean checkIfOwnRepo(String i_RepoName) {
        boolean isOwnRepo = false;

        String repoPath = getRepository(i_RepoName).getLocationPath();
        String detailsPath = Paths.get(repoPath, ".magit", "details.txt").toString();

        try {
            String details = FileUtilities.ReadTextFromFile(detailsPath);
            List<String> lines = StringUtilities.getLines(details);
            isOwnRepo = lines.size() < 2;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return isOwnRepo;
    }

    public void putBranchFromOtherRepo(Branch i_Branch, String i_RepositoryName, Engine i_OtherEngine) {
        Repository myRepo = getRepository(i_RepositoryName);
        Repository otherRepo = i_OtherEngine.getRepository(i_RepositoryName);
        String pointedCommitSha1 = i_Branch.getPointedCommitSha1();
        Commit otherCommit = otherRepo.getCommits().get(pointedCommitSha1);
        Commit myCommit = new Commit();
        String rootSha1 = otherRepo.getCommits().get(pointedCommitSha1).getRootFolderSha1();
        Folder root = otherRepo.getFolders().get(rootSha1);
        List<Folder.Data> files = root.getFiles();

        myCommit.setRootFolderSha1(rootSha1);
        myCommit.setLastChanger(otherCommit.getLastChanger());
        myCommit.setLastUpdate(otherCommit.getLastUpdate());
        myCommit.setMessage(otherCommit.getMessage());
        myCommit.setFirstPrecedingCommitSha1(findAncestorInRemote(pointedCommitSha1, pointedCommitSha1, myRepo, otherRepo));
        myCommit.setPullRequested(true);
        myCommit.setSha1(otherCommit.getSha1());

        myRepo.getBranches().put(i_Branch.getName(), i_Branch);
        myRepo.getCommits().put(pointedCommitSha1, myCommit);
        myRepo.getFolders().put(rootSha1, root);

        putFilesFromOtherRepo(myRepo, otherRepo, files);

        FileUtilities.WriteToFile(Paths.get(myRepo.getLocationPath(), ".magit", "branches", i_Branch.getName() + ".txt").toString(),
                pointedCommitSha1);
    }

    public String findAncestorInRemote(String i_InitialCommitSha1, String i_CommitSha1, Repository i_MyRepo, Repository i_RemoteRepo) {
        String ancestor;

        if (i_CommitSha1 == null || i_CommitSha1.isEmpty()) {
            ancestor = "";
        } else if(!i_InitialCommitSha1.equals(i_CommitSha1) && i_MyRepo.getCommits().containsKey(i_CommitSha1)) {
            ancestor = i_CommitSha1;
        } else {
            Commit commit = i_RemoteRepo.getCommits().get(i_CommitSha1);
            String first = commit.getFirstPrecedingSha1();

            ancestor = findAncestorInRemote(i_InitialCommitSha1, first, i_MyRepo, i_RemoteRepo);

            if(ancestor.isEmpty()) {
                String second = commit.getSecondPrecedingSha1();
                ancestor = findAncestorInRemote(i_InitialCommitSha1, second, i_MyRepo, i_RemoteRepo);
            }
        }

        return ancestor;
    }

    public void putFilesFromOtherRepo(Repository i_MyRepo, Repository i_OtherRepo, List<Folder.Data> i_Files) {
        for(Folder.Data file : i_Files) {
            if(file.getFileType().equals(eFileType.FOLDER)) {
                Folder subFolder = i_OtherRepo.getFolders().get(file.getSHA1());
                i_MyRepo.getFolders().put(file.getSHA1(), subFolder);
                putFilesFromOtherRepo(i_MyRepo, i_OtherRepo, subFolder.getFiles());
            } else {
                Blob subFolder = i_OtherRepo.getBlobs().get(file.getSHA1());
                i_MyRepo.getBlobs().put(file.getSHA1(), subFolder);
            }
        }
    }

    public ConflictsManager MergeBranches(String i_RepoName, Branch i_Ours, Branch i_Theirs,
                                          Consumer<Consumer<String>> i_GetCommitDescriptionAction,
                                          Runnable i_FastForwardMergeMessageToUserAction,
                                          Consumer<String> i_MergeExceptionMessageAction) {

        ConflictsManager conflictsManager = null;
        Repository repository = getRepository(i_RepoName);

        if(i_Ours == i_Theirs) {
            i_MergeExceptionMessageAction.accept("Branch can't merge with itself.");
        } else {
            boolean isFastForwardMerge;

            try {
                isFastForwardMerge = checkIfFastForwardMerge(i_RepoName, i_Ours, i_Theirs, i_FastForwardMergeMessageToUserAction);

                if (!isFastForwardMerge) {
                    AncestorFinder ancestorFinder = new AncestorFinder(sha1 -> repository.getCommits().get(sha1));
                    String ancestorSha1 = ancestorFinder
                            .traceAncestor(i_Ours.getPointedCommitSha1(), i_Theirs.getPointedCommitSha1());

                    Commit ancestor = repository.getCommits().get(ancestorSha1);
                    Commit ours = repository.getCommits().get(i_Ours.getPointedCommitSha1());
                    Commit theirs = repository.getCommits().get(i_Theirs.getPointedCommitSha1());

                    Folder oursRootFolder = repository.getFolders().get(ours.getRootFolderSha1());
                    Folder theirsRootFolder = repository.getFolders().get(theirs.getRootFolderSha1());
                    Folder ancestorRootFolder = repository.getFolders().get(ancestor.getRootFolderSha1());

                    Map<String, String> oursPathToSha1Map = factory.createPathToSha1Map(i_RepoName, i_Ours);
                    Map<String, String> theirsPathToSha1Map = factory.createPathToSha1Map(i_RepoName, i_Theirs);
                    Map<String, String> ancestorPathToSha1Map = factory.createPathToSha1MapFromCommit(i_RepoName, ancestor);

                    List<Map<String, String>> pathToSha1Maps = new ArrayList<>();
                    pathToSha1Maps.add(oursPathToSha1Map);
                    pathToSha1Maps.add(theirsPathToSha1Map);
                    pathToSha1Maps.add(ancestorPathToSha1Map);

                    List<Conflict> conflicts = new ArrayList<>();

                    conflicts.addAll(findNewFiles(i_RepoName, activeRepositoryPath, oursRootFolder, theirsRootFolder, ancestorRootFolder, pathToSha1Maps));
                    conflicts.addAll(findDeletedFiles(i_RepoName, activeRepositoryPath, oursRootFolder, theirsRootFolder, ancestorRootFolder, pathToSha1Maps));
                    conflicts.addAll(findChangedFiles(i_RepoName, activeRepositoryPath, oursRootFolder, theirsRootFolder, ancestorRootFolder, pathToSha1Maps));

                    if (conflicts.size() == 0) {
                        i_GetCommitDescriptionAction.accept(s -> handleNoConflictsInMerge(i_RepoName, s, i_Theirs, i_MergeExceptionMessageAction));
                    } else {
                        conflictsManager = new ConflictsManager(this, i_RepoName, conflicts, i_Theirs);
                        conflictsManager.SetActionToGetCommitDesctiprionFromUser(i_GetCommitDescriptionAction);
                        conflictsManager.SetErrorMessageAction(i_MergeExceptionMessageAction);
                    }
                }
            } catch (MergeException e) {
                i_MergeExceptionMessageAction.accept(e.getMessage());
            } catch (IOException e) {
                i_MergeExceptionMessageAction.accept("Couldn't copy files");
            }
        }

        return conflictsManager;
    }

    private boolean checkIfFastForwardMerge(String i_RepoName, Branch i_Ours, Branch i_Theirs, Runnable i_FastForwardMergeMessageToUserAction) throws MergeException, IOException {
        Repository repository = getRepository(i_RepoName);

        Commit ours = repository.getCommits().get(i_Ours.getPointedCommitSha1());
        Commit theirs = repository.getCommits().get(i_Theirs.getPointedCommitSha1());

        boolean isTheirsAncestorOfOurs = checkIfOursAncestorOfTheirs(i_RepoName, theirs, ours);
        boolean isOursAncestorOfTheirs = checkIfOursAncestorOfTheirs(i_RepoName, ours, theirs);

        if(isTheirsAncestorOfOurs) {
            throw new MergeException("Active branch contains the selected branch for merge.");
        }
        else if(isOursAncestorOfTheirs){
            i_Ours.setPointedCommitSha1(i_Theirs.getPointedCommitSha1());
            new File(Paths.get(repository.getLocationPath(), ".magit", "branches", i_Theirs.getName() + ".txt").toString()).delete();
            FileUtilities.WriteToFile(Paths.get(repository.getLocationPath(), ".magit", "branches", i_Ours.getName() + ".txt").toString(),
                    i_Theirs.getPointedCommitSha1());
            repository.getBranches().remove(i_Theirs.getName());
            i_FastForwardMergeMessageToUserAction.run();
        }

        return isOursAncestorOfTheirs;
    }

    public void copyFilesInObjectsDir(String i_RepoName, String i_RemoteLocation, Branch i_BranchToCopyFrom) throws IOException {
        Repository repository = getRepository(i_RepoName);
        Commit commit = repository.getCommits().get(i_BranchToCopyFrom.getPointedCommitSha1());
        String remotePath = Paths.get(i_RemoteLocation, ".magit", "objects", commit.getSha1()).toString();
        String localPath = Paths.get(repository.getLocationPath(), ".magit", "objects", commit.getSha1()).toString();

        if(!new File(localPath).exists()) {
            FileUtils.copyFile(new File(remotePath), new File(localPath));
        }

        copyFilesInObjectsDirHelper(repository, i_RemoteLocation, commit.getRootFolderSha1());
    }

    private void copyFilesInObjectsDirHelper(Repository i_Repository, String i_RemoteLocation, String i_Sha1) throws IOException {
        String remotePath = Paths.get(i_RemoteLocation, ".magit", "objects", i_Sha1).toString();
        String localPath = Paths.get(i_Repository.getLocationPath(), ".magit", "objects", i_Sha1).toString();

        if(!new File(localPath).exists()) {
            FileUtils.copyFile(new File(remotePath), new File(localPath));
        }

        if(i_Repository.getFolders().containsKey(i_Sha1)) {
            Folder root = i_Repository.getFolders().get(i_Sha1);
            LinkedList<Folder.Data> files = root.getFiles();

            for(Folder.Data file: files) {
                copyFilesInObjectsDirHelper(i_Repository, i_RemoteLocation, file.getSHA1());
            }
        }
    }

    private void createFilesInWc(String i_RepoName, Branch i_Branch) {
        Repository repository = getRepository(i_RepoName);
        Commit commit = repository.getCommits().get(i_Branch.getPointedCommitSha1());

        createFilesInWcHelper(repository, repository.getLocationPath(), commit.getRootFolderSha1());
    }

    private void createFilesInWcHelper(Repository i_Repository, String i_CurrentPath, String i_FolderSha1) {
        Folder root = i_Repository.getFolders().get(i_FolderSha1);
        LinkedList<Folder.Data> files = root.getFiles();

        for(Folder.Data file: files) {
            String thisFilePath = Paths.get(i_CurrentPath, file.getName()).toString();
            File thisFile = new File(thisFilePath);

            if(file.getFileType().equals(eFileType.FOLDER)) {
                if(!thisFile.exists()) {
                    thisFile.mkdirs();
                }

                createFilesInWcHelper(i_Repository, thisFilePath, i_FolderSha1);
            } else {
                if(!thisFile.exists()) {
                    FileUtilities.WriteToFile(thisFilePath, i_Repository.getBlobs().get(file.getSHA1()).getText());
                }
            }
        }
    }

    private void handleNoConflictsInMerge(String i_RepoName, String i_CommitDescription, Branch i_MergedBranch, Consumer<String> i_MergeExceptionMessageAction) {
        try {
            this.commit(i_RepoName, i_CommitDescription, i_MergedBranch);
        } catch (IOException | EmptyWcException | CommitAlreadyExistsException e) {
            i_MergeExceptionMessageAction.accept(e.getMessage());
        }
    }

    private boolean checkIfOursAncestorOfTheirs(String i_RepoName, Commit i_Ours, Commit i_Theirs) {
        boolean result;
        Repository repository = getRepository(i_RepoName);

        if(i_Theirs == null || i_Theirs.getFirstPrecedingSha1() == null) {
            result = false;
        }
        else if (i_Theirs == i_Ours) {
            result = true;
        }
        else {
            Commit firstPreceding = repository.getCommits().get(i_Theirs.getFirstPrecedingSha1());
            Commit secondPreceding = repository.getCommits().get(i_Theirs.getSecondPrecedingSha1());

            result = checkIfOursAncestorOfTheirs(i_RepoName, i_Ours, firstPreceding) ||
                    checkIfOursAncestorOfTheirs(i_RepoName, i_Ours, secondPreceding);
        }

        return result;
    }

    private List<Conflict> findChangedFiles(String i_RepoName, String i_CurrentPath, IRepositoryFile i_Ours, IRepositoryFile i_Theirs, IRepositoryFile i_Ancestor, List<Map<String, String>> i_PathToSha1Maps) {
        List<Conflict> conflicts = new ArrayList<>();
        eMergeSituation mergeSituation = findMergeSituation(i_RepoName, i_CurrentPath, i_Ours, i_Theirs, i_Ancestor, i_PathToSha1Maps);

        if(i_Ours instanceof Blob) {
            switch (mergeSituation) {
                case SAME_NAME_DIFF_SHA1:
                case SAME_NAME_EQU_SHA1:
                case CHANGED_TO_SAME_IN_BOTH:
                case CHANGED_TO_DIFF_IN_BOTH:
                    Conflict conflict = new Conflict(this, i_Ours, i_Theirs, i_Ancestor);
                    conflict.SetConflictSituation(mergeSituation);
                    conflict.SetFileLocation(i_CurrentPath);
                    conflicts.add(conflict);
                    break;
                case OURS_SAME_THEIR_CHANGED:
                    mergeSituation.Solve(this, i_CurrentPath, i_Theirs);
                    break;
            }
        }
        else {
            conflicts.addAll(checkInsideFolder(i_RepoName, i_CurrentPath, i_Ours, i_PathToSha1Maps, this::findChangedFiles));
        }


        return conflicts;
    }

    private List<Conflict> findDeletedFiles(String i_RepoName, String i_CurrentPath, IRepositoryFile i_Ours, IRepositoryFile i_Theirs, IRepositoryFile i_Ancestor, List<Map<String, String>> i_PathToSha1Maps) {
        List<Conflict> conflicts = new ArrayList<>();
        eMergeSituation mergeSituation = findMergeSituation(i_RepoName, i_CurrentPath, i_Ours, i_Theirs, i_Ancestor, i_PathToSha1Maps);

        switch (mergeSituation) {
            case OURS_CHANGED_THEIRS_DELETED:
            case OURS_DELETED_THEIRS_CHANGED:
                Conflict conflict = new Conflict(this, i_Ours, i_Theirs, i_Ancestor);
                conflict.SetConflictSituation(mergeSituation);
                conflict.SetFileLocation(i_CurrentPath);
                conflicts.add(conflict);
                break;
            case OURS_SAME_THEIRS_DELETED:
                mergeSituation.Solve(this, i_CurrentPath, i_Ours);
                break;
            default:
                conflicts.addAll(checkInsideFolder(i_RepoName, i_CurrentPath, i_Ours, i_PathToSha1Maps, this::findDeletedFiles));
                conflicts.addAll(checkInsideFolder(i_RepoName, i_CurrentPath, i_Theirs, i_PathToSha1Maps, this::findDeletedFiles));
                break;
        }

        return conflicts;
    }

    private List<Conflict> findNewFiles(String i_RepoName, String i_CurrentPath, IRepositoryFile i_Ours, IRepositoryFile i_Theirs, IRepositoryFile i_Ancestor, List<Map<String, String>> i_PathToSha1Maps) {
        eMergeSituation mergeSituation = findMergeSituation(i_RepoName, i_CurrentPath, i_Ours, i_Theirs, i_Ancestor, i_PathToSha1Maps);

        if(mergeSituation.equals(eMergeSituation.NEW_FILE_IN_THEIRS)) {
            mergeSituation.Solve(this, i_CurrentPath, i_Theirs);
        }

        return new ArrayList<>(checkInsideFolder(i_RepoName, i_CurrentPath, i_Theirs, i_PathToSha1Maps, this::findNewFiles));
    }

    private List<Conflict> checkInsideFolder(String i_RepoName, String i_CurrentPath, IRepositoryFile i_Folder, List<Map<String, String>> i_PathToSha1Maps, IMergeTask i_MergeTask) {
        List<Conflict> conflicts = new ArrayList<>();
        Repository repository = getRepository(i_RepoName);

        if (i_Folder instanceof Folder) {
            Map<String, String> oursPathToSha1 = i_PathToSha1Maps.get(0);
            Map<String, String> theirsPathToSha1 = i_PathToSha1Maps.get(1);
            Map<String, String> ancestorPathToSha1 = i_PathToSha1Maps.get(2);

            for (Folder.Data file : ((Folder) i_Folder).getFiles()) {
                String filePath = Paths.get(i_CurrentPath, file.getName()).toString();
                IRepositoryFile ours = file.getFileType().equals(eFileType.FOLDER) ?
                        repository.getFolders().get(oursPathToSha1.get(filePath)) :
                        repository.getBlobs().get(oursPathToSha1.get(filePath));
                IRepositoryFile theirs = file.getFileType().equals(eFileType.FOLDER) ?
                        repository.getFolders().get(theirsPathToSha1.get(filePath)) :
                        repository.getBlobs().get(theirsPathToSha1.get(filePath));
                IRepositoryFile ancestor = file.getFileType().equals(eFileType.FOLDER) ?
                        repository.getFolders().get(ancestorPathToSha1.get(filePath)) :
                        repository.getBlobs().get(ancestorPathToSha1.get(filePath));

                conflicts.addAll(i_MergeTask.ExecuteTask(i_RepoName, filePath, ours, theirs, ancestor, i_PathToSha1Maps));
            }
        }

        return conflicts;
    }

    private eMergeSituation findMergeSituation(String i_RepoName, String i_CurrentPath, IRepositoryFile i_Ours, IRepositoryFile i_Theirs, IRepositoryFile i_Ancestor, List<Map<String, String>> i_PathToSha1Maps) {
        eMergeSituation mergeSituation;

        String oursSha1 = "";
        String theirsSha1 = "";
        String ancestorSha1 = "";

        String remoteRepositoryLocation = getRemoteRepositoryLocation(i_RepoName);
        String currentPath = remoteRepositoryLocation.isEmpty() ? i_CurrentPath : replaceRootPath(i_CurrentPath, remoteRepositoryLocation);

        if(i_Ours != null) {
            oursSha1 = i_Ours instanceof Folder ? DigestUtils.sha1Hex(((Folder) i_Ours).toStringForSha1(Paths.get(currentPath))) :
                    DigestUtils.sha1Hex(((Blob) i_Ours).toStringForSha1());
        }

        if(i_Theirs != null) {
            theirsSha1 = i_Theirs instanceof Folder ? DigestUtils.sha1Hex(((Folder) i_Theirs).toStringForSha1(Paths.get(currentPath))) :
                    DigestUtils.sha1Hex(((Blob) i_Theirs).toStringForSha1());
        }

        if(i_Ancestor != null) {
            ancestorSha1 = i_Ancestor instanceof Folder ? DigestUtils.sha1Hex(((Folder) i_Ancestor).toStringForSha1(Paths.get(currentPath))) :
                    DigestUtils.sha1Hex(((Blob) i_Ancestor).toStringForSha1());
        }

        boolean isFileExistsInOurs = !oursSha1.isEmpty();
        boolean isFileExistsInTheirs = !theirsSha1.isEmpty();
        boolean isFileExistsInAncestor = !ancestorSha1.isEmpty();
        boolean isOursEqualsTheirs = oursSha1.equals(theirsSha1);
        boolean isTheirsEqualsAncestor = theirsSha1.equals(ancestorSha1);
        boolean isAncestorEqualsOurs = ancestorSha1.equals(oursSha1);

        if(!isFileExistsInOurs && isFileExistsInTheirs && !isFileExistsInAncestor) {
            mergeSituation = eMergeSituation.NEW_FILE_IN_THEIRS;
        }
        else if(!isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
                !isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.OURS_DELETED_THEIRS_CHANGED;
        }
//        else if(!isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
//                !isOursEqualsTheirs && isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
//            mergeSituation = eMergeSituation.OURS_DELETED_THEIRS_SAME;
//        }
//        else if(isFileExistsInOurs && !isFileExistsInTheirs && !isFileExistsInAncestor &&
//                !isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
//            mergeSituation = eMergeSituation.NEW_FILE_IN_OURS;
//        }
        else if(isFileExistsInOurs && !isFileExistsInTheirs && isFileExistsInAncestor &&
                !isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.OURS_CHANGED_THEIRS_DELETED;
        }
        else if(isFileExistsInOurs && !isFileExistsInTheirs && isFileExistsInAncestor &&
                !isOursEqualsTheirs && !isTheirsEqualsAncestor && isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.OURS_SAME_THEIRS_DELETED;
        }
        else if (isFileExistsInOurs && isFileExistsInTheirs && !isFileExistsInAncestor &&
                isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.SAME_NAME_EQU_SHA1;
        }
        else if (isFileExistsInOurs && isFileExistsInTheirs && !isFileExistsInAncestor &&
                !isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.SAME_NAME_DIFF_SHA1;
        }
        else if (isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
                !isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.CHANGED_TO_DIFF_IN_BOTH;
        }
//        else if (isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
//                !isOursEqualsTheirs && isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
//            mergeSituation = eMergeSituation.OURS_CHANGED_THEIRS_SAME;
//        }
        else if (isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
                isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.CHANGED_TO_SAME_IN_BOTH;
        }
//        else if (isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
//                isOursEqualsTheirs && isTheirsEqualsAncestor && isAncestorEqualsOurs) {
//            mergeSituation = eMergeSituation.SAME_FILE_IN_ALL;
//        }
        else if (isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
                !isOursEqualsTheirs && !isTheirsEqualsAncestor && isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.OURS_SAME_THEIR_CHANGED;
        }
        else {
            mergeSituation = eMergeSituation.KEEP_STATE;
        }

        return mergeSituation;
    }

    public void CreateNewFileOnSystem(IRepositoryFile i_File, String i_FullPath) {
        if(i_File instanceof Folder) {
            factory.createFolder((Folder) i_File, i_FullPath);
        }
        else {
            FileUtilities.WriteToFile(i_FullPath, ((Blob) i_File).getText());
        }
    }

    private boolean IsRepoAlreadyExists(String i_Location) {
        File file = new File(i_Location + "//.magit");

        return file.exists();
    }

    private boolean IsRepoFolderAlreadyExists(String i_Location) {
        File file = new File(i_Location);

        return file.exists();
    }

    public void loadRepositoryFromXml(String i_XmlPath, StringProperty i_ProgressProperty) throws FileNotFoundException, RepositoryAlreadyExistsException, xmlErrorsException, FolderInLocationAlreadyExistsException, JAXBException {
        Path xmlPath;
        i_ProgressProperty.set("Reading xml file...");
        sleep();

        try {
            xmlPath = Paths.get(i_XmlPath);
        } catch (InvalidPathException ipe) {
            i_ProgressProperty.set("Input is not a path.");
            sleep();
            throw new xmlErrorsException("Input is not a path.");
        }

        i_ProgressProperty.set("Checking xml for errors...");

        XmlHelper xmlChecker = new XmlHelper(this, xmlPath);
        loadRepositoryFromXml(xmlChecker, i_ProgressProperty);
    }

    public void LoadRepositoryFromXml(InputStream i_XmlStream, String i_CurrentUserName, StringProperty i_ProgressProperty) throws FileNotFoundException, RepositoryAlreadyExistsException, xmlErrorsException, FolderInLocationAlreadyExistsException, JAXBException {
        Path xmlPath;
        i_ProgressProperty.set("Reading xml file...");
        sleep();
        i_ProgressProperty.set("Checking xml for errors...");
        sleep();
        XmlHelper xmlChecker = new XmlHelper(this, i_XmlStream, i_CurrentUserName);
        loadRepositoryFromXml(xmlChecker, i_ProgressProperty);
    }

    private void loadRepositoryFromXml(XmlHelper i_XmlHelper, StringProperty i_ProgressProperty) throws FileNotFoundException, RepositoryAlreadyExistsException, FolderInLocationAlreadyExistsException, xmlErrorsException, JAXBException {
        List<String> errors = i_XmlHelper.RunCheckOnXmlFile();

        if(errors.size() == 0) {
            this.Clear();
            i_ProgressProperty.set("Xml is valid.");
            sleep();
            MagitRepository magitRepository = i_XmlHelper.getMagitRepository();

            i_ProgressProperty.set("Checking if repository already exists...");
            sleep();
            boolean isRepoAlreadyExits = (repositories != null && activeRepositoryName != null && repositories.containsKey(activeRepositoryName) &&
                    repositories.get(activeRepositoryName).getLocationPath().equals(magitRepository.getLocation())) ||
                    IsRepoAlreadyExists(magitRepository.getLocation());

            if (isRepoAlreadyExits) {
                i_ProgressProperty.set("Repository is already exists.");
                sleep();
                activeRepositoryPath = Paths.get(magitRepository.getLocation()).toString().toLowerCase();
                throw new RepositoryAlreadyExistsException();
            }
            else if(IsRepoFolderAlreadyExists(magitRepository.getLocation())) {
                i_ProgressProperty.set("Folder in the given location is already exists.");
                sleep();
                throw new FolderInLocationAlreadyExistsException();
            }
            else {
                loadedProperty.set(false);
                i_ProgressProperty.set("Loading xml file...");
                sleep();
                activeRepositoryName = magitRepository.getName();
                repositories = repositories == null ? new HashMap<>() : repositories;
                repositories.put(activeRepositoryName, factory.createRepository(magitRepository));
                i_ProgressProperty.set("Load was executed successfully.");
                sleep();
                loadedProperty.set(true);
            }
        }
        else {
            i_ProgressProperty.set("Errors found.");
            throw new xmlErrorsException(errors);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {}
    }

    public void loadDataFromRepository(String i_RepositoryFullPath) throws IOException {
        this.Clear();
        String repoDetails = FileUtilities.ReadTextFromFile(Paths.get(i_RepositoryFullPath, ".magit", "details.txt").toString());
        List<String> repoDetailsList = StringUtilities.getLines(repoDetails);

        activeRepositoryName = repoDetailsList.get(0);
        repositories = repositories == null ? new HashMap<>() : repositories;
        repositories.put(activeRepositoryName, new Repository());
        Repository repository = repositories.get(activeRepositoryName);
        repository.setName(repoDetailsList.get(0));
        repository.setBranches(new HashMap<>());
        repository.setCommits(new HashMap<>());
        repository.setFolders(new HashMap<>());
        repository.setBlobs(new HashMap<>());
        repository.setLocationPath(Paths.get(i_RepositoryFullPath).toString());
        repository.setRemoteRepositoryLocation(repoDetailsList.size() == 2 ? repoDetailsList.get(1) : "");
        activeRepositoryPath = repository.getLocationPath();

        File branchesDirectory = new File(Paths.get(i_RepositoryFullPath, ".magit", "branches").toString());
        File[] branches = branchesDirectory.listFiles();

        if(branches != null) {
            List<String> localBranches = Arrays.stream(branches)
                    .map(f -> f.toPath().toString())
                    .filter(s -> !s.endsWith("head.txt") && s.contains(".txt"))
                    .collect(Collectors.toList());

            AtomicReference<String> remoteRepoName = new AtomicReference<>();
            List<String> remoteBranches = getRemoteDirectoryContentPaths(branches, remoteRepoName);

            for (String branchPath : localBranches) {
                loadBranch(repository.getName(), branchPath);
            }

            if(remoteBranches != null) {
                for (String remoteBranchPath : remoteBranches) {
                    loadRemoteBranch(repository.getName(), remoteBranchPath, remoteRepoName.get());
                }
            }

            File headFile = new File(Paths.get(i_RepositoryFullPath, ".magit", "branches", "head.txt").toString());

            try (Scanner scanner = new Scanner(headFile)) {
                String headBranchName = scanner.nextLine();
                Branch headBranch = repository.getBranches().get(headBranchName);
                headBranch.setIsHead(true);
                repository.setHeadBranch(headBranch);
            }
        }
    }

    private void loadRemoteBranch(String i_RepoName, String i_RemoteBranchPath, String i_RemoteRepoName) throws IOException {
        File branchFile = new File(i_RemoteBranchPath);
        Branch branch = Branch.parse(branchFile);
        Repository repository = getRepository(i_RepoName);

        if(repository.getBranches().containsKey(branch.getName())) {
            repository.getBranches().get(branch.getName()).setIsTracking(true);
            repository.getBranches().get(branch.getName()).setTrakingAfter(i_RemoteRepoName + "/" + branch.getName());
        }

        branch.setName(i_RemoteRepoName + "/" + branch.getName());
        branch.setIsRemote(true);
        repository.getBranches().put(branch.getName(), branch);

        if(branch.getPointedCommitSha1() != null && !branch.getPointedCommitSha1().isEmpty()) {
            loadCommitFile(i_RepoName, branch.getPointedCommitSha1());
        }
    }

    private List<String> getRemoteDirectoryContentPaths(File[] i_BranchesDirectoryFiles, AtomicReference<String> i_RemoteRepositoryName) {
        List<String> result = null;
        File[] remoteBranches = null;

        for(File file: i_BranchesDirectoryFiles) {
            if(file.isDirectory()) {
                remoteBranches = file.listFiles();
                i_RemoteRepositoryName.set(file.getName());
                break;
            }
        }

        if(remoteBranches != null) {
            result = Arrays.stream(remoteBranches)
                    .map(f -> f.toPath().toString())
                    .filter(s -> !s.endsWith("head.txt"))
                    .collect(Collectors.toList());
        }

        return result;
    }

    public void changeActiveRepository(String i_RepositoryFullPath) throws NotRepositoryFolderException, InvalidPathException, IOException {
        File repo = new File(Paths.get(i_RepositoryFullPath, ".magit").toString());

        if(repo.exists()) {
            loadedProperty.set(false);
            loadDataFromRepository(i_RepositoryFullPath);
            loadedProperty.set(true);
        }
        else {
            throw new NotRepositoryFolderException(String.format("The folder named \"%s\" is not a magit repository.", i_RepositoryFullPath));
        }
    }

    public boolean commit(String i_RepoName, String i_Description, Branch i_SecondPrecedingIfMerge) throws IOException, EmptyWcException, CommitAlreadyExistsException {
        boolean isCommitExecuted = false;
        Repository repository = getRepository(i_RepoName);
        AtomicReference<String> lastChangerRef = new AtomicReference<>();
        Map<String, String> pathToSha1Map = factory.createPathToSha1Map(i_RepoName, repository.getHeadBranch());
        Folder folder = walkInFolder(i_RepoName, repository.getLocationPath(), pathToSha1Map, lastChangerRef, false);

        if(folder != null) {
            String remoteRepositoryLocation = getRemoteRepositoryLocation(i_RepoName);
            String folderPath = remoteRepositoryLocation.isEmpty() ? repository.getLocationPath() : remoteRepositoryLocation;
            String folderSha1 = DigestUtils.sha1Hex(folder.toStringForSha1(Paths.get(folderPath)));
            boolean isFolderAlreadyExists = repository.getFolders().containsKey(folderSha1);

            if(!isFolderAlreadyExists || i_SecondPrecedingIfMerge != null) {
                if (!isFolderAlreadyExists) {
                    repository.getFolders().put(folderSha1, folder);
                }

                isCommitExecuted = true;
                Commit commit    = new Commit();
                commit.setMessage(i_Description);
                commit.setRootFolderSha1(folderSha1);
                commit.setFirstPrecedingCommitSha1(repository.getHeadBranch().getPointedCommitSha1());
                commit.setLastChanger(currentNameProperty.get());
                commit.setLastUpdate(new SimpleDateFormat(DATE_FORMAT).format(new Date(System.currentTimeMillis())));

                if(i_SecondPrecedingIfMerge != null) {
                    commit.setSecondPrecedingCommitSha1(i_SecondPrecedingIfMerge.getPointedCommitSha1());
                    new File(Paths.get(repository.getLocationPath(),
                            ".magit", "branches", i_SecondPrecedingIfMerge.getName() + ".txt").toString()).delete();
                    repository.getBranches().remove(i_SecondPrecedingIfMerge.getName());
                }

                String commitSha1 = DigestUtils.sha1Hex(commit.toStringForSha1());

                if (!repository.getCommits().containsKey(commitSha1)) {
                    String pointedBranchInHeadPath = Paths.get(repository.getLocationPath(),
                            ".magit", "branches", repository.getHeadBranch().getName() + ".txt").toString();
                    repository.getCommits().put(commitSha1, commit);
                    repository.getHeadBranch().setPointedCommitSha1(commitSha1);


                    FileUtilities.WriteToFile(pointedBranchInHeadPath, commitSha1);
                    FileUtilities.ZipFile(folderSha1, folder.toString(), Paths.get(repository.getLocationPath(),
                            ".magit", "objects", folderSha1).toString());
                    FileUtilities.ZipFile(commitSha1, commit.toString(), Paths.get(repository.getLocationPath(),
                            ".magit", "objects", commitSha1).toString());
                } else {
                    throw new CommitAlreadyExistsException(commitSha1);
                }
            }
        } else {
            throw new EmptyWcException();
        }

        return isCommitExecuted;
    }

    private Folder walkInFolder(String i_RepoName, String i_ParentPath, Map<String, String> i_PathToSha1Map, AtomicReference<String> ref_LastChanger, boolean i_IsNewItems) throws IOException {
        Folder folder;
        int filesCount = 0;
        // convert path to file and gets the content of the folder
        File parentFolderFile              = new File(i_ParentPath);
        File[] filesInParentFolder         = parentFolderFile.listFiles();
        List<File> filesInParentFolderList = null;

        if(filesInParentFolder != null) {
            filesInParentFolderList = Arrays.stream(filesInParentFolder)
                    .filter(f -> !f.getName().contains(".magit"))
                    .collect(Collectors.toList());
        }

//        if (filesInParentFolderList != null && filesInParentFolderList.size() != 0) {
            long maxLastModified = 0;

            folder = new Folder();

            for (File file : filesInParentFolderList) {
                filesCount++;
                String returnedFileSha1FromPathToSha1Map = i_PathToSha1Map.get(file.getAbsolutePath());
                boolean isNewItem = returnedFileSha1FromPathToSha1Map == null;
                boolean isFolder = file.isDirectory();
                String sha1;

                if(isFolder) {
                    sha1 = checkDelta(i_RepoName, file, i_PathToSha1Map, ref_LastChanger, isNewItem);
                } else {
                    sha1 = checkDelta(i_RepoName, file, i_PathToSha1Map, ref_LastChanger, i_IsNewItems);

                    if(isNewItem) {
                        ref_LastChanger.set(currentNameProperty.get());
                        file.setLastModified(System.currentTimeMillis());
                    } else {
                        String parentFolderSha1 = i_PathToSha1Map.get(i_ParentPath);
                        Folder parentFolder = repositories.get(activeRepositoryName).getFolders().get(parentFolderSha1);
                        Folder.Data oldItemData = getDataBySha1(parentFolder, returnedFileSha1FromPathToSha1Map);
                        boolean isChanged = !sha1.equals(oldItemData.getSHA1());

                        if(isChanged) {
                            ref_LastChanger.set(currentNameProperty.get());
                            file.setLastModified(System.currentTimeMillis());
                        } else {
                            ref_LastChanger.set(oldItemData.getLastChanger());
                        }
                    }
                }

                if (sha1 != null) {
                    Folder.Data itemDataToAdd = Folder.Data.Parse(file, sha1, getCurrentUserName());
                    itemDataToAdd.setLastChanger(ref_LastChanger.get());

                    if (file.lastModified() > maxLastModified) {
                        maxLastModified = file.lastModified();
                    }

                    folder.addFile(itemDataToAdd);
                } else {
                    filesCount--;
                }
            }

            if(filesCount > 0 || i_ParentPath.equals(activeRepositoryPath) || new File(i_ParentPath).getParent().equals(activeRepositoryPath)) {
                folder.setIsRoot(i_ParentPath.equals(activeRepositoryPath));
                folder.getFiles().sort(Folder.Data::compare);
                parentFolderFile.setLastModified(maxLastModified);
            } else {
                FileUtilities.removeFile(new File(i_ParentPath));
                folder = null;
            }
//        }

        return folder;
    }

    private String checkDelta(String i_RepoName, File i_FileToCheckDelta, Map<String, String> i_PathToSha1Map, AtomicReference<String> ref_LastChanger, boolean i_IsNewItems) throws IOException {
        String sha1;
        String filePath = i_FileToCheckDelta.toPath().toString();

        if(!i_FileToCheckDelta.isDirectory()) {
            String blobContent = FileUtilities.ReadTextFromFile(filePath);
            sha1 = DigestUtils.sha1Hex(Blob.getSha1FromContent(blobContent).replaceAll("\\s", "") + new File(filePath).getName());

            if(!repositories.get(activeRepositoryName).getBlobs().containsKey(sha1)) {
                Blob blob = new Blob();
                blob.setName(new File(filePath).getName());
                blob.setText(blobContent);
                repositories.get(activeRepositoryName).getBlobs().put(sha1, blob);
                FileUtilities.ZipFile(sha1, blobContent, Paths.get(activeRepositoryPath,".magit", "objects", sha1).toString());
            }
        }
        else {
            Folder newFolder = walkInFolder(i_RepoName, filePath, i_PathToSha1Map, ref_LastChanger, i_IsNewItems);

            if(newFolder != null) {
                String currentPath = i_FileToCheckDelta.toPath().toString();
                String remoteRepositoryLocation = getRemoteRepositoryLocation(i_RepoName);

                if(!remoteRepositoryLocation.isEmpty()) {
                    currentPath = replaceRootPath(i_FileToCheckDelta.toPath().toString(), remoteRepositoryLocation);
                }

                sha1 = DigestUtils.sha1Hex(newFolder.toStringForSha1(Paths.get(currentPath)));

                if (!repositories.get(activeRepositoryName).getFolders().containsKey(sha1)) {
                    repositories.get(activeRepositoryName).getFolders().put(sha1, newFolder);
                    FileUtilities.ZipFile(sha1, newFolder.toString(), Paths.get(activeRepositoryPath, ".magit", "objects", sha1).toString());
                }
            }
            else {
                sha1 = null;
            }
        }

        return sha1;
    }

    public String replaceRootPath(String i_OriginalPath, String i_RemotePath) {
        String[] originalPathParts = i_OriginalPath.split(Pattern.quote("\\"));
        String[] remotePathParts = i_RemotePath.split(Pattern.quote("\\"));
        originalPathParts[2] = remotePathParts[2];

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < originalPathParts.length; i++) {
            if(i > 0) {
                sb.append("\\");
            }

            sb.append(originalPathParts[i]);
        }

            return sb.toString();
    }

    private Folder.Data getDataByName(Folder i_ParentFolder, eFileType i_FileType, String i_Name) {
        Folder.Data toReturn = null;

        if(i_ParentFolder != null) {
            List<Folder.Data> folderData = i_ParentFolder.getFiles();

            for (Folder.Data data : folderData) {
                if (data.getName().equals(i_Name) && data.getFileType().equals(i_FileType)) {
                    toReturn = data;
                    break;
                }
            }
        }
        return toReturn;
    }

    private Folder.Data getDataBySha1(Folder i_ParentFolder, String i_Sha1) {
        Folder.Data toReturn = null;

        if(i_ParentFolder != null && i_Sha1 != null) {
            List<Folder.Data> folderData = i_ParentFolder.getFiles();

            for (Folder.Data data : folderData) {
                if (data.getSHA1().equals(i_Sha1)) {
                    toReturn = data;
                    break;
                }
            }
        }

        return toReturn;
    }

    public List<String> showCurrentCommitFiles(String i_RepoName) {
        List<String> commitFilesInfo = new ArrayList<>();
        Repository repository = getRepository(i_RepoName);
        String pointedCommitSha1 = repository.getHeadBranch().getPointedCommitSha1();

        if(pointedCommitSha1 != null && !pointedCommitSha1.isEmpty()) {
            Commit pointedCommit = repository.getCommits().get(pointedCommitSha1);
            String rootFolderSha1 = pointedCommit.getRootFolderSha1();
            Folder rootFolder = repository.getFolders().get(rootFolderSha1);

            List<Folder.Data> folderItems = rootFolder.getFiles();

            for (Folder.Data item : folderItems) {
                commitFilesInfo.addAll(folderDataToString(item, repository.getLocationPath()));
            }
        }

        return commitFilesInfo;
    }

    public List<Difference> getCommitDifference(String i_RepoName, String i_CommitSha1) {
        List<Difference> differences = new LinkedList<>();
        Repository repository = getRepository(i_RepoName);
        Commit commit = repository.getCommits().get(i_CommitSha1);

        if(!commit.getFirstPrecedingSha1().isEmpty()) {
            Difference firstDiff = findDiffBetweenCommits(i_RepoName, i_CommitSha1, commit.getFirstPrecedingSha1());
            differences.add(firstDiff);

            if(!commit.getSecondPrecedingSha1().isEmpty()) {
                Difference secondDiff = findDiffBetweenCommits(i_RepoName, i_CommitSha1, commit.getSecondPrecedingSha1());
                differences.add(secondDiff);
            }
        }

        return differences;
    }

    private Difference findDiffBetweenCommits(String i_RepoName, String i_CurrentCommitSha1, String i_PrecedingSha1) {
        Repository repository = getRepository(i_RepoName);

        Commit currentCommit = repository.getCommits().get(i_CurrentCommitSha1);
        Map<String, String> currentCommitPathToSha1 = factory.createPathToSha1MapFromCommit(i_RepoName, currentCommit);
        Commit firstPreceding = repository.getCommits().get(i_PrecedingSha1);
        Map<String, String> precedingCommitPathToSha1 = factory.createPathToSha1MapFromCommit(i_RepoName, firstPreceding);
        Difference difference = new Difference();

        List<Folder.Data> newFiles = getNewDiff(i_RepoName, difference, currentCommitPathToSha1, precedingCommitPathToSha1);
        List<Folder.Data> changedFiles = getChangedDiff(i_RepoName, difference, currentCommitPathToSha1, precedingCommitPathToSha1);
        List<Folder.Data> deletedFiles = getDeletedDiff(i_RepoName, difference, currentCommitPathToSha1, precedingCommitPathToSha1);

        difference.setNewFiles(newFiles);
        difference.setChangedFiles(changedFiles);
        difference.setDeletedFiles(deletedFiles);

        return difference;
    }

    private List<Folder.Data> getDeletedDiff(String i_RepoName, Difference i_Diff, Map<String, String> i_CurrentCommitPathToSha1, Map<String, String> i_PrecedingCommitPathToSha1) {
        List<Folder.Data> diff = new LinkedList<>();
        String repoLocation = getRepositoryPath(i_RepoName);
        Repository repository = getRepository(i_RepoName);

        for(Map.Entry<String, String> mapEntry: i_PrecedingCommitPathToSha1.entrySet()) {
            if(!i_CurrentCommitPathToSha1.containsKey(mapEntry.getKey()) && !mapEntry.getKey().equals(repoLocation)) {
                File thisFile = new File(mapEntry.getKey());
                String parentSha1 = i_PrecedingCommitPathToSha1.get(thisFile.getParent());
                Folder parent = repository.getFolders().get(parentSha1);
                Folder.Data file = getFile(parent, mapEntry.getValue());
                file.setName(removeServerPath(mapEntry.getKey()));

                Folder.Data clonedFile = new Folder.Data();
                clonedFile.setName(removeServerPath(mapEntry.getKey()));
                clonedFile.setLastChanger(file.getLastChanger());
                clonedFile.setSHA1(file.getSHA1());
                clonedFile.setlastUpdate(file.getlastUpdate());
                clonedFile.setFileType(file.getFileType());
                clonedFile.setCreationTimeMillis(file.getCreationTimeMillis());

                diff.add(clonedFile);

                if(!thisFile.isDirectory()) {
                    i_Diff.addBlob(mapEntry.getValue(), repository.getBlobs().get(mapEntry.getValue()));
                }
            }
        }

        return diff;
    }

    private List<Folder.Data> getChangedDiff(String i_RepoName, Difference i_Diff, Map<String, String> i_CurrentCommitPathToSha1, Map<String, String> i_PrecedingCommitPathToSha1) {
        List<Folder.Data> diff = new LinkedList<>();
        String repoLocation = getRepositoryPath(i_RepoName);
        Repository repository = getRepository(i_RepoName);

        for(Map.Entry<String, String> mapEntry: i_PrecedingCommitPathToSha1.entrySet()) {
            if(i_CurrentCommitPathToSha1.containsKey(mapEntry.getKey()) && !mapEntry.getKey().equals(repoLocation)) {
                String precedingItemSha1 = mapEntry.getValue();
                String currentCommitItemSha1 = i_CurrentCommitPathToSha1.get(mapEntry.getKey());
                boolean isItemsDiff = !precedingItemSha1.equals(currentCommitItemSha1);

                if(isItemsDiff) {
                    File thisFile = new File(mapEntry.getKey());
                    String parentSha1 = i_CurrentCommitPathToSha1.get(thisFile.getParent());
                    Folder parent = repository.getFolders().get(parentSha1);
                    Folder.Data file = getFile(parent, currentCommitItemSha1);

                    Folder.Data clonedFile = new Folder.Data();
                    clonedFile.setName(removeServerPath(mapEntry.getKey()));
                    clonedFile.setLastChanger(file.getLastChanger());
                    clonedFile.setSHA1(file.getSHA1());
                    clonedFile.setlastUpdate(file.getlastUpdate());
                    clonedFile.setFileType(file.getFileType());
                    clonedFile.setCreationTimeMillis(file.getCreationTimeMillis());

                    diff.add(clonedFile);

                    if(!thisFile.isDirectory()) {
                        i_Diff.addBlob(currentCommitItemSha1, repository.getBlobs().get(currentCommitItemSha1));
                    }
                }
            }
        }

        return diff;
    }

    private List<Folder.Data> getNewDiff(String i_RepoName, Difference i_Diff, Map<String, String> i_CurrentCommitPathToSha1, Map<String, String> i_PrecedingCommitPathToSha1) {
        List<Folder.Data> diff = new LinkedList<>();
        String repoLocation = getRepositoryPath(i_RepoName);
        Repository repository = getRepository(i_RepoName);

        for(Map.Entry<String, String> mapEntry: i_CurrentCommitPathToSha1.entrySet()) {
            if(!i_PrecedingCommitPathToSha1.containsKey(mapEntry.getKey()) && !mapEntry.getKey().equals(repoLocation)) {
                File thisFile = new File(mapEntry.getKey());
                String parentSha1 = i_CurrentCommitPathToSha1.get(thisFile.getParent());
                Folder parent = repository.getFolders().get(parentSha1);
                Folder.Data file = getFile(parent, mapEntry.getValue());

                Folder.Data clonedFile = new Folder.Data();
                clonedFile.setName(removeServerPath(mapEntry.getKey()));
                clonedFile.setLastChanger(file.getLastChanger());
                clonedFile.setSHA1(file.getSHA1());
                clonedFile.setlastUpdate(file.getlastUpdate());
                clonedFile.setFileType(file.getFileType());
                clonedFile.setCreationTimeMillis(file.getCreationTimeMillis());

                diff.add(clonedFile);

                if(!thisFile.isDirectory()) {
                    i_Diff.addBlob(mapEntry.getValue(), repository.getBlobs().get(mapEntry.getValue()));
                }
            }
        }

        return diff;
    }

    private String removeServerPath(String i_Path) {
        String[] pathParts = i_Path.split(Pattern.quote("\\"));
        StringBuilder sb = new StringBuilder();

        for(int i = 4; i < pathParts.length; i++) {
            sb.append("/");
            sb.append(pathParts[i]);
        }

        return sb.toString();
    }

    private Folder.Data getFile(Folder i_Folder, String i_FileSha1) {
        Folder.Data res = null;

        for (Folder.Data file : i_Folder.getFiles()) {
            if (file.getSHA1().equals(i_FileSha1)) {
                res = file;
                break;
            }
        }

        return res;
    }

    public List<List<List<String>>> getCommitDiff(String i_RepoName, String i_CommitSha1) {
        List<List<List<String>>> wcStatus = null;
        Commit commit = repositories.get(activeRepositoryName).getCommits().get(i_CommitSha1);

        if(!commit.getFirstPrecedingSha1().isEmpty()) {
            wcStatus = new ArrayList<>();
            List<List<String>> diffFirstPreceding = new ArrayList<>();
            wcStatus.add(diffFirstPreceding);

            findDiffBetweenCommits(i_RepoName, i_CommitSha1, commit.getFirstPrecedingSha1(), diffFirstPreceding);

            if(!commit.getSecondPrecedingSha1().isEmpty()) {
                List<List<String>> diffSecondPreceding = new ArrayList<>();
                wcStatus.add(diffSecondPreceding);

                findDiffBetweenCommits(i_RepoName, i_CommitSha1, commit.getSecondPrecedingSha1(), diffSecondPreceding);
            }
        }

        return wcStatus;
    }

    private void findDiffBetweenCommits(String i_RepoName, String i_CurrentCommitSha1, String i_PrecedingSha1, List<List<String>> i_Diff) {
        Commit currentCommit = repositories.get(activeRepositoryName).getCommits().get(i_CurrentCommitSha1);
        Map<String, String> currentCommitPathToSha1 = factory.createPathToSha1MapFromCommit(i_RepoName, currentCommit);
        Commit firstPreceding = repositories.get(activeRepositoryName).getCommits().get(i_PrecedingSha1);
        Map<String, String> precedingCommitPathToSha1 = factory.createPathToSha1MapFromCommit(i_RepoName, firstPreceding);

        List<String> deletedItems = new ArrayList<>();
        List<String> newItems = new ArrayList<>();
        List<String> changedItems = new ArrayList<>();

        i_Diff.add(deletedItems);
        i_Diff.add(newItems);
        i_Diff.add(changedItems);

        getNewDiff(i_RepoName, newItems, currentCommitPathToSha1, precedingCommitPathToSha1);
        getChangedDiff(i_RepoName, changedItems, currentCommitPathToSha1, precedingCommitPathToSha1);
        getDeletedDiff(i_RepoName, deletedItems, currentCommitPathToSha1, precedingCommitPathToSha1);
    }

    private void getDeletedDiff(String i_RepoName, List<String> i_DeletedItems, Map<String, String> i_CurrentCommitPathToSha1, Map<String, String> i_PrecedingCommitPathToSha1) {
        String repoLocation = getRepositoryPath(i_RepoName);

        for(Map.Entry<String, String> mapEntry: i_PrecedingCommitPathToSha1.entrySet()) {
            if(mapEntry.getKey().equals(repoLocation)) {
                i_DeletedItems.add(String.format("%s;%s", mapEntry.getKey(), mapEntry.getValue()));
            }

            if(!i_CurrentCommitPathToSha1.containsKey(mapEntry.getKey()) && !mapEntry.getKey().equals(repoLocation)) {
                i_DeletedItems.add(String.format("%s;%s", mapEntry.getKey(), mapEntry.getValue()));
            }
        }
    }

    private void getChangedDiff(String i_RepoName, List<String> i_ChangedItems, Map<String, String> i_CurrentCommitPathToSha1, Map<String, String> i_PrecedingCommitPathToSha1) {
        String repoLocation = getRepositoryPath(i_RepoName);

        for(Map.Entry<String, String> mapEntry: i_PrecedingCommitPathToSha1.entrySet()) {
            if(mapEntry.getKey().equals(repoLocation)) {
                i_ChangedItems.add(String.format("%s;%s", mapEntry.getKey(), mapEntry.getValue()));
            }

            if(i_CurrentCommitPathToSha1.containsKey(mapEntry.getKey()) && !mapEntry.getKey().equals(repoLocation)) {
                String precedingItemSha1 = mapEntry.getValue();
                String currentCommitItemSha1 = i_CurrentCommitPathToSha1.get(mapEntry.getKey());
                boolean isItemsDiff = !precedingItemSha1.equals(currentCommitItemSha1);

                if(isItemsDiff) {
                    i_ChangedItems.add(String.format("%s;%s", mapEntry.getKey(), mapEntry.getValue()));
                }
            }
        }
    }

    private void getNewDiff(String i_RepoName, List<String> i_NewItems, Map<String, String> i_CurrentCommitPathToSha1, Map<String, String> i_PrecedingCommitPathToSha1) {
        String repoLocation = getRepositoryPath(i_RepoName);

        for(Map.Entry<String, String> mapEntry: i_CurrentCommitPathToSha1.entrySet()) {
            if(mapEntry.getKey().equals(repoLocation)) {
                i_NewItems.add(String.format("%s;%s", mapEntry.getKey(), mapEntry.getValue()));
            }

            if(!i_PrecedingCommitPathToSha1.containsKey(mapEntry.getKey()) && !mapEntry.getKey().equals(repoLocation)) {
                i_NewItems.add(String.format("%s;%s", mapEntry.getKey(), mapEntry.getValue()));
            }
        }
    }

    public List<List<String>> getWorkingCopyDelta(String i_RepoName) {
        List<List<String>> wcStatus = new ArrayList<>();
        List<String> deletedItems = new ArrayList<>();
        List<String> newItems = new ArrayList<>();
        List<String> changedItems = new ArrayList<>();

        wcStatus.add(deletedItems);
        wcStatus.add(newItems);
        wcStatus.add(changedItems);

        Repository repository = getRepository(i_RepoName);

        if(repository != null && repository.getHeadBranch() != null) {
            String pointedCommitSha1 = repository.getHeadBranch().getPointedCommitSha1();
            Map<String, String> pathToSha1Map = factory.createPathToSha1Map(i_RepoName, repository.getHeadBranch());

            if (!pointedCommitSha1.isEmpty()) {
                try {
                    getNewItems(i_RepoName, pathToSha1Map, repository.getLocationPath(), newItems);
                    Commit currentCommit = repository.getCommits().get(pointedCommitSha1);
                    String rootFolderSha1 = currentCommit.getRootFolderSha1();

                    if (rootFolderSha1 != null) {
                        Folder currentRootFolder = repository.getFolders().get(rootFolderSha1);
                        getDeletedItems(i_RepoName, currentRootFolder, repository.getLocationPath(), deletedItems);
                    }

                    getChangedItems(i_RepoName, pathToSha1Map, repository.getLocationPath(), changedItems);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return wcStatus;
    }

    private void getDeletedItems(String i_RepoName, IRepositoryFile i_CurrentFile, String i_CurrentPath, List<String> i_DeletedItems) {
        if(i_CurrentFile instanceof Folder) {
            List<Folder.Data> itemsInCurrentFolder = ((Folder)i_CurrentFile).getFiles();
            Repository repository = getRepository(i_RepoName);

            for(Folder.Data item: itemsInCurrentFolder) {
                String path = Paths.get(i_CurrentPath, item.getName()).toString();
                File file = new File(path);

                if(item.getFileType().equals(eFileType.FOLDER)){
                    Folder subFolder = repository.getFolders().get(item.getSHA1());

                    if(file.exists()) {
                        getDeletedItems(i_RepoName, subFolder, path, i_DeletedItems);
                    }
                    else {
                        i_DeletedItems.addAll(folderDataToStringList(subFolder.getFiles(), path));
                        i_DeletedItems.add(itemDataToString(item, i_CurrentPath));
                    }
                }
                else {
                    if(!file.exists()){
                        i_DeletedItems.add(itemDataToString(item, i_CurrentPath));
                    }
                }
            }
        }
    }

    private void getChangedItems(String i_RepoName, Map<String, String> i_PathToSha1Map, String i_CurrentPath, List<String> i_ChangedItems) throws IOException {
        File folder = new File(i_CurrentPath);
        File[] filesInFolder = folder.listFiles();

        if(filesInFolder != null) {
            List<File> filesInFolderList = Arrays.stream(filesInFolder).filter(f -> !f.getName().contains(".magit")).collect(Collectors.toList());
            String sha1;
            Repository repository = getRepository(i_RepoName);

            for (File file : filesInFolderList) {
                String path = file.toPath().toString();

                if (i_PathToSha1Map.containsKey(path)) {
                    if (file.isDirectory()) {
                        sha1 = factory.createFolder(i_RepoName, path, null, this.getCurrentUserName());

                        if (!repository.getFolders().containsKey(sha1)) {
                            Folder.Data folderData = Folder.Data.Parse(file, sha1, getCurrentUserName());
                            i_ChangedItems.add(itemDataToString(folderData, i_CurrentPath));
                            getChangedItems(i_RepoName, i_PathToSha1Map, path, i_ChangedItems);
                        }
                    } else {
                        sha1 = factory.createBlob(path);

                        if (!repository.getBlobs().containsKey(sha1)) {
                            Folder.Data folderData = Folder.Data.Parse(file, sha1, getCurrentUserName());
                            i_ChangedItems.add(itemDataToString(folderData, i_CurrentPath));
                        }
                    }
                }
            }
        }
    }

    private void getNewItems(String i_RepoName, Map<String, String> i_PathToSha1Map, String i_CurrentPath, List<String> i_NewItems) throws IOException {
        File folder = new File(i_CurrentPath);
        File[] filesInFolder = folder.listFiles();

        if(filesInFolder != null) {
            List<File> filesInFolderList = Arrays.stream(filesInFolder)
                    .filter(f -> !f.getName().contains(".magit"))
                    .collect(Collectors.toList());
            String sha1;

            for (File file : filesInFolderList) {
                String path = file.toPath().toString();
                if (!i_PathToSha1Map.containsKey(path)) {
                    if (file.isDirectory()) {
                        sha1 = factory.createFolder(i_RepoName, path, new SimpleDateFormat(DATE_FORMAT).format(new Date(file.lastModified())), this.getCurrentUserName());
                        List<String> newItems = folderDataToStringList(factory.getTmpFolders().get(sha1).getFiles(), path);
                        i_NewItems.addAll(newItems);

                        // empty folder is not considered new item
                        if(newItems.size() != 0) {
                            Folder.Data folderData = Folder.Data.Parse(file, sha1, getCurrentUserName());
                            String folderDataString = itemDataToString(folderData, i_CurrentPath);
                            i_NewItems.add(folderDataString);
                        }
                    } else {
                        sha1 = factory.createBlob(path);
                        Folder.Data blobData = Folder.Data.Parse(file, sha1, getCurrentUserName());
                        i_NewItems.add(itemDataToString(blobData, i_CurrentPath));
                    }
                } else {
                    if (file.isDirectory()) {
                        getNewItems(i_RepoName, i_PathToSha1Map, path, i_NewItems);
                    }
                }
            }
        }
    }

    private List<String> folderDataToStringList(List<Folder.Data> i_FolderItemsData, String i_CurrentPath) {
        List<String> result = new ArrayList<>();

        for(Folder.Data item: i_FolderItemsData) {
            result.addAll(folderDataToString(item, i_CurrentPath));
        }

        return result;
    }

    private List<String> folderDataToString(Folder.Data i_ItemData, String i_CurrentPath) {
        List<String> result = new ArrayList<>();

        if(i_ItemData.getFileType().equals(eFileType.FOLDER)) {
            Folder folder = repositories.get(activeRepositoryName).getFolders().get(i_ItemData.getSHA1());

            if(folder == null) {
                folder = factory.getTmpFolders().get(i_ItemData.getSHA1());
            }

            if(folder != null) {
                List<Folder.Data> folderData = folder.getFiles();
                result.add(itemDataToString(i_ItemData, i_CurrentPath));

                for (Folder.Data item : folderData) {
                    String itemPath = Paths.get(i_CurrentPath, i_ItemData.getName()).toString();

                    if (item.getFileType().equals(eFileType.FOLDER)) {
                        result.addAll(folderDataToString(item, itemPath));
                    } else {
                        result.add(itemDataToString(item, itemPath));
                    }
                }
            }
        }
        else {
            result.add(itemDataToString(i_ItemData, i_CurrentPath));
        }

        return result;
    }

    private String itemDataToString(Folder.Data i_Data, String i_CurrentPath) {
        StringBuilder sb = new StringBuilder();
        String[] parts = i_Data.toString().split(";");
        parts[0] = Paths.get(i_CurrentPath, parts[0]).toString();

        for (String part : parts) {
            sb.append(part);
            sb.append(";");
        }

        return sb.toString();
    }

    public List<String> showAllBranches() {
        List<String> branchesInfo = new ArrayList<>();
        Map<String, Branch> branchesMap = repositories.get(activeRepositoryName).getBranches();

        for(Map.Entry<String, Branch> branch: branchesMap.entrySet()) {
            StringBuilder sb = new StringBuilder();

            String pointedCommitSha1 = branch.getValue().getPointedCommitSha1();
            boolean isPointedCommitSha1Init = pointedCommitSha1 != null && !pointedCommitSha1.isEmpty();
            String commitMessage = isPointedCommitSha1Init ? repositories.get(activeRepositoryName).getCommits().get(branch.getValue().getPointedCommitSha1()).getMessage() : "-";

            sb.append(branch.getValue().getName());
            sb.append(";");
            sb.append(isPointedCommitSha1Init ? pointedCommitSha1 : "-");
            sb.append(";");
            sb.append(commitMessage);

            if(branch.getValue().isHead()) {
                sb.append(";");
                sb.append("Head branch");
            }

            branchesInfo.add(sb.toString());
        }

        return branchesInfo;
    }

    public boolean isBranchNameExists(String i_RepoName, String i_BranchName) {
        return getRepository(i_RepoName).getBranches().containsKey(i_BranchName);
    }

    public void createNewBranch(String i_RepoName, String i_BranchName) throws PointedCommitEmptyException {
        Repository repository = getRepository(i_RepoName);
        Branch newBranch = new Branch();
        newBranch.setName(i_BranchName);
        String headBranchPointedCommitSha1 = repository.getHeadBranch().getPointedCommitSha1();

        if(headBranchPointedCommitSha1 != null) {
            newBranch.setPointedCommitSha1(headBranchPointedCommitSha1);
        }
        else {
            throw new PointedCommitEmptyException();
        }

        String branchPath = Paths.get(repository.getLocationPath(), ".magit", "branches", newBranch.getName() + ".txt").toString();
        FileUtilities.WriteToFile(branchPath, headBranchPointedCommitSha1);
        repository.getBranches().put(i_BranchName, newBranch);
    }

    public void deleteBranch(String i_RepoName, String i_BranchName) throws IOException {
        Repository repository = getRepository(i_RepoName);
        Files.delete(Paths.get(repository.getLocationPath(), ".magit", "branches", i_BranchName + ".txt"));
        repository.getBranches().remove(i_BranchName);
    }

    public void CreateRTB(String i_RepoName, String i_BranchName) {
        String rtbName = i_BranchName.split("/")[1];
        Repository repository = getRepository(i_RepoName);
        String pointedCommit = repository.getBranches().get(i_BranchName).getPointedCommitSha1();

        String rtbLocation = Paths.get(repository.getLocationPath(), ".magit", "branches", rtbName + ".txt").toString();
        FileUtilities.WriteToFile(rtbLocation, pointedCommit);
        Branch rtbBranch = new Branch();
        rtbBranch.setPointedCommitSha1(pointedCommit);
        rtbBranch.setTrakingAfter(i_BranchName);
        rtbBranch.setIsTracking(true);
        rtbBranch.setName(rtbName);
        repository.getBranches().put(rtbName, rtbBranch);
    }

    public void checkout(String i_RepoName, String i_BranchName, boolean i_IsSkipWcCheck) throws Exception {
        if(!i_BranchName.contains("/")) {
            List<List<String>> wcStatus = null;
            List<String> deletedFiles = null;
            List<String> newFiles = null;
            List<String> changedFiles = null;

            if (!i_IsSkipWcCheck) {
                wcStatus = getWorkingCopyDelta(i_RepoName);
                deletedFiles = wcStatus.get(0);
                newFiles = wcStatus.get(1);
                changedFiles = wcStatus.get(2);
            }

            if (i_IsSkipWcCheck || deletedFiles.size() == 0 && newFiles.size() == 0 && changedFiles.size() == 0) {
                Repository repository = getRepository(i_RepoName);
                Branch branch = repository.getBranches().get(i_BranchName);
                repository.getHeadBranch().setIsHead(false);
                branch.setIsHead(true);
                repository.setHeadBranch(branch);

                cleanWc();
                factory.createWc(i_RepoName, branch);
            } else {
                throw new OpenChangesInWcException(wcStatus);
            }
        } else {
            throw new Exception("Checkout is illigal on remote branches.");
        }
    }

    public void CreateFolderFromCommit(Commit i_Commit, String i_Location) {
        factory.createFolderFromCommit(i_Commit, i_Location);
    }

    private void cleanWc() throws IOException {
        File rootFolder = new File(activeRepositoryPath);
        File[] filesInRootFolder = rootFolder.listFiles();

        if(filesInRootFolder != null) {
            List<File> filesInRootFolderList = Arrays.stream(filesInRootFolder).filter(f -> !f.getName().contains(".magit")).collect(Collectors.toList());

            for (File file : filesInRootFolderList) {
                if (file.isDirectory()) {
                    FileUtils.deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        throw new IOException();
                    }
                }
            }
        }
    }

    public void DeleteFile(IRepositoryFile i_File, String i_FullPath, boolean i_IsCreateNew) {
        if(i_File instanceof Folder || new File(i_FullPath).isDirectory()) {
            try {
                FileUtils.deleteDirectory(new File(i_FullPath));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(i_IsCreateNew) {
                CreateNewFileOnSystem(i_File, i_FullPath);
            }
        }
        else {
            new File(i_FullPath).delete();

            if(i_IsCreateNew) {
                FileUtilities.WriteToFile(i_FullPath, ((Blob) i_File).getText());
            }
        }
    }

    public List<String> showActiveBranchHistory() {
        List<String> history = new ArrayList<>();
        Branch activeBranch = repositories.get(activeRepositoryName).getHeadBranch();
        Commit commit = repositories.get(activeRepositoryName).getCommits().get(activeBranch.getPointedCommitSha1());

        ShowActiveBranchHistoryRec(commit, activeBranch.getPointedCommitSha1(), history);

        return history;
    }

    private void ShowActiveBranchHistoryRec(Commit i_Commit, String i_Sha1, List<String> i_History) {
        if(i_Commit == null) {
            return;
        }
        else {
            String precedingCommitSha1 = i_Commit.getFirstPrecedingSha1();
            ShowActiveBranchHistoryRec(repositories.get(activeRepositoryName).getCommits().get(precedingCommitSha1), precedingCommitSha1, i_History);

            StringBuilder sb = new StringBuilder();
            sb.append(i_Sha1);
            sb.append(";");
            sb.append(i_Commit.getMessage());
            sb.append(";");
            sb.append(i_Commit.getLastUpdate());
            sb.append(";");
            sb.append(i_Commit.getLastChanger());

            i_History.add(sb.toString());
        }
    }

    public Repository getActiveRepository() {
        return repositories == null ? null : repositories.get(activeRepositoryName);
    }

    public String getRepositoryPath(String i_RepoName) {
        String location = null;

        if(repositories != null && i_RepoName != null && !i_RepoName.isEmpty()) {
            Repository repository = repositories.get(i_RepoName);

            if(repository != null) {
                location = repository.getLocationPath();
            }
        }

        return location;
    }

    public void setRepositoryPath(String i_RepositoryPath) {
        activeRepositoryPath = i_RepositoryPath == null ? null : Paths.get(i_RepositoryPath).toString().toLowerCase();
    }

    public void setActiveRepository(Repository i_Repository) {
        activeRepositoryName = i_Repository.getName();
        activeRepositoryPath = i_Repository.getLocationPath();
        
        if(!repositories.containsKey(i_Repository.getName())) {
            repositories.put(activeRepositoryName, i_Repository);
        }
    }

    public void setCurrentUserName(String i_CurrentUserName) {
        currentNameProperty.set(i_CurrentUserName);
    }

    public String getCurrentUserName() {
        return currentNameProperty.get();
    }

    public void createRepositoryAndFiles(String i_RepositoryName, String i_RepositoryLocation) throws RepositoryAlreadyExistsException, FolderInLocationAlreadyExistsException {
        this.Clear();
        File repositoryFolder = new File(i_RepositoryLocation);
        File magit            = new File(repositoryFolder, ".magit");

        if(magit.exists()) {
            throw new RepositoryAlreadyExistsException();
        }
        else if(repositoryFolder.exists() && repositoryFolder.listFiles() != null && repositoryFolder.listFiles().length != 0) {
            throw new FolderInLocationAlreadyExistsException();
        }
        else {
            loadedProperty.set(false);
            factory.createRepositoryAndFiles(i_RepositoryName, i_RepositoryLocation, true);
            loadedProperty.set(true);
        }
    }

    public void resetHeadBranch(String i_RepoName, String i_PointedCommitSha1) throws IOException, Sha1LengthException {
        if(i_PointedCommitSha1.length() != 40) {
            throw new Sha1LengthException();
        }

        Repository repository = getRepository(i_RepoName);

        if(!repositories.get(activeRepositoryName).getCommits().containsKey(i_PointedCommitSha1)) {
            loadCommitFile(i_RepoName, i_PointedCommitSha1);
        }

        FileUtilities.WriteToFile(Paths.get(activeRepositoryPath,
                ".magit", "branches", repositories.get(activeRepositoryName).getHeadBranch().getName() + ".txt").toString(), i_PointedCommitSha1);
        repositories.get(activeRepositoryName).getHeadBranch().setPointedCommitSha1(i_PointedCommitSha1);

        cleanWc();
        factory.createWc(i_RepoName, repositories.get(activeRepositoryName).getHeadBranch());
    }

    private void loadBranch(String i_RepoName, String i_BranchPath) throws IOException {
        File branchFile = new File(i_BranchPath);
        Branch branch = Branch.parse(branchFile);
        getRepository(i_RepoName).getBranches().put(branch.getName(), branch);

        if(branch.getPointedCommitSha1() != null && !branch.getPointedCommitSha1().isEmpty()) {
            loadCommitFile(i_RepoName, branch.getPointedCommitSha1());
        }
    }

    private void loadCommitFile(String i_RepoName, String i_CommitSha1) throws IOException {
        String objectsFolderPath = Paths.get(this.getRepositoryPath(i_RepoName), ".magit", "objects").toString();
        Commit commit = Commit.parse(new File(Paths.get(objectsFolderPath, i_CommitSha1).toString()));
        Repository repository = getRepository(i_RepoName);

        if(!repository.getCommits().containsKey(i_CommitSha1)) {
            repository.getCommits().put(i_CommitSha1, commit);

            if (!commit.getFirstPrecedingSha1().isEmpty()) {
                boolean isFirstPrecedingExists = repository.getCommits().containsKey(commit.getFirstPrecedingSha1());

                if(!isFirstPrecedingExists) {
                    loadCommitFile(i_RepoName, commit.getFirstPrecedingSha1());
                }

                if (!commit.getSecondPrecedingSha1().isEmpty()) {
                    boolean isSecondPrecedingExists = repository.getCommits().containsKey(commit.getSecondPrecedingSha1());

                    if(!isSecondPrecedingExists) {
                        loadCommitFile(i_RepoName, commit.getSecondPrecedingSha1());
                    }
                }
            }
        }

        if(commit.getRootFolderSha1() != null && !commit.getRootFolderSha1().isEmpty()) {
            loadFolderFile(i_RepoName, commit.getRootFolderSha1());
            repository.getFolders().get(commit.getRootFolderSha1()).setIsRoot(true);
        }
    }

    private void loadFolderFile(String i_RepoName, String i_FolderSha1) throws IOException {
        String objectsFolderPath = Paths.get(this.getRepositoryPath(i_RepoName), ".magit", "objects").toString();
        Folder folder = Folder.parse(new File(Paths.get(objectsFolderPath, i_FolderSha1).toString()));
        Repository repository = getRepository(i_RepoName);

        repository.getFolders().put(i_FolderSha1, folder);

        for(Folder.Data item: folder.getFiles()) {
            if(item.getSHA1() != null && !item.getSHA1().isEmpty()) {
                if (item.getFileType().equals(eFileType.FOLDER)) {
                    loadFolderFile(i_RepoName, item.getSHA1());
                } else {
                    loadBlobFile(i_RepoName, item.getSHA1());
                }
            }
        }
    }

    private void loadBlobFile(String i_RepoName, String i_BlobSHA1) throws IOException {
        String objectsFolderPath = Paths.get(this.getRepositoryPath(i_RepoName), ".magit", "objects").toString();
        Blob blob = Blob.parse(new File(Paths.get(objectsFolderPath, i_BlobSHA1).toString()));
        getRepository(i_RepoName).getBlobs().put(i_BlobSHA1, blob);
    }

    public void exportRepositoryToXml(String i_RepoName, String i_XmlPath) throws xmlErrorsException, RepositoryNotLoadedException {
        Path xmlPath;

        try {
            xmlPath = Paths.get(i_XmlPath);
        } catch (InvalidPathException ipe) {
            throw new xmlErrorsException("Input is not a path.");
        }

        XmlHelper xmlHelper = new XmlHelper(this, xmlPath);

        if(xmlHelper.IsValidXmlPath()) {
            Repository repository = getRepository(i_RepoName);

            if(repository != null) {
                MagitRepository magitRepository = new MagitRepository();
                magitRepository.setLocation(repository.getLocationPath());
                magitRepository.setName(repository.getName());

                if (repository.getBranches().size() != 0) {
                    createMagitBranches(magitRepository);
                }

                if (repository.getCommits().size() != 0) {
                    createMagitCommits(magitRepository);
                }

                xmlHelper.MagitRepositoryToXml(magitRepository);
            }
            else {
                throw new RepositoryNotLoadedException();
            }
        }
        else {
            throw new xmlErrorsException("Xml file should end with \".xml\".");
        }
    }

    private void createMagitBranches(MagitRepository i_MagitRepository) {
        Map<String, Branch> branches = repositories.get(activeRepositoryName).getBranches();
        i_MagitRepository.setMagitBranches(new MagitBranches());
        List<MagitSingleBranch> magitBranches = i_MagitRepository.getMagitBranches().getMagitSingleBranch();

        for(Map.Entry<String, Branch> branchEntry: branches.entrySet()) {
            MagitSingleBranch magitBranch = new MagitSingleBranch();
            MagitSingleBranch.PointedCommit pointedCommit = new MagitSingleBranch.PointedCommit();

            pointedCommit.setId(branchEntry.getValue().getPointedCommitSha1());
            magitBranch.setPointedCommit(pointedCommit);
            magitBranch.setTrackingAfter(branchEntry.getValue().getTrakingAfter());
            magitBranch.setTracking(branchEntry.getValue().isTracking());
            magitBranch.setName(branchEntry.getValue().getName());
            magitBranch.setIsRemote(branchEntry.getValue().isRemote());

            if(branchEntry.getValue().isHead()) {
                i_MagitRepository.getMagitBranches().setHead(magitBranch.getName());
            }

            magitBranches.add(magitBranch);
        }
    }

    private void createMagitCommits(MagitRepository i_MagitRepository) {
        Map<String, Commit> commits = repositories.get(activeRepositoryName).getCommits();

        i_MagitRepository.setMagitCommits(new MagitCommits());
        i_MagitRepository.setMagitFolders(new MagitFolders());
        i_MagitRepository.setMagitBlobs(new MagitBlobs());
        Set<String> sha1TrackerSet = new HashSet<>();

        List<MagitSingleCommit> magitCommits = i_MagitRepository.getMagitCommits().getMagitSingleCommit();
        Set<String> rootFoldersTracker = new HashSet<>();

        for(Map.Entry<String, Commit> commitEntry: commits.entrySet()) {
            if(commitEntry.getKey().length() == 40) {
                MagitSingleCommit magitCommit = new MagitSingleCommit();
                PrecedingCommits precedingCommits = new PrecedingCommits();
                List<PrecedingCommits.PrecedingCommit> magitPrecedingCommits = precedingCommits.getPrecedingCommit();

                for(String precedingCommitSha1: commitEntry.getValue().getPrecedingCommits()) {
                    PrecedingCommits.PrecedingCommit magitPrecedingCommit = new PrecedingCommits.PrecedingCommit();
                    magitPrecedingCommit.setId(precedingCommitSha1);
                    magitPrecedingCommits.add(magitPrecedingCommit);
                }

                RootFolder magitRootFolder = new RootFolder();
                magitRootFolder.setId(commitEntry.getValue().getRootFolderSha1());
                magitCommit.setRootFolder(magitRootFolder);

                magitCommit.setPrecedingCommits(precedingCommits);
                magitCommit.setMessage(commitEntry.getValue().getMessage());
                magitCommit.setId(commitEntry.getKey());
                magitCommit.setDateOfCreation(commitEntry.getValue().getLastUpdate());
                magitCommit.setAuthor(commitEntry.getValue().getLastChanger());

                magitCommits.add(magitCommit);

                //////////////////////////////////////////////////////////////////////////////////

                if(!rootFoldersTracker.contains(commitEntry.getValue().getRootFolderSha1())) {
                    rootFoldersTracker.add(commitEntry.getValue().getRootFolderSha1());
                    MagitSingleFolder magitFolder = new MagitSingleFolder();
                    magitFolder.setName(null);
                    magitFolder.setLastUpdater(commitEntry.getValue().getLastChanger());
                    magitFolder.setLastUpdateDate(commitEntry.getValue().getLastUpdate());
                    magitFolder.setIsRoot(true);
                    magitFolder.setId(commitEntry.getValue().getRootFolderSha1());

                    createMagitFolders(i_MagitRepository, magitFolder, sha1TrackerSet);
                }
            }
        }
    }

    private void createMagitFolders(MagitRepository i_MagitRepository, MagitSingleFolder i_MagitFolder, Set<String> i_Sha1TrackerSet) {
        List<MagitSingleFolder> magitFolders = i_MagitRepository.getMagitFolders().getMagitSingleFolder();
        magitFolders.add(i_MagitFolder);
        i_Sha1TrackerSet.add(i_MagitFolder.getId());

        MagitSingleFolder.Items items = new MagitSingleFolder.Items();
        List<Item> itemsList = items.getItem();
        i_MagitFolder.setItems(items);

        Folder folder = repositories.get(activeRepositoryName).getFolders().get(i_MagitFolder.getId());

        for(Folder.Data itemData: folder.getFiles()) {
            Item item = new Item();
            item.setType(itemData.getFileType().toString().toLowerCase());
            item.setId(itemData.getSHA1());
            itemsList.add(item);

            if(!i_Sha1TrackerSet.contains(itemData.getSHA1())) {
                if (itemData.getFileType().equals(eFileType.FOLDER)) {
                    MagitSingleFolder magitSubFolder = new MagitSingleFolder();
                    magitSubFolder.setId(itemData.getSHA1());
                    magitSubFolder.setIsRoot(false);
                    magitSubFolder.setLastUpdateDate(itemData.getlastUpdate());
                    magitSubFolder.setLastUpdater(itemData.getLastChanger());
                    magitSubFolder.setName(itemData.getName());

                    createMagitFolders(i_MagitRepository, magitSubFolder, i_Sha1TrackerSet);
                }
                else {
                    MagitBlob magitBlob = new MagitBlob();
                    magitBlob.setName(itemData.getName());
                    magitBlob.setLastUpdater(itemData.getLastChanger());
                    magitBlob.setLastUpdateDate(itemData.getlastUpdate());
                    magitBlob.setId(itemData.getSHA1());

                    createMagitBlob(i_MagitRepository, magitBlob, i_Sha1TrackerSet);
                }
            }
        }
    }

    private void createMagitBlob(MagitRepository i_MagitRepository, MagitBlob i_MagitBlob, Set<String> i_Sha1TrackerSet) {
        List<MagitBlob> magitBlobs = i_MagitRepository.getMagitBlobs().getMagitBlob();
        String blobContent = repositories.get(activeRepositoryName).getBlobs().get(i_MagitBlob.getId()).getText();
        i_MagitBlob.setContent(blobContent);
        magitBlobs.add(i_MagitBlob);
        i_Sha1TrackerSet.add(i_MagitBlob.getId());
    }

    public void Clear() {
        activeRepositoryName = null;
        activeRepositoryPath = null;
        currentNameProperty.set("Administrator");
        factory.clear();
    }

    public CommitNode BuildTree(Map<String, CommitNode> i_TreeNodes, Branch i_BranchToAdd) {
        Commit pointedCommit = repositories.get(activeRepositoryName).getCommits().get(i_BranchToAdd.getPointedCommitSha1());
        return pointedCommit == null ? null : buildTreeRec(pointedCommit, i_BranchToAdd, i_BranchToAdd, i_TreeNodes);
    }

    private CommitNode buildTreeRec(Commit i_CommitToNode, Branch i_PointingBranch, Branch i_OnBranch, Map<String, CommitNode> i_TreeNodes) {
        CommitNode node = null;
        String firstPrecedingSha1 = i_CommitToNode.getFirstPrecedingSha1();
        String secondPrecedingSha1 = i_CommitToNode.getSecondPrecedingSha1();

        if(!firstPrecedingSha1.isEmpty() && !i_TreeNodes.containsKey(i_CommitToNode.getSha1())) {
            Commit firstPreceding = repositories.get(activeRepositoryName).getCommits().get(firstPrecedingSha1);
            CommitNode firstPrecedingNode = buildTreeRec(firstPreceding, null, i_OnBranch, i_TreeNodes);
            CommitNode secondPrecedingNode = null;

            if(!secondPrecedingSha1.isEmpty()) {
                Commit secondPreceding = repositories.get(activeRepositoryName).getCommits().get(secondPrecedingSha1);
                secondPrecedingNode = buildTreeRec(secondPreceding, null, i_OnBranch, i_TreeNodes);
            }

            node = createCommitNode(i_CommitToNode, firstPrecedingNode, secondPrecedingNode, i_PointingBranch);
            i_TreeNodes.put(node.getSha1(), node);
            firstPrecedingNode.addChildren(node);

            if(secondPrecedingNode != null) {
                secondPrecedingNode.addChildren(node);
            }
        }

        if(node == null && i_TreeNodes.containsKey(i_CommitToNode.getSha1())) {
            node = i_TreeNodes.get(i_CommitToNode.getSha1());

            if(i_PointingBranch != null) {
                node.getPointingBranches().add(i_PointingBranch);
            }

            setOnBranch(node.getFirstParent(), i_OnBranch);
            setOnBranch(node.getSecondParent(), i_OnBranch);
        }
        else if(node == null) {
            node = createCommitNode(i_CommitToNode, null, null, i_PointingBranch);
            i_TreeNodes.put(node.getSha1(), node);
        }

        node.getOnBranches().add(i_OnBranch);

        return node;
    }

    private void setOnBranch(CommitNode i_Node, Branch i_OnBranch) {
        if(i_Node == null) {
            return;
        }

        i_Node.getOnBranches().add(i_OnBranch);
        setOnBranch(i_Node.getFirstParent(), i_OnBranch);
        setOnBranch(i_Node.getSecondParent(), i_OnBranch);
    }

    private CommitNode createCommitNode(Commit i_CommitToNode, CommitNode i_FirstPrecedingNode, CommitNode i_SecondPrecedingNode, Branch i_PointingBranch) {
        CommitNode result = new CommitNode(i_FirstPrecedingNode, i_SecondPrecedingNode);
        result.setCommit(i_CommitToNode);

        if(i_PointingBranch != null) {
            result.getPointingBranches().add(i_PointingBranch);
        }

        return result;
    }

    public CommitNode FindRoot(CommitNode i_Leaf) {
        if(i_Leaf == null || i_Leaf.getFirstParent() == null) {
            return i_Leaf;
        }

        return FindRoot(i_Leaf.getFirstParent());
    }

    public void Clone(String i_LocalRepositoryName, String i_LocalRepositoryFullPath, String i_RemoteRepositoryFullPath) throws IOException, CollaborationException {
        if(!new File(i_LocalRepositoryFullPath).exists()) {
            String remoteRepositoryLocation = Paths.get(i_RemoteRepositoryFullPath).toString().toLowerCase();
            File remoteRepoFile = new File(i_RemoteRepositoryFullPath);

            if(remoteRepoFile.exists()) {
                if (Arrays.stream(Objects.requireNonNull(remoteRepoFile.listFiles()))
                        .anyMatch(f -> f.getName().contains("magit"))) {
                    File localRepoFile = new File(i_LocalRepositoryFullPath);
                    File branchesDirectory = new File(Paths.get(i_LocalRepositoryFullPath, ".magit", "branches").toString());

                    FileUtils.copyDirectory(remoteRepoFile, localRepoFile);
                    FileUtilities.WriteToFile(Paths.get(i_LocalRepositoryFullPath, ".magit", "details.txt").toString(), String.format(
                            "%s%s%s", i_LocalRepositoryName, System.lineSeparator(), remoteRepositoryLocation
                    ));

                    File[] branchesFiles = branchesDirectory.listFiles();
                    File remoteBranchesDirectory = new File(Paths.get(branchesDirectory.toPath().toString(),
                            remoteRepoFile.getName()).toString());
                    remoteBranchesDirectory.mkdir();

                    String headBranchName = FileUtilities.ReadTextFromFile(Paths.get(branchesDirectory.toPath().toString(), "head.txt").toString());

                    for (File branchFile : branchesFiles) {
                        if (!branchFile.getName().contains("head") && !branchFile.getName().contains(headBranchName)) {
                            FileUtils.copyFile(branchFile, new File(Paths.get(remoteBranchesDirectory.toPath().toString(), branchFile.getName()).toString()));
                            branchFile.delete();
                        }

                        if (branchFile.getName().contains(headBranchName)) {
                            FileUtils.copyFile(branchFile, new File(Paths.get(remoteBranchesDirectory.toPath().toString(), branchFile.getName()).toString()));
                        }
                    }

                    loadedProperty.set(false);
                    loadDataFromRepository(i_LocalRepositoryFullPath);
                    getRepository(i_LocalRepositoryName).setRemoteRepositoryLocation(remoteRepositoryLocation);
                    loadedProperty.set(true);
                } else {
                    throw new CollaborationException("Remote directory is not a magit repository.");
                }
            } else {
                throw new CollaborationException("Remote directory not exists.");
            }
        } else {
            throw new CollaborationException("Directory with the same name already exists in the given location.");
        }
    }

    public void Fetch(String i_RepoName) {
        String repoPath = getRepositoryPath(i_RepoName);
        String remoteRepositoryLocation = getRemoteRepositoryLocation(i_RepoName);
        String remoteBranchesDirPath = Paths.get(remoteRepositoryLocation, ".magit", "branches").toString();
        String remoteObjectsDirPath = Paths.get(remoteRepositoryLocation, ".magit", "objects").toString();
        String localRemoteBranchesDirPath = Paths.get(repoPath, ".magit", "branches", new File(remoteRepositoryLocation).getName()).toString();
        String localObjectsDirPath = Paths.get(repoPath, ".magit", "objects").toString();

        try {
            FileUtils.copyDirectory(new File(remoteBranchesDirPath), new File(localRemoteBranchesDirPath));
            FileUtils.copyDirectory(new File(remoteObjectsDirPath), new File(localObjectsDirPath));
            loadDataFromRepository(repoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Push(String i_RepoName, String i_RemoteLocation) throws CollaborationException, IOException {
        Repository repository = getRepository(i_RepoName);

        if(repository.getHeadBranch().isTracking()) {
            String remoteRepositoryLocation = getRemoteRepositoryLocation(i_RepoName);

            if(isPushNeeded(i_RepoName, i_RemoteLocation)) {
                String localRepoPath = repository.getLocationPath();
                String localHeadBranchName = repository.getHeadBranch().getName();

                loadDataFromRepository(remoteRepositoryLocation);
                boolean isRemoteWcClean = isWcClean(i_RepoName);

                if (isRemoteWcClean) {
                    String remotePointedCommitSha1 = repository.getBranches().get(localHeadBranchName).getPointedCommitSha1();
                    loadDataFromRepository(localRepoPath);
                    Branch trackedBranch = repository.getBranches().get(repository.getHeadBranch().getTrakingAfter());
                    String trackedBranchPointedCommitSha1 = trackedBranch.getPointedCommitSha1();

                    if (remotePointedCommitSha1.equals(trackedBranchPointedCommitSha1)) {
                        String headBranchPointedCommitSha1 = repository.getHeadBranch().getPointedCommitSha1();

                        String remoteBranchPath = Paths.get(remoteRepositoryLocation,
                                ".magit", "branches", localHeadBranchName + ".txt").toString();
                        String trackedBranchPath = Paths.get(repository.getLocationPath(),
                                ".magit", "branches", new File(remoteRepositoryLocation).getName(),
                                localHeadBranchName + ".txt").toString();

                        FileUtilities.WriteToFile(remoteBranchPath, headBranchPointedCommitSha1);
                        FileUtilities.WriteToFile(trackedBranchPath, headBranchPointedCommitSha1);
                        trackedBranch.setPointedCommitSha1(headBranchPointedCommitSha1);

                        copyCommitsFilesFromBranch(repository.getHeadBranch(), remoteRepositoryLocation, repository.getLocationPath());
                        copyWcToRemote(i_RepoName);

                    } else {
                        throw new CollaborationException("Remote branch is not pointing to the same commit as local branch.");
                    }

                } else {
                    throw new CollaborationException("Remote repository working directory is not clean.");
                }
            }
        } else {
            throw new CollaborationException("Head branch is not a remote tracking branch.");
        }
    }

    private void copyWcToRemote(String i_RepoName) throws IOException {
        String remoteRepositoryLocation = getRemoteRepositoryLocation(i_RepoName);
        File remoteRepo = new File(remoteRepositoryLocation);
        File localRepo = new File(activeRepositoryPath);

        File[] filesInRemote = remoteRepo.listFiles();
        File[] filesInLocal = localRepo.listFiles();

        if(filesInRemote != null) {
            List<File> filterdFilesInRemote = Arrays.stream(filesInRemote).filter(f -> !f.getName().contains(".magit")).collect(Collectors.toList());

            for(File file: filesInRemote) {
                file.delete();
            }
        }

        if(filesInLocal != null) {
            List<File> filterdFilesInRemote = Arrays.stream(filesInLocal).filter(f -> !f.getName().contains(".magit")).collect(Collectors.toList());

            for(File file: filterdFilesInRemote) {
                if(file.isDirectory()) {
                    FileUtils.copyDirectory(file, new File(Paths.get(remoteRepositoryLocation, file.getName()).toString()));
                }
                else {
                    FileUtils.copyFile(file, new File(replaceRootPath(file.toPath().toString(), remoteRepositoryLocation)));
                }
            }
        }
    }

    private void copyCommitsFilesFromBranch(Branch i_Branch, String i_Dest, String i_Src) throws IOException {
        copyCommitFile(i_Branch.getPointedCommitSha1(), i_Dest, i_Src);
    }

    private void copyCommitFile(String i_CommitSha1, String i_Dest, String i_Src) throws IOException {
        if(i_CommitSha1.isEmpty()) {
            return;
        }

        String srcContent = copyFile(i_CommitSha1, i_Dest, i_Src);

        String[] details = srcContent.split(";");

        copyFolderFile(details[0], i_Dest, i_Src);
        copyCommitFile(details[1], i_Dest, i_Src);
        copyCommitFile(details[2], i_Dest, i_Src);
    }

    private void copyFolderFile(String i_FolderSha1, String i_Dest, String i_Src) throws IOException {
        String srcContent = copyFile(i_FolderSha1, i_Dest, i_Src);
        List<String> filesData = StringUtilities.getLines(srcContent);

        for(String data: filesData) {
            String[] dataParts = data.split(";");

            if(dataParts.length >= 3 && dataParts[2].equals("folder")) {
                copyFolderFile(dataParts[1], i_Dest, i_Src);
            }
            else {
                copyFile(dataParts[1], i_Dest, i_Src);
            }
        }
    }

    private String copyFile(String i_Sha1, String i_Dest, String i_Src) throws IOException {
        String srcFilePath = Paths.get(i_Src, ".magit", "objects", i_Sha1).toString();
        String destFilePath = Paths.get(i_Dest, ".magit", "objects", i_Sha1).toString();

        String srcContent = FileUtilities.UnzipFile(srcFilePath);
        FileUtilities.ZipFile(i_Sha1, srcContent, destFilePath);

        return srcContent;
    }

    public void Pull(String i_RepoName, String i_RemoteLocation) throws OpenChangesInWcException, CollaborationException, Exception {
        Repository repository = getRepository(i_RepoName);
        Branch localHeadBranch = repository.getHeadBranch();

        if(localHeadBranch.isTracking()) {
            if(isWcClean(i_RepoName)) {
                if(!isPushNeeded(i_RepoName, i_RemoteLocation)) {
                    String remoteHeadBranchPath = Paths.get(i_RemoteLocation, ".magit", "branches", localHeadBranch.getName() + ".txt").toString();
                    String localHeadBranchPath = Paths.get(repository.getLocationPath(), ".magit", "branches", localHeadBranch.getName() + ".txt").toString();
                    String trackedBranchByHeadBranch = Paths.get(repository.getLocationPath(), ".magit", "branches", new File(i_RemoteLocation).getName(), localHeadBranch.getName() + ".txt").toString();

                    try {
                        String remotePointedCommit = FileUtilities.ReadTextFromFile(remoteHeadBranchPath);
                        FileUtilities.WriteToFile(localHeadBranchPath, remotePointedCommit);
                        FileUtilities.WriteToFile(trackedBranchByHeadBranch, remotePointedCommit);

                        Branch temp = new Branch();
                        temp.setPointedCommitSha1(FileUtilities.ReadTextFromFile(remoteHeadBranchPath));

                        this.copyCommitsFilesFromBranch(temp, repository.getLocationPath(), i_RemoteLocation);
                        this.loadDataFromRepository(repository.getLocationPath());
                        getActiveRepository().setRemoteRepositoryLocation(repository.getRemoteRepositoryLocation());
                        getActiveRepository().setForked(repository.isForked());
                        getActiveRepository().setUsernameForkedFrom(repository.getUsernameForkedFrom());
                        getActiveRepository().setOwner(repository.getOwner());
                        setCurrentUserName(repository.getOwner());
                        this.checkout(i_RepoName, repository.getHeadBranch().getName(), true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    throw new CollaborationException("Push operation is necessary before pull execution.");
                }
            }
            else {
                throw new OpenChangesInWcException(null);
            }
        }
        else {
            throw new CollaborationException("Head branch is not remote tracking branch.");
        }
    }

    private boolean isPushNeeded(String i_RepoName, String i_RemoteLocation) {
        String currentCommitSha1 = repositories.get(activeRepositoryName).getHeadBranch().getPointedCommitSha1();
        String commitFilePathInRemote = Paths.get(i_RemoteLocation, ".magit", "objects", currentCommitSha1).toString();
        File commitFileInRemote = new File(commitFilePathInRemote);

        return !commitFileInRemote.exists();
    }

    public boolean isWcClean(String i_RepoName) {
        List<List<String>> wcStatus = getWorkingCopyDelta(i_RepoName);

        List<String> deletedFiles = wcStatus.get(0);
        List<String> newFiles = wcStatus.get(1);
        List<String> changedFiles = wcStatus.get(2);

        return deletedFiles.size() == 0 && newFiles.size() == 0 && changedFiles.size() == 0;
    }

    public Repository getRepository(String i_Name) {
        Repository repository = null;

        if(repositories != null) {
            repository = repositories.get(i_Name);
        }
        return repository;
    }

    public String getRemoteRepositoryLocation(String i_RepoName) { return repositories.get(i_RepoName).getRemoteRepositoryLocation(); }

    public static class Creator {
        private static Engine m_Instance = null;
        private static final Object m_Lock = new Object();

        private Creator() {
        }

        public static Engine getInstance() {
            if (m_Instance == null) {
                synchronized (m_Lock) {
                    if (m_Instance == null) {
                        try {
                            Constructor[] constructors = Engine.class.getDeclaredConstructors();
                            for (Constructor constructor : constructors) {
                                if (!constructor.isAccessible()) {
                                    constructor.setAccessible(true);
                                    m_Instance = (Engine) constructor.newInstance();
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return m_Instance;
        }
    }
}

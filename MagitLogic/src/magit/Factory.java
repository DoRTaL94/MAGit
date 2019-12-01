package magit;

import IO.FileUtilities;
import data.structures.*;
import org.apache.commons.codec.digest.DigestUtils;
import resources.jaxb.schema.generated.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Factory {
    private final Map<String, Blob> tmpBlobs = new HashMap<>();
    private final Map<String, Folder> tmpFolders = new HashMap<>();
    private final Engine engine;

    public Factory(Engine i_Engine) {
        engine = i_Engine;
    }

    Repository createRepository(MagitRepository i_MagitRepository) {
        boolean isEmptyRepo = i_MagitRepository.getMagitCommits().getMagitSingleCommit().size() == 0;
        Repository repository = createRepositoryAndFiles(i_MagitRepository.getName(), i_MagitRepository.getLocation(), isEmptyRepo);

        if(!isEmptyRepo) {
            // Set lists of magit data structures.
            List<MagitSingleCommit> magitCommits = i_MagitRepository.getMagitCommits().getMagitSingleCommit();
            List<MagitSingleBranch> magitBranches = i_MagitRepository.getMagitBranches().getMagitSingleBranch();
            List<MagitBlob> magitBlobs = i_MagitRepository.getMagitBlobs().getMagitBlob();
            List<MagitSingleFolder> magitFolders = i_MagitRepository.getMagitFolders().getMagitSingleFolder();

            // Transform magit lists to maps and set maps in repository.
            repository.setBranches(generateBranchesHashMap(magitBranches));
            repository.setCommits(generateCommitsHashMap(magitCommits));
            repository.setFolders(generateFoldersHashMap(magitFolders, generateMagitBlobsHashMap(magitBlobs)));
            repository.setBlobs(generateBlobsHashMap(magitBlobs));

            try {
                String headBranchName = i_MagitRepository.getMagitBranches().getHead();
                createFilesOnSystem(repository.getName() ,headBranchName);
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }

        return repository;
    }

    Repository createRepositoryAndFiles(String i_RepositoryName, String i_RepositoryLocation, boolean i_IsEmptyRepo) {
        Repository repository = new Repository();
        repository.setName(i_RepositoryName);

        File repositoryFolder = new File(i_RepositoryLocation);
        File magit            = new File(repositoryFolder, ".magit");
        File objects          = new File(magit, "objects");
        File Branches         = new File(magit, "branches");

        repositoryFolder.mkdir();
        magit.mkdir();
        objects.mkdir();
        Branches.mkdir();

        repository.setLocationPath(repositoryFolder.getAbsolutePath());
        engine.setRepositoryPath(repositoryFolder.getAbsolutePath());
        engine.setActiveRepository(repository);
        FileUtilities.WriteToFile(Paths.get(repository.getLocationPath(),
                ".magit", "details.txt").toString(), String.format("%s%s%s", i_RepositoryName, System.lineSeparator(), engine.getRemoteRepositoryLocation(i_RepositoryName)));

        if(i_IsEmptyRepo) {
            repository.setBlobs(new HashMap<>());
            repository.setFolders(new HashMap<>());
            repository.setCommits(new HashMap<>());
            repository.setBranches(new HashMap<>());

            Branch masterBranch = new Branch();
            masterBranch.setIsHead(true);
            masterBranch.setName("master");
            repository.getBranches().put("master", masterBranch);

            String headBranchPath = Paths.get(i_RepositoryLocation, ".magit", "branches", "head.txt").toString();
            String masterBranchPath = Paths.get(i_RepositoryLocation, ".magit", "branches", "master.txt").toString();
            FileUtilities.WriteToFile(masterBranchPath, "");
            FileUtilities.WriteToFile(headBranchPath, "master");

            repository.setHeadBranch(masterBranch);
        }

        return repository;
    }

    private void createFilesOnSystem(String i_RepoName, String i_HeadBranchName) throws IOException, ParseException {
        Repository repository = engine.getActiveRepository();
        Map<String, Branch> branches = engine.getActiveRepository().getBranches();
        Map<String, Commit> commits  = engine.getActiveRepository().getCommits();
        Set<String> sha1Set = new HashSet<>();

        for(Map.Entry<String, Branch> branch: branches.entrySet()) {
            Branch newBranch = new Branch();
            String pointedCommitSha1 = branch.getValue().getPointedCommitSha1();
            String branchPath = Paths.get(repository.getLocationPath(),
                    ".magit", "branches", branch.getValue().getName().concat(".txt")).toString();
            Commit commit = commits.get(pointedCommitSha1);

            if(!sha1Set.contains(pointedCommitSha1)) {
                pointedCommitSha1 = spreadCommitAndSetNewSha1s(i_RepoName, commit, sha1Set);
            }

            if(branch.getValue().getName().contains("/") ||
                    branch.getValue().getName().contains("\\")) {
                String[] parts = branch.getValue().getName().split("/");
                String remoteRepoName;

                if(parts.length == 1) {
                    parts = branch.getValue().getName().split(Pattern.quote("\\"));
                }

                remoteRepoName = parts[0];
                String remoteRepoFolderPath = Paths.get(repository.getLocationPath(),
                        ".magit", "branches", remoteRepoName).toString();
                new File(remoteRepoFolderPath).mkdir();
            }

            FileUtilities.WriteToFile(branchPath, pointedCommitSha1);

            if(branch.getValue().getName().equals(i_HeadBranchName)) {
                newBranch.setIsHead(true);
                engine.getActiveRepository().setHeadBranch(newBranch);
            }

            newBranch.setPointedCommitSha1(pointedCommitSha1);
            newBranch.setName(branch.getValue().getName());
            branches.put(newBranch.getName(), newBranch);
        }

        createWc(i_RepoName, engine.getActiveRepository().getHeadBranch());
    }

    void createWc(String i_RepoName, Branch i_HeadBranch) throws IOException {
        String repoPath = engine.getActiveRepository().getLocationPath();
        String headBranchFilePath = Paths.get(repoPath,".magit", "branches", "head.txt").toString();
        FileUtilities.WriteToFile(headBranchFilePath, i_HeadBranch.getName());

        String pointedCommitSha1 = i_HeadBranch.getPointedCommitSha1();
        Commit currentCommit     = engine.getActiveRepository().getCommits().get(pointedCommitSha1);

        if(currentCommit != null) {
            createFolderFromCommit(currentCommit, engine.getRepositoryPath(i_RepoName));
        }
    }

    void createFolderFromCommit(Commit i_Commit, String i_Location) {
        String rootFolderSha1    = i_Commit.getRootFolderSha1();
        Folder rootFolder        = engine.getActiveRepository().getFolders().get(rootFolderSha1);

        createWcRec(rootFolder, i_Location);
    }

    void createFolder(Folder i_Folder, String i_FullPath) {
        File folderFile = new File(i_FullPath);
        folderFile.mkdir();
        createWcRec(i_Folder, i_FullPath);
    }

    private void createWcRec(Folder i_RootFolder, String i_CurrentLocation) {
        List<Folder.Data> filesInFolder = i_RootFolder.getFiles();

        for(Folder.Data file: filesInFolder) {
            if(file.getFileType().equals(eFileType.FOLDER)) {
                Folder subFolder = engine.getActiveRepository().getFolders().get(file.getSHA1());
                String subFolderLocation = Paths.get(i_CurrentLocation, file.getName()).toString();
                File folderFile = new File(subFolderLocation);
                folderFile.mkdir();

                createWcRec(subFolder, subFolderLocation);
            }
            else {
                Blob blob = engine.getActiveRepository().getBlobs().get(file.getSHA1());
                String blobPath = Paths.get(i_CurrentLocation, file.getName()).toString();
                FileUtilities.WriteToFile(blobPath, blob.getText());
            }
        }
    }

    public String spreadCommitAndSetNewSha1s(String i_RepoName, Commit i_Commit, Set<String> i_Sha1Set) throws IOException, ParseException {
        String firstPrecedingCommitSha1 = i_Commit.getFirstPrecedingSha1();
        String secondPrecedingCommitSha1 = i_Commit.getSecondPrecedingSha1();

        // Setting first preceding commit
        if(!firstPrecedingCommitSha1.isEmpty()) {
            Commit firstPrecedingCommit = engine.getRepository(i_RepoName).getCommits().get(firstPrecedingCommitSha1);

            if(firstPrecedingCommit != null) {
                firstPrecedingCommitSha1 = spreadCommitAndSetNewSha1s(i_RepoName, firstPrecedingCommit, i_Sha1Set);
                i_Commit.setFirstPrecedingCommitSha1(firstPrecedingCommitSha1);
            }

            // Setting Second preceding commit
            if(!secondPrecedingCommitSha1.isEmpty()) {
                Commit secondPrecedingCommit =  engine.getRepository(i_RepoName).getCommits().get(secondPrecedingCommitSha1);

                if(secondPrecedingCommit != null) {
                    secondPrecedingCommitSha1 = spreadCommitAndSetNewSha1s(i_RepoName, secondPrecedingCommit, i_Sha1Set);
                    i_Commit.setSecondPrecedingCommitSha1(secondPrecedingCommitSha1);
                }
            }
        }

        // Setting root folder
        String rootFolderSha1 = i_Commit.getRootFolderSha1();
        Folder rootFolder     = engine.getActiveRepository().getFolders().get(rootFolderSha1);
        String objectsPath    = Paths.get(engine.getActiveRepository().getLocationPath(), ".magit", "objects").toString();
        rootFolderSha1        = spreadCommitAndSetNewSha1sRec(rootFolder, "", i_Commit.getLastUpdate(), engine.getRepositoryPath(i_RepoName), false);

        engine.getActiveRepository().getFolders().put(rootFolderSha1, rootFolder);
        i_Commit.setRootFolderSha1(rootFolderSha1);

        // Zip and files management
        String commitSha1 = DigestUtils.sha1Hex(i_Commit.toStringForSha1());
        String zipPath = Paths.get(objectsPath, commitSha1).toString();
        FileUtilities.ZipFile(commitSha1, i_Commit.toString(), zipPath);
        i_Sha1Set.add(commitSha1);
        File rootFolderFile = new File(Paths.get(objectsPath, rootFolderSha1).toString());
        rootFolderFile.setLastModified(new SimpleDateFormat(Engine.DATE_FORMAT).parse(i_Commit.getLastUpdate()).getTime());

        engine.getActiveRepository().getCommits().put(commitSha1, i_Commit);

        return commitSha1;
    }

    private String spreadCommitAndSetNewSha1sRec(IRepositoryFile i_File, String i_FileNameInFolder, String i_LastModified, String i_CurrentPath,  boolean i_IsCreateWC) throws IOException, ParseException {
        String objectsFolderPath = Paths.get(engine.getActiveRepository().getLocationPath(), ".magit", "objects").toString();
        String Sha1;

        if(i_File instanceof Blob) {
            String blobContent = ((Blob)i_File).getText();
            Sha1               = DigestUtils.sha1Hex(((Blob)i_File).toStringForSha1());
            String zipPath     = Paths.get(objectsFolderPath, Sha1).toString();

            if(i_IsCreateWC) {
                String blobPath = Paths.get(i_CurrentPath, i_FileNameInFolder).toString();
                FileUtilities.WriteToFile(blobPath, blobContent);
                File blobFile = new File(blobPath);
                blobFile.setLastModified(new SimpleDateFormat(Engine.DATE_FORMAT).parse(i_LastModified).getTime());
            }

            if(!isSha1Exists(Sha1, objectsFolderPath)) {
                FileUtilities.ZipFile(Sha1, blobContent, zipPath);
            }
        }
        else {
            Folder folder = (Folder)i_File;
            String folderPath = Paths.get(i_CurrentPath, i_FileNameInFolder).toString();
            File folderToCreate = new File(folderPath);

            if(!i_FileNameInFolder.equals("") && i_IsCreateWC) {
                folderToCreate.mkdir();
            }

            for(Folder.Data file: folder.getFiles()) {
                IRepositoryFile item = file.getFileType().equals(eFileType.BLOB) ?
                        engine.getActiveRepository().getBlobs().get(file.getSHA1()) :
                        engine.getActiveRepository().getFolders().get(file.getSHA1());
                String itemSha1 = spreadCommitAndSetNewSha1sRec(item, file.getName(), file.getlastUpdate(), folderPath, i_IsCreateWC);

                if(file.getFileType().equals(eFileType.BLOB)) {
                    engine.getActiveRepository().getBlobs().put(itemSha1,
                            engine.getActiveRepository().getBlobs().get(file.getSHA1()));
                }
                else {
                    engine.getActiveRepository().getFolders().put(itemSha1,
                            engine.getActiveRepository().getFolders().get(file.getSHA1()));
                }

                file.setSHA1(itemSha1);
            }

            folder.getFiles().sort(Folder.Data::compare);
            Sha1 = DigestUtils.sha1Hex(folder.toStringForSha1(Paths.get(folderPath)));

            if(!isSha1Exists(Sha1, objectsFolderPath)) {
                String zipPath = Paths.get(objectsFolderPath, Sha1).toString();
                FileUtilities.ZipFile(Sha1, folder.toString(), zipPath);
            }

            if(!i_FileNameInFolder.equals("") && i_IsCreateWC) {
                folderToCreate.setLastModified(new SimpleDateFormat(Engine.DATE_FORMAT).parse(i_LastModified).getTime());
            }
        }

        return Sha1;
    }

    private boolean isSha1Exists(String i_Sha1, String i_ObjectsPath) {
        boolean isExists = false;
        File objectsDir = new File(i_ObjectsPath);
        File[] files = objectsDir.listFiles();

        if(files != null) {
            for (File file : files) {
                if (file.getName().equals(i_Sha1)) {
                    isExists = true;
                    break;
                }
            }
        }

        return isExists;
    }

    public Map<String, Blob> getTmpBlobs() {
        return tmpBlobs;
    }

    Map<String, Folder> getTmpFolders() {
        return tmpFolders;
    }

    Map<String, String> createPathToSha1Map(String i_RepoName, Branch i_Branch) {
        Map<String, String> map = new HashMap<>();
        String currentCommitSha1 = i_Branch.getPointedCommitSha1();

        if(!currentCommitSha1.isEmpty()) {
            Commit currentCommit = engine.getActiveRepository().getCommits().get(currentCommitSha1);
            map = createPathToSha1MapFromCommit(i_RepoName, currentCommit);
        }

        return map;
    }

    Map<String, String> createPathToSha1MapFromCommit(String i_RepoName, Commit i_Commit) {
        HashMap<String, String> map = new HashMap<>();
        Repository repository = engine.getRepository(i_RepoName);
        Folder rootFolder = repository.getFolders().get(i_Commit.getRootFolderSha1());
        String path = repository.getLocationPath();

        createCurrentCommitPathToSha1MapRec(i_RepoName, rootFolder, path, map);

        rootFolder.getFiles().sort(Folder.Data::compare);
        String remoteRepositoryLocation = repository.getRemoteRepositoryLocation();

        if(!remoteRepositoryLocation.isEmpty()) {
            path = remoteRepositoryLocation;
        }

        String rootFolderSha1 = DigestUtils.sha1Hex(rootFolder.toStringForSha1(Paths.get(path)));
        map.put(repository.getLocationPath(), rootFolderSha1);

        return map;
    }

    private void createCurrentCommitPathToSha1MapRec(String i_RepoName, IRepositoryFile i_Item, String i_CurrentPath, Map<String, String> i_Map) {
        if(i_Item instanceof Folder) {
            Repository repository = engine.getRepository(i_RepoName);
            List<Folder.Data> filesInFolder = ((Folder)i_Item).getFiles();

            for(Folder.Data file: filesInFolder) {
                String newPath = Paths.get(i_CurrentPath, file.getName()).toString();

                if(file.getFileType().equals(eFileType.FOLDER)) {
                    Folder subFolder = repository.getFolders().get(file.getSHA1());
                    createCurrentCommitPathToSha1MapRec(i_RepoName, subFolder, newPath, i_Map);
                    i_Map.put(newPath, file.getSHA1());
                }
                else {
                    i_Map.put(newPath, file.getSHA1());
                }
            }
        }

    }

    String createBlob(String i_Path) throws IOException {
        Blob blob = new Blob();

        blob.setText(FileUtilities.ReadTextFromFile(i_Path));
        String name = new File(i_Path).getName();
        blob.setName(name);
        String sha1 = DigestUtils.sha1Hex(blob.toStringForSha1());
        tmpBlobs.put(sha1, blob);

        return sha1;
    }

    String createFolder(String i_RepoName, String i_Path, String i_PutLastModifiedIfNew, String i_CurrentUserName) throws IOException {
        Path folderPath = Paths.get(i_Path);
        int filesCount = 0;
        Folder folder = new Folder();
        File folderFile = new File(i_Path);
        File[] filesInFolder = folderFile.listFiles();
        String sha1 = null;

        if(filesInFolder != null) {
            for (File file : filesInFolder) {
                filesCount++;
                boolean isFolder = file.isDirectory();

                if(!isFolder || file.listFiles().length != 0) {
                    if (isFolder) {
                        sha1 = createFolder(i_RepoName, file.toPath().toString(), i_PutLastModifiedIfNew, i_CurrentUserName);
                    } else {
                        sha1 = createBlob(file.toPath().toString());
                    }

                    if(sha1 == null) {
                        filesCount--;
                    } else {
                        Folder.Data fileData = Folder.Data.Parse(file, sha1, i_CurrentUserName);

                        if (i_PutLastModifiedIfNew != null) {
                            fileData.setlastUpdate(i_PutLastModifiedIfNew);
                        }

                        folder.addFile(fileData);
                    }
                }
            }

            if(filesCount > 0 || i_Path.equals(engine.getRepositoryPath(i_RepoName)) || new File(i_Path).getParent().equals(engine.getRepositoryPath(i_RepoName))) {
                if (i_Path.equals(engine.getRepositoryPath(i_RepoName))) {
                    folder.setIsRoot(true);
                }

                folder.getFiles().sort(Folder.Data::compare);

                if (engine.getRemoteRepositoryLocation(i_RepoName).isEmpty()) {
                    sha1 = DigestUtils.sha1Hex(folder.toStringForSha1(folderPath));
                } else {
                    sha1 = DigestUtils.sha1Hex(folder.toStringForSha1(Paths.get(engine.replaceRootPath(i_Path,
                            engine.getRemoteRepositoryLocation(i_RepoName)))));
                }
                tmpFolders.put(sha1, folder);
            } else {
                sha1 = null;
            }
        }

        return sha1;
    }

    private Map<String, MagitBlob> generateMagitBlobsHashMap(List<MagitBlob> i_MagitBlobs) {
        return i_MagitBlobs.stream().collect(Collectors.toMap(MagitBlob::getId, Function.identity()));
    }

    private  Map<String, Folder> generateFoldersHashMap(List<MagitSingleFolder> i_MagitFolders, Map<String, MagitBlob> i_MagitBlobsHashMap) {
        HashMap<String, MagitSingleFolder> magitFoldersHashMap = new HashMap<>();

        for(MagitSingleFolder folder : i_MagitFolders) {
            magitFoldersHashMap.put(folder.getId(), folder);
        }

        return transformMagitFolder(magitFoldersHashMap, i_MagitBlobsHashMap);
    }

    private Map<String, Folder> transformMagitFolder(HashMap<String, MagitSingleFolder> i_MagitFoldersHashMap, Map<String, MagitBlob> i_MagitBlobsHashMap) {
        HashMap<String, Folder> foldersHashMap = new HashMap<>();

        for(Map.Entry<String, MagitSingleFolder> folder : i_MagitFoldersHashMap.entrySet()) {
            if(!foldersHashMap.containsKey(folder.getValue().getId())) {
                boolean isRoot = folder.getValue().isIsRoot();
                Folder folderToPut = new Folder();
                foldersHashMap.put(folder.getValue().getId(), folderToPut);
                folderToPut.setIsRoot(isRoot);
            }

            for (Item item : folder.getValue().getItems().getItem()) {
                boolean isFolder = item.getType().equals("folder");
                Folder.Data folderData = new Folder.Data();

                folderData.setSHA1(item.getId());
                folderData.setFileType(isFolder ?
                        eFileType.FOLDER :
                        eFileType.BLOB);
                folderData.setLastChanger(isFolder ?
                        i_MagitFoldersHashMap.get(item.getId()).getLastUpdater() :
                        i_MagitBlobsHashMap.get(item.getId()).getLastUpdater());
                folderData.setlastUpdate(isFolder ?
                        i_MagitFoldersHashMap.get(item.getId()).getLastUpdateDate() :
                        i_MagitBlobsHashMap.get(item.getId()).getLastUpdateDate());
                folderData.setName(isFolder ?
                        i_MagitFoldersHashMap.get(item.getId()).getName() :
                        i_MagitBlobsHashMap.get(item.getId()).getName());

                foldersHashMap.get(folder.getValue().getId()).addFile(folderData);
            }
        }

        return foldersHashMap;
    }

    private Map<String, Blob> generateBlobsHashMap(List<MagitBlob> i_MagitBlobs) {
        return i_MagitBlobs.stream().collect(Collectors.toMap(MagitBlob::getId, Blob::parse));
    }

    private Map<String, Commit> generateCommitsHashMap(List<MagitSingleCommit> i_MagitCommits) {
        return i_MagitCommits.stream().collect(Collectors.toMap(MagitSingleCommit::getId, Commit::parse));
    }

    private Map<String, Branch> generateBranchesHashMap(List<MagitSingleBranch> i_MagitBranches) {
        return i_MagitBranches.stream().collect(Collectors.toMap(MagitSingleBranch::getName, Branch::parse));
    }

    public void clear() {
        tmpBlobs.clear();
        tmpFolders.clear();
    }
}

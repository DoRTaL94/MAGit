package components.filetree.folder;

import data.structures.*;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import magit.Engine;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.file.Paths;
import java.util.List;

public class FolderFileTree extends TreeView<Folder.Data> {
    private static final String FOLDER_ICON_PATH = "/components/filetree/resources/folder.png";
    private static final String TXT_FILE_ICON_PATH = "/components/filetree/resources/text-x-generic.png";
    private final Repository f_ActiveRepository =  Engine.Creator.getInstance().getActiveRepository();
    private final Folder m_Folder;
    private final String m_Location;

    public FolderFileTree(Folder i_Folder, String i_Location) {
        m_Location = i_Location;
        m_Folder = i_Folder;
        initializeFactory();
        CreateTree();
    }

    private void initializeFactory() {
        this.setCellFactory(e -> new TreeCell<Folder.Data>() {
            @Override
            protected void updateItem(Folder.Data item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item.getName());
                    setGraphic(getTreeItem().getGraphic());
                } else {
                    setText("");
                    setGraphic(null);
                }
            }
        });
    }

    private void CreateTree() {
        Folder.Data rootFolderData = new Folder.Data();
        rootFolderData.setSHA1(DigestUtils.sha1Hex(m_Folder.toStringForSha1(Paths.get(m_Location))));
        rootFolderData.setName(f_ActiveRepository.getName());
        rootFolderData.setFileType(eFileType.FOLDER);

        this.setRoot(CreateTreeRec(rootFolderData));
        this.getRoot().setExpanded(true);
    }

    private TreeItem<Folder.Data> CreateTreeRec(Folder.Data i_File) {
        TreeItem<Folder.Data> item = new TreeItem<>(i_File);

        if(i_File.getFileType().equals(eFileType.FOLDER)) {
            Folder root = f_ActiveRepository.getFolders().get(i_File.getSHA1());
            List<Folder.Data> files = root.getFiles();

            for(Folder.Data file: files) {
                item.getChildren().add(CreateTreeRec(file));
            }

            item.setGraphic(new ImageView(getClass().getResource(FOLDER_ICON_PATH).toExternalForm()));
        }
        else {
            item.setGraphic(new ImageView(getClass().getResource(TXT_FILE_ICON_PATH).toExternalForm()));
        }

        return item;
    }
}
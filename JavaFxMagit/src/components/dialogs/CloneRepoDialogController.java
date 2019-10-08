package components.dialogs;

import components.themes.ThemesController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class CloneRepoDialogController {

    @FXML private HBox hBoxHeader;
    @FXML private GridPane gridPaneInputs;
    @FXML private TextField textFieldRepoName;
    @FXML private TextField textFieldLocalRepoPath;
    @FXML private TextField textFieldRemoteRepoPath;
    @FXML private Button buttonOk;
    @FXML private Button buttonCancel;
    @FXML private VBox vBoxRoot;

    private static final String darkTheme = "resources/Dark.css";
    private static final String colorfulTheme = "resources/Colorful.css";
    private String currentTheme;
    private Runnable okAction;
    private Stage stage;

    public CloneRepoDialogController() {
        if(ThemesController.themeChangedProperty.get().equals("Dark")) {
            currentTheme = darkTheme;
        } else if(ThemesController.themeChangedProperty.get().equals("Colorful")) {
            currentTheme = colorfulTheme;
        } else {
            currentTheme = "";
        }

        ThemesController.themeChangedProperty.addListener((observable, oldValue, newValue) -> {
            if(newValue.equals("Dark")) {
                currentTheme = darkTheme;
            }
            else if(newValue.equals("Colorful")) {
                currentTheme = colorfulTheme;
            }

            if(newValue.equals("Default")) {
                currentTheme = "";
                vBoxRoot.getStylesheets().clear();
                vBoxRoot.applyCss();
            } else {
                vBoxRoot.getStylesheets().clear();
                vBoxRoot.getStylesheets().add(getClass().getResource(currentTheme).toExternalForm());
                vBoxRoot.applyCss();
            }
        });
    }

    public HBox getHBoxHeader() {
        return hBoxHeader;
    }

    public static CloneRepoDialogController loadFxml() {
        FXMLLoader fxmlLoader = new FXMLLoader();
        URL url = CloneRepoDialogController.class.getResource("CloneRepoDialog.fxml");
        fxmlLoader.setLocation(url);

        try {
            fxmlLoader.load(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fxmlLoader.getController();
    }

    @FXML public void initialize() {
        if(!currentTheme.isEmpty()) {
            vBoxRoot.getStylesheets().add(getClass().getResource(currentTheme).toExternalForm());
            vBoxRoot.applyCss();
        }
    }

    public void showDialog() {
        stage = new Stage();
        Scene mainScene = new Scene(vBoxRoot);
        stage.setScene(mainScene);
        stage.setTitle("Clone Repository");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/main/resources/MAGit.png")));
        stage.show();
    }

    @FXML void cancelAction() {
        stage.close();
    }

    @FXML void okAction() {
        okAction.run();
        stage.close();
    }

    public void setOkAction(Runnable i_OkAction) {
        okAction = i_OkAction;
    }

    public TextField getTextFieldRepoName() {
        return textFieldRepoName;
    }

    public TextField getTextFieldLocalRepoPath() {
        return textFieldLocalRepoPath;
    }

    public TextField getTextFieldRemoteRepoPath() {
        return textFieldRemoteRepoPath;
    }
}

package components.progress;

import components.Diff.DiffController;
import components.themes.ThemesController;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import main.MagitController;

import java.io.IOException;
import java.net.URL;

public class ProgressController {
    @FXML private VBox vBoxLoading;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea textBoxDetails;
    @FXML private Label labelPercent;

    private static final String darkTheme = "/main/resources/DarkDialog.css";
    private static final String colorfulTheme = "/main/resources/ColorfulDialog.css";
    private String currentTheme;
    private Stage stage;
    private Scene scene;
    private Task task;

    public ProgressController() {
        stage = new Stage();

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
                vBoxLoading.getStylesheets().clear();
                vBoxLoading.applyCss();
            } else {
                vBoxLoading.getStylesheets().clear();
                vBoxLoading.getStylesheets().add(getClass().getResource(currentTheme).toExternalForm());
                vBoxLoading.applyCss();
            }
        });
    }

    public void setTask(Task i_Task) {
        task = i_Task;

        i_Task.progressProperty().addListener((observable, oldValue, newValue) -> {
            progressBar.setProgress((double) newValue);
            labelPercent.setText(String.format("%.0f%s", ((double) newValue) * 100, "%"));
        });
        i_Task.setOnCancelled(event -> stage.close());
        i_Task.messageProperty().addListener((observable, oldValue, newValue) -> addText(newValue));
        MagitController.UIRefreshedProperty.addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                stage.close();
            }
        });
    }

    public void run() {
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
        stage.show();
    }

    public static ProgressController loadFXML() {
        FXMLLoader fxmlLoader = new FXMLLoader();
        URL url = DiffController.class.getResource("/components/progress/ProgressUI.fxml");
        fxmlLoader.setLocation(url);

        try {
            fxmlLoader.load(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fxmlLoader.getController();
    }

    @FXML public void initialize() {
        scene = new Scene(vBoxLoading);
        stage.setScene(scene);
        stage.setTitle("Loading...");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/main/resources/MAGit.png")));

        if(!currentTheme.isEmpty()) {
            vBoxLoading.getStylesheets().add(getClass().getResource(currentTheme).toExternalForm());
            vBoxLoading.applyCss();
        }
    }

    public void addText(String i_Text) {
        StringBuilder sb = new StringBuilder(textBoxDetails.getText());
        sb.append(String.format("%s%s", i_Text, System.lineSeparator()));
        textBoxDetails.setText(sb.toString());
    }
}

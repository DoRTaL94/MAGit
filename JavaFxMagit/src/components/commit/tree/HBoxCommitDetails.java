package components.commit.tree;

import components.themes.ThemesController;
import data.structures.Branch;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HBoxCommitDetails extends HBox {
    private static final double HBOX_HEIGHT = 32.0;
    private static final String darkTheme = "resources/Dark.css";
    private static final String colorfulTheme = "resources/Colorful.css";
    private static final String defaultTheme = "resources/Default.css";

    private static final String darkSelectedTheme = "resources/DarkSelected.css";
    private static final String colorfulSelectedTheme = "resources/ColorfulSelected.css";
    private static final String defaultSelectedTheme = "resources/DefaultSelected.css";

    private Label labelPointedCommitDescription;
    private Label labelUserName;
    private Label labelCommitTimeStamp;
    private Label labelCommitSha1;
    private Pane paneSpacer;
    private List<StackPane> stackPaneBranchNameList;
    private Runnable doubleClickAction;
    private Runnable clickAction;
    private String currentTheme;
    private String currentSelectedTheme;

    public HBoxCommitDetails() {
        this.setOnMouseClicked(this::commitDetails_Click);
        CommitItemFactory.GetListItemClickedProperty().addListener(observable -> updateGraphics());

        stackPaneBranchNameList = new ArrayList<>();
        paneSpacer = new Pane();
        labelUserName = new Label();
        labelPointedCommitDescription = new Label();
        labelCommitTimeStamp = new Label();
        labelCommitSha1 = new Label();

        this.setHeight(HBOX_HEIGHT);
        this.setPrefHeight(HBOX_HEIGHT);
        this.setMinHeight(HBOX_HEIGHT);
        this.setSpacing(10);

        if(ThemesController.themeChangedProperty.get().equals("Dark")) {
            currentTheme = darkTheme;
            currentSelectedTheme = darkTheme;
        } else if(ThemesController.themeChangedProperty.get().equals("Colorful")) {
            currentTheme = colorfulTheme;
            currentSelectedTheme = colorfulSelectedTheme;
        } else {
            currentTheme = defaultTheme;
            currentSelectedTheme = defaultSelectedTheme;
        }

        setStylesheet(currentTheme);

        ThemesController.themeChangedProperty.addListener((observable, oldValue, newValue) -> {
            if(newValue.equals("Dark")) {
                currentSelectedTheme = darkSelectedTheme;
                currentTheme = darkTheme;
            }
            else if(newValue.equals("Colorful")) {
                currentSelectedTheme = colorfulSelectedTheme;
                currentTheme = colorfulTheme;
            }
            else {
                currentSelectedTheme = defaultSelectedTheme;
                currentTheme = defaultTheme;
            }

            setStylesheet(currentTheme);
        });
    }

    private void updateGraphics() {
        if(currentTheme.equals(darkTheme)) {
            setStylesheet(darkTheme);
        }
        else if(currentTheme.equals(colorfulTheme)) {
            setStylesheet(colorfulTheme);
        }
        else {
            setStylesheet(defaultTheme);
        }
    }

    private void setStylesheet(String i_CssPath) {
        this.getStylesheets().clear();
        this.getStyleClass().clear();
        this.getStyleClass().add("hBoxCommitDetails");
        this.getStylesheets().add(getClass().getResource(i_CssPath).toExternalForm());
        this.applyCss();
    }

    private void commitDetails_Click(MouseEvent mouseEvent) {
        if(mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if(mouseEvent.getClickCount() == 1) {
                CommitItemFactory.GetListItemClickedProperty().set(CommitItemFactory.GetListItemClickedProperty().not().get());
                setStylesheet(currentSelectedTheme);
                if(clickAction != null) {
                    clickAction.run();
                }
            }
            else if(mouseEvent.getClickCount() == 2) {
                if (doubleClickAction != null) {
                    doubleClickAction.run();
                }
            }
        }
    }

    public void setDoubleClickAction(Runnable i_Action) {
        doubleClickAction = i_Action;
    }

    public void setClickAction(Runnable i_Action) {
        clickAction = i_Action;
    }

    public void addBranchNameRectangle(Branch i_Branch) {
        Text textLabelBranch = new Text(i_Branch.getName());
        textLabelBranch.setFont(Font.font("System", FontWeight.BOLD, 15));
        double labelBranchNameWidth = textLabelBranch.getLayoutBounds().getWidth();

        StackPane stackPaneBranchName = new StackPane();
        Rectangle rectBranchName = new Rectangle();
        Label labelBranch = new Label();

        labelBranch.setText(i_Branch.getName());
        labelBranch.setPrefWidth(labelBranchNameWidth);

        stackPaneBranchName.minWidthProperty().bind(rectBranchName.widthProperty());
        stackPaneBranchName.maxWidthProperty().bind(rectBranchName.widthProperty());
        stackPaneBranchName.setPrefHeight(HBOX_HEIGHT);

        if(i_Branch.isTracking()) {
            rectBranchName.getStyleClass().add("rectBranchNameRTB");
        } else {
            rectBranchName.getStyleClass().add("rectBranchName");
        }

        rectBranchName.setHeight(HBOX_HEIGHT);
        rectBranchName.widthProperty().bind(Bindings.add(30, labelBranch.prefWidthProperty()));

        labelBranch.getStyleClass().add("labelBranchName");
        labelBranch.setPrefHeight(HBOX_HEIGHT);
        labelBranch.minWidthProperty().bind(labelBranch.prefWidthProperty());
        labelBranch.maxWidthProperty().bind(labelBranch.prefWidthProperty());

        stackPaneBranchName.getChildren().add(rectBranchName);
        stackPaneBranchName.getChildren().add(labelBranch);

        stackPaneBranchNameList.add(stackPaneBranchName);
    }

    public void update() {
        paneSpacer.setPrefWidth(0);
        paneSpacer.setPrefHeight(HBOX_HEIGHT);

        setLabelStyle(labelUserName);
        setLabelStyle(labelPointedCommitDescription);
        setLabelStyle(labelCommitTimeStamp);
        setLabelStyle(labelCommitSha1);

        this.getChildren().add(paneSpacer);

        for(StackPane stackPane: stackPaneBranchNameList) {
            this.getChildren().add(stackPane);
        }

        this.getChildren().add(labelPointedCommitDescription);
        this.getChildren().add(labelUserName);
        this.getChildren().add(labelCommitTimeStamp);
        this.getChildren().add(labelCommitSha1);
    }

    private void setLabelStyle(Label i_Label) {
        i_Label.getStyleClass().add("CommitDetail");
        i_Label.setPrefHeight(HBOX_HEIGHT);
        i_Label.minWidthProperty().bind(i_Label.prefWidthProperty());
        i_Label.maxWidthProperty().bind(i_Label.prefWidthProperty());
    }

    public List<StackPane> getBranchNameRectangleList() {
        return stackPaneBranchNameList;
    }

    public Label getLabelPointedCommitDescription() {
        return labelPointedCommitDescription;
    }

    public Label getLabelUserName() {
        return labelUserName;
    }

    public Label getLabelCommitDaysAgo() {
        return labelCommitTimeStamp;
    }

    public Label getLabelCommitSha1() {
        return labelCommitSha1;
    }

    public Pane getPaneSpacer() {
        return paneSpacer;
    }
}

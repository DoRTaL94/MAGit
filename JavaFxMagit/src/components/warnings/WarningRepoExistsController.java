package components.warnings;

import javafx.beans.property.BooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class WarningRepoExistsController {

    @FXML private Button buttonExistingRepo;
    @FXML private Button buttonDeleteAndCreateNew;
    @FXML private Button buttonCancelRepoWarning;

    private BooleanProperty isLoadRepoProperty;
    private BooleanProperty isCreateNewProperty;
    private BooleanProperty isCancelProperty;

    @FXML void cancelAction(ActionEvent event) {
        isCancelProperty.set(!isCancelProperty.get());
    }

    @FXML void deleteAndCreateNewRepoAction(ActionEvent event) {
        isCreateNewProperty.set(!isCreateNewProperty.get());
    }

    @FXML void loadExistingRepoAction(ActionEvent event) {
        isLoadRepoProperty.set(!isCancelProperty.get());
    }

    public void setIsCancelProperty(BooleanProperty i_IsCancelProperty) {
        isCancelProperty = i_IsCancelProperty;
    }

    public void setIsLoadRepoProperty(BooleanProperty i_IsLoadRepoProperty) {
        isLoadRepoProperty = i_IsLoadRepoProperty;
    }

    public void setIsCreateNewProperty(BooleanProperty i_IsCreateNewProperty) {
        isCreateNewProperty = i_IsCreateNewProperty;
    }
}

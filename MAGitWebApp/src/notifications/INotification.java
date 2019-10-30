package notifications;

import javafx.beans.property.BooleanProperty;

public interface INotification {
    void setReadByUser(boolean i_IsRead);
    void setNotShowNotification(boolean i_IsNotShow);
}

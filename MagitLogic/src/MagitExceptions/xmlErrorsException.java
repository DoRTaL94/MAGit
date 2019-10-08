package MagitExceptions;

import java.util.List;

public class xmlErrorsException extends Exception {
    List<String> errors = null;

    public xmlErrorsException(String i_Message) {
        super(i_Message);
    }

    public xmlErrorsException(List<String> i_ErrorsList) {
        errors = i_ErrorsList;
    }

    public List<String> getErrors() {
        return errors;
    }
}

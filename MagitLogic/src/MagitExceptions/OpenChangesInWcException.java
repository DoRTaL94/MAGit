package MagitExceptions;

import java.util.List;

public class OpenChangesInWcException extends Exception {
    List<List<String>> wcStatus;

    public OpenChangesInWcException(List<List<String>> i_WcStatus) {
        wcStatus = i_WcStatus;
    }

    public List<List<String>> getWcStatus() {
        return wcStatus;
    }
}

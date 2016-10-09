package model;

import java.io.IOException;

/**
 * Created by Boris on 09-Oct-16.
 */
public class DBException extends IOException {
    public DBException(String message) {
        super("DataBase error: " + message + ". Probably DataBase is down");
    }
}

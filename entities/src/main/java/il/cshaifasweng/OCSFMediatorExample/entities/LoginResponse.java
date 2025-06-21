package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class LoginResponse implements Serializable {
    public boolean success;
    public String message;
    public String username;

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public LoginResponse(boolean success, String message, String username) {
        this.success = success;
        this.message = message;
        this.username = username;
    }
}

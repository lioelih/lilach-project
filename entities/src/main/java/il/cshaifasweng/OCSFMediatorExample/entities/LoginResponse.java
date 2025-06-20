package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class LoginResponse implements Serializable {
    public boolean success;
    public String message;

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}

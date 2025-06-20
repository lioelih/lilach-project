package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class LogoutRequest implements Serializable {
    public String username;

    public LogoutRequest(String username) {
        this.username = username;
    }
}
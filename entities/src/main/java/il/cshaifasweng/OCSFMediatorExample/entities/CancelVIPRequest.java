package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class CancelVIPRequest implements Serializable {

    private String username;

    public CancelVIPRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}

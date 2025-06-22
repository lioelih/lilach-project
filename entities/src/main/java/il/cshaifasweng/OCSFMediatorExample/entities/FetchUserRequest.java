package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class FetchUserRequest implements Serializable {
    private String username;
    public FetchUserRequest(String username) { this.username = username; }
    public String getUsername() { return username; }
}

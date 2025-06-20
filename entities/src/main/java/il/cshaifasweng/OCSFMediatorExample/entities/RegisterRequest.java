package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class RegisterRequest implements Serializable {
    public String username, password, phoneNumber, email, fullName;

    public RegisterRequest(String username, String email, String phoneNumber, String fullName, String password) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.fullName = fullName;
        this.password = password;
    }
}

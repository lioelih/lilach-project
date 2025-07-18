package il.cshaifasweng;

import java.io.Serializable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class UserDisplayDTO implements Serializable {
    private int id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String role;
    private boolean active;
    private double totalSpent;
    private final BooleanProperty vip = new SimpleBooleanProperty();
    private String branchName;

    public UserDisplayDTO(int id, String username, String email, String phone,
                          String role, boolean active, double totalSpent, String branchName, String password, boolean vip){
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.active = active;
        this.totalSpent = totalSpent;
        this.branchName = branchName;
        this.password = password;
        this.vip.set(vip);
    }

    public String getBranchName() {
        return branchName;
    }


    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setUsername(String newUsername) {
        this.username = newUsername;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isVip() {
        return vip.get();
    }
    public void setVip(boolean vip) {
        this.vip.set(vip);
    }

    public BooleanProperty vipProperty() {
        return vip;
    }
}

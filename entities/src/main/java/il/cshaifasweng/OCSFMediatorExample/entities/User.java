package il.cshaifasweng.OCSFMediatorExample.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
@Entity
@Table(name = "users")
public class User implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "branch")
    private String branch;

    @Column(name = "isVIP")
    private boolean isVIP = false;

    @Column(name = "isActive")
    private boolean isActive = true;

    @Column(name = "identification_number")
    private String identificationNumber;

    @Column(name = "credit_card_number")
    private String creditCardNumber;

    @Column(name = "credit_card_expiration")
    private String creditCardExpiration;

    @Column(name = "credit_card_security_code")
    private String creditCardSecurityCode;

    @Column(name = "vip_expiration_date")
    private LocalDate vipExpirationDate;

    @Column(name = "vip_canceled")
    private boolean vipCanceled;

    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        USER, WORKER, MANAGER, ADMIN
    }
    @Column(name = "is_logged_in")
    private boolean loggedIn;

    public User() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public boolean isLoggedIn() { return loggedIn; }
    public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public boolean isVIP() {
        return isVIP;
    }
    public void setVIP(boolean isVIP) {
        this.isVIP = isVIP;
    }

    public boolean isActive() {
        return isActive;
    }
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }
    public void setIdentificationNumber(String identificationNumber) {
        this.identificationNumber = identificationNumber;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }
    public void setCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public String getCreditCardExpiration() {
        return creditCardExpiration;
    }
    public void setCreditCardExpiration(String creditCardExpiration) {
        this.creditCardExpiration = creditCardExpiration;
    }

    public String getCreditCardSecurityCode() {
        return creditCardSecurityCode;
    }
    public void setCreditCardSecurityCode(String creditCardSecurityCode) {
        this.creditCardSecurityCode = creditCardSecurityCode;
    }

    public LocalDate getVipExpirationDate() {
        return vipExpirationDate;
    }
    public void setVipExpirationDate(LocalDate vipExpirationDate) {
        this.vipExpirationDate = vipExpirationDate;
    }

    public boolean getVipCanceled() {
        return vipCanceled;
    }

    public void setVipCanceled(boolean vipCanceled) {
        this.vipCanceled = vipCanceled;
    }
}

package il.cshaifasweng;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class LoginUserDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;                 // User.Role.name()
    private boolean VIP;                 // keep name to match existing isVIP usage
    private boolean active;
    private LocalDate vipExpirationDate;
    private boolean vipCanceled;
    private Integer branchId;            // nullable
    private String branchName;           // nullable
    private double compensationTab;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isVIP() { return VIP; }
    public void setVIP(boolean VIP) { this.VIP = VIP; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDate getVipExpirationDate() { return vipExpirationDate; }
    public void setVipExpirationDate(LocalDate vipExpirationDate) { this.vipExpirationDate = vipExpirationDate; }

    public boolean isVipCanceled() { return vipCanceled; }
    public void setVipCanceled(boolean vipCanceled) { this.vipCanceled = vipCanceled; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public double getCompensationTab() { return compensationTab; }
    public void setCompensationTab(double compensationTab) { this.compensationTab = compensationTab; }
}

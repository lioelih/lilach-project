package il.cshaifasweng.OCSFMediatorExample.entities;


abstract class UserPrivilege {
    protected String username;
    protected String fullName;
    protected UserRole role;

    public UserPrivilege(String username, String fullName, UserRole role) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public abstract int getPrivilegeLevel();

    public UserRole getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }
}
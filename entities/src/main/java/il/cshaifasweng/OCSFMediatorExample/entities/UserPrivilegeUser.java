package il.cshaifasweng.OCSFMediatorExample.entities;

class UserPrivilegeUser extends UserPrivilege {
    public UserPrivilegeUser(String username, String fullName) {
        super(username, fullName, UserRole.USER);
    }

    @Override
    public int getPrivilegeLevel() {
        return 1;
    }
}

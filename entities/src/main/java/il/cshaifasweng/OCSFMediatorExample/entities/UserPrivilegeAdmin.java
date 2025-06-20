package il.cshaifasweng.OCSFMediatorExample.entities;

class UserPrivilegeAdmin extends UserPrivilege {
    public UserPrivilegeAdmin(String username, String fullName) {
        super(username, fullName, UserRole.ADMIN);
    }

    @Override
    public int getPrivilegeLevel() {
        return 4;
    }
}

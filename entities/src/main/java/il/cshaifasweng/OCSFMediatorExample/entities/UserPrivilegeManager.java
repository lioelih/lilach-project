package il.cshaifasweng.OCSFMediatorExample.entities;

class UserPrivilegeManager extends UserPrivilege {
    public UserPrivilegeManager(String username, String fullName) {
        super(username, fullName, UserRole.MANAGER);
    }

    @Override
    public int getPrivilegeLevel() {
        return 3;
    }
}

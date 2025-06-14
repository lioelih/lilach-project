package il.cshaifasweng.OCSFMediatorExample.entities;

class UserPrivilegeWorker extends UserPrivilege {
    public UserPrivilegeWorker(String username, String fullName) {
        super(username, fullName, UserRole.WORKER);
    }

    @Override
    public int getPrivilegeLevel() {
        return 2;
    }
}

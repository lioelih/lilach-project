package il.cshaifasweng.OCSFMediatorExample.entities;
import java.io.Serializable;

public class FetchUserResponse implements Serializable {
    private User user;
    public FetchUserResponse(User user) { this.user = user; }
    public User getUser() { return user; }
}

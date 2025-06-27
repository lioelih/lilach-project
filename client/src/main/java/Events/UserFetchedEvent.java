package Events;

import il.cshaifasweng.Msg;

public class UserFetchedEvent {
    private Msg msg;

    public UserFetchedEvent(Msg msg) {
        this.msg = msg;
    }

    public Msg getMsg() {
        return msg;
    }
}

package Events;

import il.cshaifasweng.Msg;

public class LoginEvent {
    private Msg msg;

    public LoginEvent(Msg msg) {
        this.msg = msg;
    }

    public Msg getMsg() {
        return msg;
    }
}

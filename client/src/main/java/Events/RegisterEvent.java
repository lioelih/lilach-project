package Events;

import il.cshaifasweng.Msg;

public class RegisterEvent {
    private Msg msg;

    public RegisterEvent(Msg msg) {
        this.msg = msg;
    }

    public Msg getMsg() {
        return msg;
    }
}

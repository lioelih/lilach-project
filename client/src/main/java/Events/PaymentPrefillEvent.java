package Events;

import il.cshaifasweng.Msg;

public class PaymentPrefillEvent {
    private Msg msg;

    public PaymentPrefillEvent(Msg msg) {
        this.msg = msg;
    }

    public Msg getMsg() {
        return msg;
    }
}

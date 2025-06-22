package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class PaymentInfoResponse implements Serializable {
    private boolean success;
    private String message;

    public PaymentInfoResponse(boolean success, String message) {
    this.success = success;
    this.message = message;
    }
}
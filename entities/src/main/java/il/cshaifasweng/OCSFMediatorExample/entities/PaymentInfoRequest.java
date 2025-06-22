package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class PaymentInfoRequest implements Serializable {
    private String username;
    private String idNumber;
    private String cardNumber;
    private String expDate;
    private String cvv;
    private boolean activateVIP;

    public PaymentInfoRequest(String username, String idNumber, String cardNumber, String expDate, String cvv, boolean activateVIP) {
        this.username = username;
        this.idNumber = idNumber;
        this.cardNumber = cardNumber;
        this.expDate = expDate;
        this.cvv = cvv;
        this.activateVIP = activateVIP;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpDate() {
        return expDate;
    }

    public void setExpDate(String expDate) {
        this.expDate = expDate;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public boolean isActivateVIP() {
        return activateVIP;
    }

    public void setActivateVIP(boolean activateVIP) {
        this.activateVIP = activateVIP;
    }
}

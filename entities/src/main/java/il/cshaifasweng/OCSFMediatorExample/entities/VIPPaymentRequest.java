
package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class VIPPaymentRequest implements Serializable {
    private String username;
    private String idNumber;
    private String cardNumber;
    private String expDate;
    private String cvv;

    public VIPPaymentRequest(String username, String idNumber, String cardNumber, String expDate, String cvv) {
        this.username = username;
        this.idNumber = idNumber;
        this.cardNumber = cardNumber;
        this.expDate = expDate;
        this.cvv = cvv;
    }

    public String getUsername() {
        return username;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getExpDate() {
        return expDate;
    }

    public String getCvv() {
        return cvv;
    }
}

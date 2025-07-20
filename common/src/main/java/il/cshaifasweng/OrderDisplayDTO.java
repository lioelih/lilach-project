package il.cshaifasweng;

import il.cshaifasweng.OCSFMediatorExample.entities.Order;

import java.io.Serializable;
import java.time.LocalDateTime;

public class OrderDisplayDTO implements Serializable {
    private int id;
    private String username;
    private String fulfilment;
    private int status;
    private double totalPrice;
    private LocalDateTime deadline;
    private final String recipient;
    private final String greeting;
    private double compensationUsed;
    public OrderDisplayDTO(int id, String username, String fulfilment,
                           double totalPrice, LocalDateTime deadline,
                           String recipient, String greeting, int status, double compensationUsed ) {
        this.id          = id;
        this.username    = username;
        this.fulfilment  = fulfilment;
        this.totalPrice  = totalPrice;
        this.deadline    = deadline;
        this.recipient   = recipient;
        this.greeting    = greeting;
        this.status      = status;
        this.compensationUsed = compensationUsed;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFulfilment() { return fulfilment; }
    public int getStatus() { return status; }
    public double getTotalPrice() { return totalPrice; }
    public boolean isReceived()  { return status == Order.STATUS_RECEIVED; }
    public boolean isCancelled() { return status == Order.STATUS_CANCELLED; }
    public LocalDateTime getDeadline() { return deadline; }
    public double getCompensationUsed() { return compensationUsed; }
    public String getRecipient(){ return recipient; }
    public String getGreeting() { return greeting;  }
}

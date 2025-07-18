package il.cshaifasweng;

import java.io.Serializable;
import java.time.LocalDateTime;

public class OrderDisplayDTO implements Serializable {
    private int id;
    private String username;
    private String fulfilment;
    private String status;
    private double totalPrice;
    private boolean received;
    private LocalDateTime deadline;
    public OrderDisplayDTO(int id, String username, String fulfilment,
                           String status, double totalPrice,LocalDateTime deadline, boolean received) {
        this.id = id;
        this.username = username;
        this.fulfilment = fulfilment;
        this.status = status;
        this.totalPrice = totalPrice;
        this.received = received;
        this.deadline = deadline;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFulfilment() { return fulfilment; }
    public String getStatus() { return status; }
    public double getTotalPrice() { return totalPrice; }
    public boolean isReceived() { return received; }
    public LocalDateTime getDeadline() { return deadline; }
}

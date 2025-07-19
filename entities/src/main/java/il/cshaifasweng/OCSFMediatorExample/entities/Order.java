
package il.cshaifasweng.OCSFMediatorExample.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "orders")
public class Order {

    public static final int STATUS_PENDING   = 0;
    public static final int STATUS_RECEIVED  = 1;
    public static final int STATUS_CANCELLED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private int orderId;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "delivery")
    private String delivery;

    @Column(name="status", nullable=false)
    private int status = STATUS_PENDING;

    @Column(name = "total_price")
    private double totalPrice;

    @Column(name="deadline", nullable=false)
    private LocalDateTime deadline;

    @Column(name="recipient", nullable=false)
    private String recipient;

    @Column(name="greeting")
    private String greeting;

    @Column(name="compensation_used", nullable=false)
    private double compensationUsed = 0.0;

    public Order() {}

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public String getDelivery() {
        return delivery;
    }

    public void setDelivery(String delivery) {
        this.delivery = delivery;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public boolean isReceived()   { return status == STATUS_RECEIVED;  }
    public boolean isCancelled()  { return status == STATUS_CANCELLED; }


    public double getTotalPrice()        { return totalPrice; }
    public void   setTotalPrice(double p){ this.totalPrice = p; }

    public String getFulfilInfo() {
        return (delivery == null || delivery.isBlank()) ? branch.getName() : delivery;
    }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getRecipient() { return recipient; }
    public void   setRecipient(String r){ this.recipient = r; }

    public String getGreeting(){ return greeting; }
    public void   setGreeting(String g){ this.greeting = g; }

    public double getCompensationUsed() { return compensationUsed; }
    public void   setCompensationUsed(double v) { compensationUsed = v; }
}

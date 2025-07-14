
package il.cshaifasweng.OCSFMediatorExample.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {

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

    @Column(name = "received")
    private boolean received;

    @Column(name = "total_price")
    private double totalPrice;


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

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }

    public String getStatusString() {
        return received ? "Delivered" : "Awaiting Delivery";
    }

    public double getTotalPrice()        { return totalPrice; }
    public void   setTotalPrice(double p){ this.totalPrice = p; }

    public String getFulfilInfo() {
        return (delivery == null || delivery.isBlank()) ? branch.getName() : delivery;
    }
}

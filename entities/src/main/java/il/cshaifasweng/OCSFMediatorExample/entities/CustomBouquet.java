package il.cshaifasweng.OCSFMediatorExample.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "custom_bouquet")
public class CustomBouquet implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;

    // who created it
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // once ordered, link here
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, length = 50)
    private String style;

    @Column(name = "dom_color", length = 30)
    private String dominantColor;

    @Column(length = 50)
    private String pot;

    @Column(name = "custom_price", nullable = false)
    private double totalPrice;

    @Column(name = "created_at", nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "bouquet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomBouquetItem> items = new ArrayList<>();

    // --- constructors, getters & setters ---

    public CustomBouquet() { }

    public Integer getId() {
        return id;
    }

    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public Order getOrder() {
        return order;
    }
    public void setOrder(Order order) {
        this.order = order;
    }

    public String getStyle() {
        return style;
    }
    public void setStyle(String style) {
        this.style = style;
    }

    public String getDominantColor() {
        return dominantColor;
    }
    public void setDominantColor(String dominantColor) {
        this.dominantColor = dominantColor;
    }

    public String getPot() {
        return pot;
    }
    public void setPot(String pot) {
        this.pot = pot;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<CustomBouquetItem> getItems() {
        return items;
    }
    public void addItem(CustomBouquetItem item) {
        items.add(item);
        item.setBouquet(this);
    }
    public void removeItem(CustomBouquetItem item) {
        items.remove(item);
        item.setBouquet(null);
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

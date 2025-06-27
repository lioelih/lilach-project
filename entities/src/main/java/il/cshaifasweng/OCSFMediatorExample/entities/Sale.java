package il.cshaifasweng.OCSFMediatorExample.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sales")
public class Sale implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType;

    @Column(name = "discount_value")
    private Double discountValue; // can be null for BUY_X_GET_Y

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    // For BUY_X_GET_Y
    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @Column(name = "get_quantity")
    private Integer getQuantity;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleProduct> saleProducts;

    // Constructors
    public Sale() {}

    // Getters and Setters

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public Double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(Double discountValue) {
        this.discountValue = discountValue;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Integer getBuyQuantity() {
        return buyQuantity;
    }

    public void setBuyQuantity(Integer buyQuantity) {
        this.buyQuantity = buyQuantity;
    }

    public Integer getGetQuantity() {
        return getQuantity;
    }

    public void setGetQuantity(Integer getQuantity) {
        this.getQuantity = getQuantity;
    }

    public List<SaleProduct> getSaleProducts() {
        return saleProducts;
    }

    public void setSaleProducts(List<SaleProduct> saleProducts) {
        this.saleProducts = saleProducts;
    }
}

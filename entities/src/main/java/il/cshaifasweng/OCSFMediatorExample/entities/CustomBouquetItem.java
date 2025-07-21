package il.cshaifasweng.OCSFMediatorExample.entities;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "custom_bouquet_item")
public class CustomBouquetItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // back‑ref to the bouquet
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bouquet_id", nullable = false)
    private CustomBouquet bouquet;

    // which flower
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    // --- constructors, getters & setters ---

    public CustomBouquetItem() { }

    public Integer getId() {
        return id;
    }

    public CustomBouquet getBouquet() {
        return bouquet;
    }
    public void setBouquet(CustomBouquet bouquet) {
        this.bouquet = bouquet;
    }

    public Product getProduct() {
        return product;
    }
    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

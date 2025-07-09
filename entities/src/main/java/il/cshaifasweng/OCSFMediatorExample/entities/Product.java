package il.cshaifasweng.OCSFMediatorExample.entities;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "products")
public class Product implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int product_id;

    private String product_name;
    private String product_type;
    private double product_price;

    @Lob
    @Column(name = "product_image", columnDefinition = "MEDIUMBLOB")
    private byte[] product_image;

    public Product() {}

    public Product(int product_id, String product_name, String product_type, double product_price, byte[] product_image) {
        this.product_id = product_id;
        this.product_name = product_name;
        this.product_type = product_type;
        this.product_price = product_price;
        this.product_image = product_image;
    }

    public Product(String product_name, String product_type, double product_price, byte[] product_image) {
        this.product_name = product_name;
        this.product_type = product_type;
        this.product_price = product_price;
        this.product_image = product_image;
    }

    public int getId() {
        return product_id;
    }

    public String getName() {
        return product_name;
    }

    public void setName(String product_name) {
        this.product_name = product_name;
    }

    public String getType() {
        return product_type;
    }

    public void setType(String product_type) {
        this.product_type = product_type;
    }

    public double getPrice() {
        return product_price;
    }

    public void setPrice(double product_price) {
        this.product_price = product_price;
    }

    public byte[] getImage() {
        return product_image;
    }

    public void setImage(byte[] product_image) {
        this.product_image = product_image;
    }

    //method for updating product details
    public void updateProduct(String product_name, String product_type, double product_price, byte[] product_image) {
        this.product_name = product_name;
        this.product_type = product_type;
        this.product_price = product_price;
        this.product_image = product_image;
    }

    @Override
    public String toString() {
        return String.format("ID: %d | Name: %s | Type: %s | Price: %.2f | Image: %s",
                product_id, product_name, product_type, product_price, product_image != null ? product_image.length : 0);
    }
}

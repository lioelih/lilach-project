package il.cshaifasweng.OCSFMediatorExample.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Entity
@Table(name = "sale_products")
public class SaleProduct implements Serializable {

    @EmbeddedId
    private SaleProductId id = new SaleProductId();

    @ManyToOne
    @MapsId("saleId")
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @ManyToOne
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @Column(name = "get_quantity")
    private Integer getQuantity;

    public SaleProduct() {}

    public SaleProduct(Sale sale, Product product) {
        this.sale = sale;
        this.product = product;
        this.id = new SaleProductId(sale.getId(), product.getId());
    }

    // Getters & setters ...

    // ✅ Embedded composite key defined inside the class
    @Embeddable
    public static class SaleProductId implements Serializable {
        private int saleId;
        private int productId;

        public SaleProductId() {}

        public SaleProductId(int saleId, int productId) {
            this.saleId = saleId;
            this.productId = productId;
        }

        public int getSaleId() {
            return saleId;
        }

        public void setSaleId(int saleId) {
            this.saleId = saleId;
        }

        public int getProductId() {
            return productId;
        }

        public void setProductId(int productId) {
            this.productId = productId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SaleProductId)) return false;
            SaleProductId that = (SaleProductId) o;
            return saleId == that.saleId && productId == that.productId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(saleId, productId);
        }
    }
}

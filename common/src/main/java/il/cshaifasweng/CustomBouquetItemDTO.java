
package il.cshaifasweng;

import java.io.Serializable;

public class CustomBouquetItemDTO implements Serializable {
    private Integer bouquetId;  // null for a new line
    private Integer productId;  // which flower
    private int     quantity;   // how many

    public CustomBouquetItemDTO() {}

    public CustomBouquetItemDTO(
            Integer bouquetId,
            Integer productId,
            int     quantity
    ) {
        this.bouquetId = bouquetId;
        this.productId = productId;
        this.quantity  = quantity;
    }

    public Integer getBouquetId() { return bouquetId; }
    public Integer getProductId() { return productId; }
    public int     getQuantity()  { return quantity; }
}

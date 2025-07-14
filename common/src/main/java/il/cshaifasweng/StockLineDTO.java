package il.cshaifasweng;

import java.io.Serializable;

public record StockLineDTO(
        int storage_id,
        int product_id,
        String product_name,
        String product_type,
        double product_price,
        byte[] product_image,
        int branch_id,
        String branch_name,
        int quantity
) implements Serializable {}

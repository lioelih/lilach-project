package il.cshaifasweng;

import java.io.Serializable;
import java.util.List;
// Seems redundant to make so many DTOS but with each one having a different purpose I think it's safer to distribute them that way and use them for each different aspect rather than one concentrated bunch
public class OrderDetailsDTO implements Serializable {
    public static class Line implements Serializable {
        private final String productName;
        private final int    quantity;
        private final double linePrice;
        public Line(String productName, int qty, double linePrice) {
            this.productName = productName;
            this.quantity    = qty;
            this.linePrice   = linePrice;
        }
        public String getProductName() { return productName; }
        public int    getQuantity()    { return quantity;    }
        public double getLinePrice()   { return linePrice;   }
    }

    private final List<Line> lines;
    private final double     subtotal;
    private final double     saleDiscount;
    private final double     vipDiscount;
    private final double     deliveryFee;
    private final double     total;
    private final double compensationUsed;
    public OrderDetailsDTO(
            List<Line> lines,
            double subtotal,
            double saleDiscount,
            double vipDiscount,
            double deliveryFee,
            double total,
            double compensationUsed
    ) {
        this.lines        = lines;
        this.subtotal     = subtotal;
        this.saleDiscount = saleDiscount;
        this.vipDiscount  = vipDiscount;
        this.deliveryFee  = deliveryFee;
        this.total        = total;
        this.compensationUsed = compensationUsed;
    }

    public List<Line> getLines()         { return lines;        }
    public double     getSubtotal()      { return subtotal;     }
    public double     getSaleDiscount()  { return saleDiscount; }
    public double     getVipDiscount()   { return vipDiscount;  }
    public double     getDeliveryFee()   { return deliveryFee;  }
    public double     getTotal()         { return total;        }
    public double getCompensationUsed() { return compensationUsed; }
}

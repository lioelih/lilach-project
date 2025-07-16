package il.cshaifasweng.OCSFMediatorExample.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
public class Sale implements Serializable {

    public enum DiscountType {
        PERCENTAGE, FIXED, BUNDLE, BUY_X_GET_Y
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value")
    private Double discountValue;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @Column(name = "get_quantity")
    private Integer getQuantity;

    @Transient
    private List<Integer> productIds = new ArrayList<>();

    // --- Constructors ---

    public Sale() {
    }

    // --- Getters and Setters ---

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

    public List<Integer> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Integer> products) {
        this.productIds = products;
    }

    /**
     * checks if the given product is on sale and returns true\false accordingly
     *
     * @param product The product that we want to check for sales.
     * @param sales The list of all sales.
     * @return product is or isn't on sale.
     */
    public static boolean onSale(Product product, List<Sale> sales) {
        if (sales == null) return false;

        for (Sale sale : sales)
            if (sale.getProductIds().contains(product.getId()) && sale.getEndDate().isAfter(LocalDateTime.now()))
                return true;

        return false;
    }

    /**
     * search for the active sales of a given product
     *
     * @param product The product that we want to check for sales.
     * @param sales The list of all sales.
     * @return List of all active sales for the given product. if there are no sales return null
     */
    public static List<Sale> getProductSales(Product product, List<Sale> sales) {
        if(!Sale.onSale(product,sales)) return null;
        List<Sale> activeProductSales = new ArrayList<>();

        for (Sale sale : sales)
            if (sale.getProductIds().contains(product.getId()) && sale.getEndDate().isAfter(LocalDateTime.now()))
                activeProductSales.add(sale);

        return activeProductSales;
    }

    /**
     * output: the product that is bundled with the given one.
     * if there is no active bundle than it will return Null
     *
     * @param product The product that we want to check for bundled sales.
     * @param products The list of all active products.
     * @param sale The bundled sale.
     * @return The product that is bundled with the given one.
     */
    public static Product getBundledProduct(Product product, List<Product> products, Sale sale) {
        int bundledProductId = -1;
        //find the product id if exists
        if(sale != null) {
            if (sale.getProductIds().contains(product.getId()) && sale.getEndDate().isAfter(LocalDateTime.now())) {
                if (sale.getProductIds().get(0) == product.getId()) {
                    bundledProductId = sale.getProductIds().get(1);
                } else {
                    bundledProductId = sale.getProductIds().get(0);
                }
            }
        }
        //find the product from list if exists
        if(bundledProductId != -1)
            for (Product product1 : products)
                if(product1.getId() == bundledProductId) return product1;
        return  null;
    }

    /**
     * get the final price for a product after the sale discount
     * only work for PERCENTAGE, FIXED discount types.
     *
     * @param product The product that we want to check for discount.
     * @param sales The list of all sales.
     * @return The final price of the product.
     */
    public static double getDiscountedPrice(Product product, List<Sale> sales) {
        double finalPrice = product.getPrice();
        if (!onSale(product, sales)) return finalPrice;
        List<Sale> activeProductSales = getProductSales(product,sales);
        if(activeProductSales == null) return  finalPrice;
        for(Sale sale : activeProductSales) {
            if(sale.getDiscountType() == DiscountType.FIXED)
                finalPrice -= sale.getDiscountValue();
            if(sale.getDiscountType() == DiscountType.PERCENTAGE) {
                finalPrice *= sale.getDiscountValue();
                finalPrice /= 100;
                finalPrice = product.getPrice() - finalPrice;
            }
        }
        return finalPrice;
    }
}

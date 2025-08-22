package il.cshaifasweng.OCSFMediatorExample.entities;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "sales")
public class Sale implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
     * Checks if the sale is active at any point during the given date range.
     * This includes any overlap between the sale's start/end and the range.
     *
     * @param rangeStart start of the date range
     * @param rangeEnd end of the date range
     * @return true if any part of the sale falls within the range
     */
    public boolean isActiveBetween(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (startDate == null || endDate == null || rangeStart == null || rangeEnd == null) {
            return false;
        }

        return !startDate.isAfter(rangeEnd) && !endDate.isBefore(rangeStart);
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

    /**
     * Calculates the total discount amount for a list of basket items based on applicable sales.
     * The function checks each item in the basket against the provided sales,
     * applies any applicable discount, and returns the total value of the discount applied.
     *
     * @param basketList The list of basket items currently in the user's basket.
     * @param sales The list of all available sales.
     * @return The total discount amount to be subtracted from the basket's total price.
     */
    public static double calculateTotalDiscount(List<Basket> basketList, List<Sale> sales) {
        if (basketList == null || sales == null) return 0.0;

        Map<Integer, Integer> paidQuantities = new HashMap<>();
        Map<Integer, Integer> freeQuantities = new HashMap<>();
        Map<Integer, Double> realPrices = new HashMap<>();

        // Step 1: Calculate real prices (apply percentage or fixed)
        for (Basket basket : basketList) {
            Product product = basket.getProduct();
            int productId = product.getId();
            double basePrice = product.getPrice();
            double realPrice = basePrice;

            for (Sale sale : sales) {
                if (!sale.getProductIds().contains(productId)) continue;
                if (sale.getStartDate().isAfter(java.time.LocalDateTime.now()) ||
                        sale.getEndDate().isBefore(java.time.LocalDateTime.now())) continue;

                if (sale.getDiscountType() == DiscountType.PERCENTAGE) {
                    realPrice = basePrice * (1 - sale.getDiscountValue() / 100.0);
                    break;
                } else if (sale.getDiscountType() == DiscountType.FIXED) {
                    realPrice = basePrice - sale.getDiscountValue();
                    break;
                }
            }

            realPrices.put(productId, Math.max(0.0, realPrice));
            paidQuantities.put(productId, basket.getAmount());
        }

        double totalDiscount = 0.0;

        // Step 1.5: Calculate and apply percentage/fixed discount
        for (Basket basket : basketList) {
            int productId = basket.getProduct().getId();
            double originalPrice = basket.getProduct().getPrice();
            double realPrice = realPrices.getOrDefault(productId, originalPrice);
            int quantity = basket.getAmount();

            double priceDifference = originalPrice - realPrice;

            if (priceDifference > 0) {
                totalDiscount += priceDifference * quantity;
            }
        }

        // Step 2: BUY_X_GET_Y
        for (Sale sale : sales) {
            if (sale.getDiscountType() != DiscountType.BUY_X_GET_Y) continue;
            if (sale.getStartDate().isAfter(java.time.LocalDateTime.now()) ||
                    sale.getEndDate().isBefore(java.time.LocalDateTime.now())) continue;

            for (int productId : sale.getProductIds()) {
                int totalQty = paidQuantities.getOrDefault(productId, 0);
                int buy = sale.getBuyQuantity();
                int get = sale.getGetQuantity();

                int fullSets = totalQty / (buy + get);
                int actualPaidQty = fullSets * buy;
                int actualFreeQty = fullSets * get;

                double unitPrice = realPrices.getOrDefault(productId, 0.0);
                totalDiscount += unitPrice * actualFreeQty;

                freeQuantities.put(productId, actualFreeQty);
                paidQuantities.put(productId, totalQty - actualFreeQty);
            }
        }

        // Step 3: BUNDLE
        for (Sale sale : sales) {
            if (sale.getDiscountType() != DiscountType.BUNDLE) continue;
            if (sale.getStartDate().isAfter(java.time.LocalDateTime.now()) ||
                    sale.getEndDate().isBefore(java.time.LocalDateTime.now())) continue;

            List<Integer> bundleProducts = sale.getProductIds();
            int bundleSets = Integer.MAX_VALUE;

            for (int productId : bundleProducts) {
                int paidQty = paidQuantities.getOrDefault(productId, 0);
                bundleSets = Math.min(bundleSets, paidQty);
            }

            if (bundleSets > 0 && bundleSets != Integer.MAX_VALUE) {
                totalDiscount += bundleSets * sale.getDiscountValue();
            }
        }

        return totalDiscount;
    }


    @Override
    public String toString() {
        return "Sale{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", discountType=" + discountType +
                ", discountValue=" + discountValue +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", buyQuantity=" + buyQuantity +
                ", getQuantity=" + getQuantity +
                ", productIds=" + productIds +
                '}';
    }

}

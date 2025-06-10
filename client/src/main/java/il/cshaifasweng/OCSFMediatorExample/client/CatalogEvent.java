package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import java.util.List;

public class CatalogEvent {
    private final List<Product> products;

    public CatalogEvent(List<Product> products) {
        this.products = products;
    } // Takes a list and then constructs it into individual products

    public List<Product> getProducts() {
        return products;
    }
}
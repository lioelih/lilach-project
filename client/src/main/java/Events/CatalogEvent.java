package Events;

import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import java.util.List;

public class CatalogEvent {
    private final String use_case;
    private final List<Product> products;

    public CatalogEvent(String use_case, List<Product> products) {
        this.use_case = use_case;
        this.products = products;
    } // Takes a list and then constructs it into individual products

    public List<Product> getProducts() {
        return products;
    }
    public String getUse_case() {return use_case;}
}
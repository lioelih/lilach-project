package Events;

import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import java.util.List;

public class SalesEvent {
    private final String useCase;
    private final List<Sale> sales;

    public SalesEvent(String useCase, List<Sale> sales) {
        this.useCase = useCase;
        this.sales = sales;
    }

    public String getUseCase() {
        return useCase;
    }

    public List<Sale> getSales() {
        return sales;
    }
}

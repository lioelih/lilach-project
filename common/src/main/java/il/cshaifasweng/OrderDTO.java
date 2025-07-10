package il.cshaifasweng;

import java.io.Serializable;
import java.util.List;

public class OrderDTO implements Serializable {
    private final String username;
    private final List<Integer> basketIds;
    private final String fulfilType;
    private final String fulfilInfo;

    public OrderDTO(String username, List<Integer> basketIds,
                    String fulfilType, String fulfilInfo) {
        this.username = username;
        this.basketIds = basketIds;
        this.fulfilType = fulfilType;
        this.fulfilInfo = fulfilInfo;
    }

    public String getUsername() { return username; }
    public List<Integer> getBasketIds() { return basketIds; }
    public String getFulfilType() { return fulfilType; }
    public String getFulfilInfo() { return fulfilInfo; }
}

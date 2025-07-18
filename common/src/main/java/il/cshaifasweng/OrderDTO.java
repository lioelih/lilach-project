package il.cshaifasweng;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDTO implements Serializable {
    private final String username;
    private final List<Integer> basketIds;
    private final String fulfilType;
    private final String fulfilInfo;
    private LocalDateTime deadline;

    public OrderDTO(String username, List<Integer> basketIds,
                    String fulfilType, String fulfilInfo, LocalDateTime deadline) {
        this.username = username;
        this.basketIds = basketIds;
        this.fulfilType = fulfilType;
        this.fulfilInfo = fulfilInfo;
        this.deadline   = deadline;
    }

    public String getUsername() { return username; }
    public List<Integer> getBasketIds() { return basketIds; }
    public String getFulfilType() { return fulfilType; }
    public String getFulfilInfo() { return fulfilInfo; }
    public LocalDateTime getDeadline() { return deadline; }
}

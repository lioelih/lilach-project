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
    private final String recipient;   // non‑null
    private final String greeting;    // nullable
    private final boolean useCompensation;
    private final double compensationToUse;
    public OrderDTO(String username, List<Integer> basketIds,
                    String fulfilType, String fulfilInfo, LocalDateTime deadline, String recipient,
                    String greeting, boolean useCompensation,
                    double compensationToUse) {
        this.username = username;
        this.basketIds = basketIds;
        this.fulfilType = fulfilType;
        this.fulfilInfo = fulfilInfo;
        this.deadline   = deadline;
        this.recipient = recipient;
        this.greeting  = greeting;
        this.useCompensation    = useCompensation;
        this.compensationToUse = compensationToUse;
    }

    public String getUsername() { return username; }
    public List<Integer> getBasketIds() { return basketIds; }
    public String getFulfilType() { return fulfilType; }
    public String getFulfilInfo() { return fulfilInfo; }
    public LocalDateTime getDeadline() { return deadline; }
    public String getRecipient() { return recipient; }
    public String getGreeting()  { return greeting;  }
    public boolean isUseCompensation()    { return useCompensation;    }
    public double  getCompensationToUse() { return compensationToUse; }
}

// in common/src/main/java/il/cshaifasweng/CustomBouquetDTO.java
package il.cshaifasweng;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class CustomBouquetDTO implements Serializable {
    private final Integer id;
    private final String username;
    private String name;
    private final String style;
    private final String dominantColor;
    private final String pot;
    private final double totalPrice;
    private final LocalDateTime createdAt;
    private final List<CustomBouquetItemDTO> items;

    public CustomBouquetDTO(
            Integer id,
            String username,
            String name,
            String style,
            String dominantColor,
            String pot,
            double totalPrice,
            LocalDateTime createdAt,
            List<CustomBouquetItemDTO> items
    ) {
        this.id            = id;
        this.username      = username;
        this.name = name;
        this.style         = style;
        this.dominantColor = dominantColor;
        this.pot           = pot;
        this.totalPrice    = totalPrice;
        this.createdAt     = createdAt;
        this.items         = items;
    }

    public Integer getId()            { return id; }
    public String  getUsername()      { return username; }
    public String  getStyle()         { return style; }
    public String  getDominantColor() { return dominantColor; }
    public String  getPot()           { return pot; }
    public double  getTotalPrice()    { return totalPrice; }
    public List<CustomBouquetItemDTO> getItems() { return items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public CustomBouquetItemDTO getItemById(Integer id) { return items.get(id); }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

}

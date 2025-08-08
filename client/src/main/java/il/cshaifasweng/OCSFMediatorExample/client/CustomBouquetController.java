package il.cshaifasweng.OCSFMediatorExample.client;

import Events.CatalogEvent;
import Events.SalesEvent;
import il.cshaifasweng.CustomBouquetDTO;
import il.cshaifasweng.CustomBouquetItemDTO;
import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import ui.ToggleFlower;

import java.io.IOException;
import java.util.*;

/**
 * CustomBouquetController now supports sale‐adjusted pricing on both
 * flowers and pots, and only rebuilds its UI once both the catalog
 * and the sales list have arrived from the server.
 */
public class CustomBouquetController {

    // ─── FXML Injected Controls ───────────────────────────────────────────────
    @FXML private ImageView        logoImage;
    @FXML private Button           backButton;
    @FXML private Button           addToBasketButton;
    @FXML private ComboBox<String> existingCombo;
    @FXML private VBox             newPane;
    @FXML private Label            limitLabel;

    @FXML private ComboBox<String> styleBox;
    @FXML private ComboBox<String> potBox;
    @FXML private FlowPane         flowerPane;
    @FXML private TextField        nameField;
    @FXML private Button           saveButton;
    @FXML private Label            totalPriceLabel;

    // ─── Internal State ──────────────────────────────────────────────────────
    private final Map<ToggleFlower,Integer> selections    = new LinkedHashMap<>();
    private final Map<String,Product>        potProductMap = new HashMap<>();
    private final Map<String,Double>         potPriceMap   = new HashMap<>();

    private List<CustomBouquetDTO> myBouquets      = List.of();
    private List<Product>          catalogProducts = List.of();
    private List<Sale>             sales           = List.of();
    private Integer                currentCustomId;

    // Flags to know when we have both pieces of data:
    private boolean haveCatalog = false;
    private boolean haveSales   = false;

    @FXML
    public void initialize() {
        // Logo
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));

        // Flower grid styling
        flowerPane.setAlignment(Pos.CENTER);
        flowerPane.setHgap(8);
        flowerPane.setVgap(8);

        // Limit label
        limitLabel.setText("Up to 50 flowers allowed in total per Custom Bouquet");
        limitLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #555;");

        // “My Bouquets” dropdown
        existingCombo.getItems().add("Create New…");
        existingCombo.getSelectionModel().selectFirst();
        existingCombo.setOnAction(this::onExistingSelected);

        // Style selector
        styleBox.getItems().setAll("Bridal", "Tidy", "Clustered");
        styleBox.valueProperty().addListener((o,ov,nv) -> updateSaveEnabled());

        // Pot selector
        potBox.getItems().setAll("None");
        potBox.getSelectionModel().select("None");
        potBox.valueProperty().addListener((o,ov,nv) -> {
            updateSaveEnabled();
            updatePrice();
        });

        // Show the “new” pane
        newPane.setVisible(true);

        // Register for both CatalogEvent & SalesEvent
        EventBus.getDefault().register(this);

        // Fire off the three requests
        try {
            var client = SimpleClient.getClient();
            client.ensureConnected();
            client.sendToServer(new Msg("LIST_CUSTOM_BOUQUETS", SceneController.loggedUsername));
            client.sendToServer(new Msg("GET_CATALOG",        null));
            client.sendToServer(new Msg("GET_SALES",          null));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // initial UI state
        updateSaveEnabled();
        addToBasketButton.setDisable(true);
    }

    // ─── 1) Receive list of existing custom bouquets ─────────────────────────
    @Subscribe @SuppressWarnings("unchecked")
    public void onCustomList(Msg m) {
        if (!"LIST_CUSTOM_BOUQUETS_OK".equals(m.getAction())) return;
        myBouquets = (List<CustomBouquetDTO>) m.getData();
        Platform.runLater(() -> {
            existingCombo.getItems().setAll("Create New…");
            for (var dto : myBouquets) {
                existingCombo.getItems().add(dto.getId() + ": " + dto.getStyle());
            }
            existingCombo.getSelectionModel().selectFirst();
        });
    }

    // ─── 2) Receive the catalog ───────────────────────────────────────────────
    @Subscribe
    public void onCatalog(CatalogEvent event) {
        catalogProducts = event.getProducts();
        haveCatalog = true;
        System.out.println(">>> onCatalog(): got "
                + catalogProducts.size()
                + " catalog items, haveSales=" + haveSales
        );
        Platform.runLater(() -> {
            flowerPane.getChildren().clear();
            selections.clear();
            catalogProducts.stream()
                    .filter(p -> "flower".equalsIgnoreCase(p.getType()))
                    .forEach(p -> {
                        ToggleFlower tf = new ToggleFlower(p);
                        tf.setOnToggled(on -> {
                            if (on) {
                                int used = selections.values().stream().mapToInt(i -> i).sum();
                                int rem  = Math.max(0, 50 - used);
                                selections.put(tf, 1);
                                tf.showQuantitySelector(1, 1 + rem, qty -> {
                                    selections.put(tf, qty);
                                    updateSaveEnabled();
                                    updateQuantityLimits();
                                    updatePrice();
                                });
                            } else {
                                selections.remove(tf);
                                tf.removeQuantitySelector();
                            }
                            updateSaveEnabled();
                            updateQuantityLimits();
                            updatePrice();
                        });
                        flowerPane.getChildren().add(tf);
                    });
            tryRebuildPotBox();
        });
    }


    // ─── 3) Receive the sales list ───────────────────────────────────────────
    @Subscribe
    public void onSales(SalesEvent e) {
        if (!"SENT_SALES".equals(e.getUseCase())
                && !"SALE_ADDED".equals(e.getUseCase())
                && !"SALE_UPDATED".equals(e.getUseCase())
                && !"SALE_DELETED".equals(e.getUseCase())
        ) return;

        sales     = e.getSales();
        haveSales = true;
        System.out.println(">>> onSales(): got "
                + sales.size()
                + " sales, haveCatalog=" + haveCatalog
        );
        tryRebuildPotBox();
    }


    // ─── Only rebuild flower grid once both catalog & sales are present ────
    private void tryRebuildFlowerGrid() {
        System.out.println(">>> tryRebuildFlowerGrid(): haveCatalog=" +
                haveCatalog + " haveSales=" + haveSales);
        if (!haveCatalog || !haveSales) return;
        Platform.runLater(this::rebuildFlowerGrid);
    }

    // ─── Only rebuild potBox once both catalog & sales are present ──────────
    private void tryRebuildPotBox() {
        System.out.println(">>> tryRebuildPotBox(): haveCatalog="
                + haveCatalog + " haveSales=" + haveSales
        );
        if (!haveCatalog || !haveSales) return;
        Platform.runLater(() -> {
            System.out.println(">>> actually rebuilding potBox…");
            rebuildPotBox();
            updatePrice();
        });
    }

    // ─── Rebuild flower palette with discounted prices ───────────────────────
    private void rebuildFlowerGrid() {
        flowerPane.getChildren().clear();
        selections.clear();
        for (Product p : catalogProducts) {
            if (!"flower".equalsIgnoreCase(p.getType())) continue;

            ToggleFlower tf = new ToggleFlower(p);
            tf.setOnToggled(on -> {
                if (on) {
                    int used = selections.values().stream().mapToInt(i -> i).sum();
                    int rem  = Math.max(0, 50 - used);
                    selections.put(tf, 1);
                    tf.showQuantitySelector(1, 1 + rem, qty -> {
                        selections.put(tf, qty);
                        updateSaveEnabled();
                        updateQuantityLimits();
                        updatePrice();
                    });
                } else {
                    selections.remove(tf);
                    tf.removeQuantitySelector();
                }
                updateSaveEnabled();
                updateQuantityLimits();
                updatePrice();
            });
            flowerPane.getChildren().add(tf);
        }
    }

    // ─── Rebuild pot/vase dropdown with discounted prices ────────────────────
    private void rebuildPotBox() {
        String prev = potBox.getValue();
        String prevName = (prev != null && !"None".equals(prev) && prev.contains(" (+"))
                ? prev.substring(0, prev.indexOf(" (+"))
                : null;

        potProductMap.clear();
        potPriceMap.clear();
        potBox.getItems().setAll("None");

        for (Product p : catalogProducts) {
            String t = p.getType().toLowerCase();
            if ("pot".equals(t) || "vase".equals(t)) {
                double finalPrice = Sale.getDiscountedPrice(p, sales);
                double base       = p.getPrice();
                double discount   = base - finalPrice;

                String priceStr = String.format("₪%.2f", finalPrice);
                String label    = p.getName()
                        + " (+" + priceStr
                        + (discount > 0 ? " after sale!)" : ")");

                System.out.println("    • adding pot label: " + label);
                potProductMap.put(label, p);
                potPriceMap.put(label, finalPrice);
                potBox.getItems().add(label);
            }
        }

        if (prevName != null) {
            potProductMap.keySet().stream()
                    .filter(lbl -> lbl.startsWith(prevName))
                    .findFirst()
                    .ifPresent(lbl -> potBox.getSelectionModel().select(lbl));
        } else {
            potBox.getSelectionModel().select("None");
        }
    }

    // ─── Handle switching between “Create New…” & existing bouquets ──────────
    private void onExistingSelected(ActionEvent evt) {
        int idx       = existingCombo.getSelectionModel().getSelectedIndex();
        boolean isNew = idx <= 0;

        newPane.setVisible(true);
        saveButton.setDisable(true);
        selections.clear();
        currentCustomId = null;

        // clear any selected toggles
        flowerPane.getChildren().stream()
                .filter(n -> n instanceof ToggleFlower)
                .map(n -> (ToggleFlower) n)
                .forEach(ToggleFlower::deselect);

        if (isNew) {
            nameField.clear();
            styleBox.getSelectionModel().clearSelection();
            potBox.getSelectionModel().select("None");
            addToBasketButton.setDisable(true);
        } else {
            var dto = myBouquets.get(idx - 1);
            currentCustomId = dto.getId();
            nameField.setText(dto.getName());
            styleBox.setValue(dto.getStyle());
            addToBasketButton.setDisable(false);

            // restore pot
            if (dto.getPot() == null) {
                potBox.getSelectionModel().select("None");
            } else {
                potProductMap.keySet().stream()
                        .filter(lbl -> lbl.startsWith(dto.getPot()))
                        .findFirst()
                        .ifPresentOrElse(
                                lbl -> potBox.getSelectionModel().select(lbl),
                                ()  -> potBox.getSelectionModel().select("None")
                        );
            }

            // restore flower quantities
            for (var item : dto.getItems()) {
                flowerPane.getChildren().stream()
                        .filter(n -> n instanceof ToggleFlower)
                        .map(n -> (ToggleFlower) n)
                        .filter(tf -> tf.getProduct().getId() == item.getProductId())
                        .findFirst()
                        .ifPresent(tf -> {
                            tf.setSelectedQuantity(item.getQuantity());
                            selections.put(tf, item.getQuantity());
                        });
            }

            updateSaveEnabled();
            updateQuantityLimits();
            updatePrice();
        }
    }

    // ─── Helpers for UI state ────────────────────────────────────────────────
    private void updateSaveEnabled() {
        boolean can = styleBox.getValue() != null && !selections.isEmpty();
        saveButton.setDisable(!can);
    }

    private void updateQuantityLimits() {
        int used = selections.values().stream().mapToInt(i -> i).sum();
        int rem  = Math.max(0, 50 - used);

        flowerPane.getChildren().stream()
                .filter(n -> n instanceof ToggleFlower)
                .map(n -> (ToggleFlower) n)
                .filter(ToggleFlower::isSelected)
                .map(ToggleFlower::getSpinner)
                .filter(Objects::nonNull)
                .forEach(sp -> {
                    var vf = (SpinnerValueFactory.IntegerSpinnerValueFactory) sp.getValueFactory();
                    vf.setMax(sp.getValue() + rem);
                });

        boolean soldOut = (rem == 0);
        flowerPane.getChildren().stream()
                .filter(n -> n instanceof ToggleFlower)
                .map(n -> (ToggleFlower) n)
                .filter(tf -> !tf.isSelected())
                .forEach(tf -> tf.getToggle().setDisable(soldOut));
    }

    private void updatePrice() {
        double sum = selections.entrySet().stream()
                .mapToDouble(e -> {
                    Product prod    = e.getKey().getProduct();
                    double unitDisc = Sale.getDiscountedPrice(prod, sales);
                    System.out.println("    + flower " + prod.getName()
                            + " at discounted unitPrice=" + unitDisc
                            + " × qty=" + e.getValue());
                    return unitDisc * e.getValue();
                })
                .sum();

        String lbl = potBox.getValue();
        if (!"None".equals(lbl)) {
            sum += potPriceMap.getOrDefault(lbl, 0.0);
        }

        totalPriceLabel.setText(String.format("Total Price: ₪%.2f", sum));
    }

    // ─── Save / Add to Basket / Back ─────────────────────────────────────────
    @FXML private void onSave(ActionEvent evt) {
        String name  = nameField.getText().trim();
        String style = styleBox.getValue();
        String lbl   = potBox.getValue();
        String pot   = "None".equals(lbl)
                ? null
                : lbl.substring(0, lbl.indexOf(" (+"));

        double price = selections.entrySet().stream()
                .mapToDouble(e -> e.getKey().getProduct().getPrice() * e.getValue())
                .sum();
        if (!"None".equals(lbl)) {
            price += potPriceMap.getOrDefault(lbl, 0.0);
        }

        var dto = new CustomBouquetDTO(
                currentCustomId,
                SceneController.loggedUsername,
                name, style, "",
                pot, price, null,
                selections.entrySet().stream()
                        .map(e -> new CustomBouquetItemDTO(
                                currentCustomId,
                                e.getKey().getProduct().getId(),
                                e.getValue()))
                        .toList()
        );

        try {
            String action = (currentCustomId == null)
                    ? "CREATE_CUSTOM_BOUQUET"
                    : "UPDATE_CUSTOM_BOUQUET";
            SimpleClient.getClient().sendToServer(new Msg(action, dto));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        var alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Saved Bouquet! Add it to basket now?",
                ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(bt -> {
            addToBasketButton.setDisable(false);
            if (bt == ButtonType.YES) onAddToBasket(null);
        });
    }

    @Subscribe public void onCustomCreated(Msg m) {
        if ("CREATE_CUSTOM_BOUQUET_OK".equals(m.getAction())) {
            currentCustomId = (Integer)m.getData();
        }
    }
    @Subscribe public void onCustomUpdated(Msg m) {
        if ("UPDATE_CUSTOM_BOUQUET_OK".equals(m.getAction())) {
            currentCustomId = (Integer)m.getData();
            Platform.runLater(() -> {
                int idx = existingCombo.getSelectionModel().getSelectedIndex();
                existingCombo.getItems().set(idx,
                        currentCustomId + ": " + nameField.getText()
                );
            });
        }
    }

    @FXML private void onAddToBasket(ActionEvent evt) {
        if (currentCustomId == null) return;
        try {
            SimpleClient.getClient()
                    .sendToServer(new Msg("ADD_CUSTOM_TO_BASKET",
                            new Object[]{SceneController.loggedUsername, currentCustomId, 1}));
            SimpleClient.getClient()
                    .sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername));
            new Alert(Alert.AlertType.INFORMATION,
                    "Added \"" + nameField.getText() + "\" to basket!")
                    .showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML private void onBack(ActionEvent evt) {
        EventBus.getDefault().unregister(this);
        ((Stage)backButton.getScene().getWindow()).close();
    }
}

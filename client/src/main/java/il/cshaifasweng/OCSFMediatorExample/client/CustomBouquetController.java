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

public class CustomBouquetController {

    @FXML private ImageView logoImage;
    @FXML private Button backButton;
    @FXML private Button addToBasketButton;
    @FXML private ComboBox<String> existingCombo;
    @FXML private VBox newPane;
    @FXML private Label limitLabel;

    @FXML private ComboBox<String> styleBox;
    @FXML private FlowPane flowerPane;
    @FXML private FlowPane potPane;
    @FXML private TextField nameField;
    @FXML private Button saveButton;
    @FXML private Label totalPriceLabel;
    @FXML private ImageView stylePreview;

    private final Map<Integer,Integer> selections = new LinkedHashMap<>();
    private final Map<ToggleFlower, Label> flowerPriceLabels = new HashMap<>();
    private final Map<Integer, ToggleFlower> flowerIndex = new HashMap<>();
    private final Map<Integer, VBox> flowerCells = new HashMap<>();

    private final Map<ToggleFlower, Label> potPriceLabels = new HashMap<>();
    private final Map<Integer, ToggleFlower> potIndex = new HashMap<>();
    private final Map<Integer, VBox> potCells = new HashMap<>();
    private Integer selectedPotId = null;

    private List<CustomBouquetDTO> myBouquets = List.of();
    private List<Product> catalogProducts = List.of();
    private List<Sale> sales = List.of();
    private Integer currentCustomId;

    @FXML
    public void initialize() {
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));
        flowerPane.setAlignment(Pos.CENTER);
        flowerPane.setHgap(8);
        flowerPane.setVgap(8);
        potPane.setAlignment(Pos.CENTER);
        potPane.setHgap(8);
        potPane.setVgap(8);

        limitLabel.setText("Up to 50 flowers allowed in total per Custom Bouquet");
        limitLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #555;");

        existingCombo.getItems().add("Create New…");
        existingCombo.getSelectionModel().selectFirst();
        existingCombo.setOnAction(this::onExistingSelected);

        styleBox.getItems().setAll("Bridal", "Tidy", "Clustered");
        styleBox.valueProperty().addListener((o,ov,nv) -> {
            updateSaveEnabled();
            updateStylePreview(nv);
        });
        updateStylePreview(styleBox.getValue());

        newPane.setVisible(true);

        EventBus.getDefault().register(this);

        try {
            var client = SimpleClient.getClient();
            client.ensureConnected();
            client.sendToServer(new Msg("LIST_CUSTOM_BOUQUETS", SceneController.loggedUsername));
            client.sendToServer(new Msg("GET_CATALOG", null));
            client.sendToServer(new Msg("GET_SALES", null));
        } catch (IOException e) {
            e.printStackTrace();
        }

        updateSaveEnabled();
        addToBasketButton.setDisable(true);
    }

    @Subscribe @SuppressWarnings("unchecked")
    public void onCustomList(Msg m) {
        if (!"LIST_CUSTOM_BOUQUETS_OK".equals(m.getAction())) return;
        myBouquets = (List<CustomBouquetDTO>) m.getData();
        Platform.runLater(() -> {
            existingCombo.getItems().setAll("Create New…");
            for (var dto : myBouquets) existingCombo.getItems().add(dto.getId() + ": " + dto.getStyle());
            existingCombo.getSelectionModel().selectFirst();
        });
    }

    @Subscribe
    public void onCatalog(CatalogEvent event) {
        List<Product> incoming = event.getProducts();
        catalogProducts = incoming;

        Platform.runLater(() -> {
            Set<Integer> incomingFlowerIds = new HashSet<>();
            Set<Integer> incomingPotIds = new HashSet<>();
            for (Product p : incoming) {
                if (p.getType() == null) continue;
                String t = p.getType().toLowerCase();
                if ("flower".equals(t)) incomingFlowerIds.add(p.getId());
                else if ("pot".equals(t) || "vase".equals(t)) incomingPotIds.add(p.getId());
            }

            if (flowerIndex.isEmpty() && potIndex.isEmpty()) {
                flowerPane.getChildren().clear();
                potPane.getChildren().clear();
                for (Product p : incoming) {
                    String t = p.getType() == null ? "" : p.getType().toLowerCase();
                    if ("flower".equals(t)) upsertFlower(p);
                    else if ("pot".equals(t) || "vase".equals(t)) upsertPot(p);
                }
                updateAllFlowerPrices();
                updateAllPotPrices();
                updatePrice();
                return;
            }

            Set<Integer> existingFlowerIds = new HashSet<>(flowerIndex.keySet());
            if (existingFlowerIds.equals(incomingFlowerIds)) {
                for (Product p : incoming) if ("flower".equalsIgnoreCase(p.getType())) upsertFlower(p);
            } else {
                for (Integer id : new ArrayList<>(flowerIndex.keySet())) {
                    if (!incomingFlowerIds.contains(id)) {
                        VBox cell = flowerCells.remove(id);
                        if (cell != null) flowerPane.getChildren().remove(cell);
                        flowerIndex.remove(id);
                        selections.remove(id);
                    }
                }
                Map<Integer, Product> byId = new HashMap<>();
                for (Product p : incoming) if ("flower".equalsIgnoreCase(p.getType())) byId.put(p.getId(), p);
                for (Integer id : incomingFlowerIds) upsertFlower(byId.get(id));
            }

            Set<Integer> existingPotIds = new HashSet<>(potIndex.keySet());
            if (existingPotIds.equals(incomingPotIds)) {
                for (Product p : incoming) {
                    if (p.getType() == null) continue;
                    String t = p.getType().toLowerCase();
                    if ("pot".equals(t) || "vase".equals(t)) upsertPot(p);
                }
            } else {
                for (Integer id : new ArrayList<>(potIndex.keySet())) {
                    if (!incomingPotIds.contains(id)) {
                        VBox cell = potCells.remove(id);
                        if (cell != null) potPane.getChildren().remove(cell);
                        potIndex.remove(id);
                        if (Objects.equals(selectedPotId, id)) selectedPotId = null;
                    }
                }
                Map<Integer, Product> byId = new HashMap<>();
                for (Product p : incoming) {
                    if (p.getType() == null) continue;
                    String t = p.getType().toLowerCase();
                    if ("pot".equals(t) || "vase".equals(t)) byId.put(p.getId(), p);
                }
                for (Integer id : incomingPotIds) upsertPot(byId.get(id));
            }

            updateQuantityLimits();
            updateAllFlowerPrices();
            updateAllPotPrices();
            updatePrice();
        });
    }

    @Subscribe
    public void onSales(SalesEvent e) {
        if (!"SENT_SALES".equals(e.getUseCase())
                && !"SALE_ADDED".equals(e.getUseCase())
                && !"SALE_UPDATED".equals(e.getUseCase())
                && !"SALE_DELETED".equals(e.getUseCase())) return;

        sales = e.getSales();
        Platform.runLater(() -> {
            updateAllFlowerPrices();
            updateAllPotPrices();
            updatePrice();
        });
    }

    private void onExistingSelected(ActionEvent evt) {
        int idx = existingCombo.getSelectionModel().getSelectedIndex();
        boolean isNew = idx <= 0;

        newPane.setVisible(true);
        saveButton.setDisable(true);
        selections.clear();
        currentCustomId = null;

        flowerPane.getChildren().stream()
                .filter(n -> n instanceof ToggleFlower || (n instanceof VBox v && v.getChildren().get(0) instanceof ToggleFlower))
                .forEach(n -> {
                    ToggleFlower tf = (n instanceof ToggleFlower) ? (ToggleFlower) n : (ToggleFlower) ((VBox) n).getChildren().get(0);
                    tf.deselect();
                    updateFlowerItemPrice(tf);
                });

        potPane.getChildren().stream()
                .filter(n -> n instanceof ToggleFlower || (n instanceof VBox v && v.getChildren().get(0) instanceof ToggleFlower))
                .forEach(n -> {
                    ToggleFlower tf = (n instanceof ToggleFlower) ? (ToggleFlower) n : (ToggleFlower) ((VBox) n).getChildren().get(0);
                    tf.deselect();
                    updatePotItemPrice(tf);
                });
        selectedPotId = null;

        if (isNew) {
            nameField.clear();
            styleBox.getSelectionModel().clearSelection();
            updateStylePreview(null);
            addToBasketButton.setDisable(true);
        } else {
            var dto = myBouquets.get(idx - 1);
            currentCustomId = dto.getId();
            nameField.setText(dto.getName());
            styleBox.setValue(dto.getStyle());
            updateStylePreview(dto.getStyle());
            addToBasketButton.setDisable(false);

            if (dto.getPot() != null) {
                potPane.getChildren().stream()
                        .filter(n -> n instanceof VBox && ((VBox) n).getChildren().get(0) instanceof ToggleFlower)
                        .map(n -> (ToggleFlower) ((VBox) n).getChildren().get(0))
                        .filter(tf -> tf.getProduct().getName().equals(dto.getPot()))
                        .findFirst()
                        .ifPresent(tf -> {
                            tf.getToggle().setSelected(true);
                            selectedPotId = tf.getProduct().getId();
                            updatePotItemPrice(tf);
                        });
            }

            for (var item : dto.getItems()) {
                flowerPane.getChildren().stream()
                        .filter(n -> n instanceof VBox && ((VBox) n).getChildren().get(0) instanceof ToggleFlower)
                        .map(n -> (ToggleFlower) ((VBox) n).getChildren().get(0))
                        .filter(tf -> tf.getProduct().getId() == item.getProductId())
                        .findFirst()
                        .ifPresent(tf -> {
                            tf.setSelectedQuantity(item.getQuantity());
                            selections.put(tf.getProduct().getId(), item.getQuantity());
                            updateFlowerItemPrice(tf);
                        });
            }

            updateSaveEnabled();
            updateQuantityLimits();
            updatePrice();
        }
    }

    private void updateSaveEnabled() {
        boolean can = styleBox.getValue() != null && !selections.isEmpty();
        saveButton.setDisable(!can);
    }

    private void updateQuantityLimits() {
        int used = selections.values().stream().mapToInt(i -> i).sum();
        int rem = Math.max(0, 50 - used);

        flowerPane.getChildren().stream()
                .filter(n -> n instanceof VBox && ((VBox) n).getChildren().get(0) instanceof ToggleFlower)
                .map(n -> (ToggleFlower) ((VBox) n).getChildren().get(0))
                .filter(tf -> tf.getToggle().isSelected())
                .map(ToggleFlower::getSpinner)
                .filter(Objects::nonNull)
                .forEach(sp -> {
                    var vf = (SpinnerValueFactory.IntegerSpinnerValueFactory) sp.getValueFactory();
                    vf.setMax(sp.getValue() + rem);
                });

        boolean soldOut = (rem == 0);
        flowerPane.getChildren().stream()
                .filter(n -> n instanceof VBox && ((VBox) n).getChildren().get(0) instanceof ToggleFlower)
                .map(n -> (ToggleFlower) ((VBox) n).getChildren().get(0))
                .filter(tf -> !tf.getToggle().isSelected())
                .forEach(tf -> tf.getToggle().setDisable(soldOut));
    }

    private void updatePrice() {
        double sum = 0.0;
        for (Map.Entry<Integer,Integer> e : selections.entrySet()) {
            ToggleFlower tf = flowerIndex.get(e.getKey());
            if (tf == null) continue;
            Product prod = tf.getProduct();
            double unitDisc = Sale.getDiscountedPrice(prod, sales);
            sum += unitDisc * e.getValue();
        }
        if (selectedPotId != null) {
            ToggleFlower ptf = potIndex.get(selectedPotId);
            if (ptf != null) {
                double potPrice = Sale.getDiscountedPrice(ptf.getProduct(), sales);
                sum += potPrice;
            }
        }
        totalPriceLabel.setText(String.format("Total Price: ₪%.2f", sum));
    }

    private void upsertFlower(Product p) {
        if (p == null || p.getType() == null || !"flower".equalsIgnoreCase(p.getType())) return;
        int id = p.getId();
        ToggleFlower tf = flowerIndex.get(id);
        if (tf == null) {
            tf = new ToggleFlower(p);
            Label priceHint = new Label();
            priceHint.setStyle("-fx-text-fill:#0f172a; -fx-font-size:12; -fx-font-weight:bold;");
            priceHint.setVisible(false);
            priceHint.setManaged(false);
            flowerPriceLabels.put(tf, priceHint);

            final int pid = id;
            final ToggleFlower tfx = tf;
            tf.setOnToggled(on -> {
                if (on) {
                    int used = selections.values().stream().mapToInt(i -> i).sum();
                    int rem = Math.max(0, 50 - used);
                    selections.put(pid, 1);
                    tfx.showQuantitySelector(1, 1 + rem, qty -> {
                        selections.put(pid, qty);
                        updateSaveEnabled();
                        updateQuantityLimits();
                        updateFlowerItemPrice(tfx);
                        updatePrice();
                    });
                    updateFlowerItemPrice(tfx);
                } else {
                    selections.remove(pid);
                    tfx.removeQuantitySelector();
                    updateFlowerItemPrice(tfx);
                }
                updateSaveEnabled();
                updateQuantityLimits();
                updatePrice();
            });

            VBox cell = new VBox(6, tf, priceHint);
            cell.setAlignment(Pos.CENTER);
            flowerIndex.put(id, tf);
            flowerCells.put(id, cell);
            flowerPane.getChildren().add(cell);

            Integer prevQty = selections.get(id);
            if (prevQty != null && prevQty > 0) {
                tf.setSelectedQuantity(prevQty);
                updateFlowerItemPrice(tf);
            }
        } else {
            tf.getProduct().updateProduct(p.getName(), p.getType(), p.getPrice(), p.getImage());
            if (selections.getOrDefault(id, 0) > 0) updateFlowerItemPrice(tf);
        }
    }

    private void upsertPot(Product p) {
        if (p == null || p.getType() == null) return;
        String t = p.getType().toLowerCase();
        if (!"pot".equals(t) && !"vase".equals(t)) return;

        int id = p.getId();
        ToggleFlower tf = potIndex.get(id);
        if (tf == null) {
            tf = new ToggleFlower(p);
            Label priceHint = new Label();
            priceHint.setStyle("-fx-text-fill:#0f172a; -fx-font-size:12; -fx-font-weight:bold;");
            priceHint.setVisible(true);
            priceHint.setManaged(true);
            potPriceLabels.put(tf, priceHint);

            final int pid = id;
            final ToggleFlower self = tf;
            tf.setOnToggled(on -> {
                if (on) {
                    for (ToggleFlower other : potIndex.values()) {
                        if (other != self && other.getToggle().isSelected()) other.getToggle().setSelected(false);
                    }
                    selectedPotId = pid;
                } else {
                    if (Objects.equals(selectedPotId, pid)) selectedPotId = null;
                }
                updatePotItemPrice(self);
                updatePrice();
            });


            VBox cell = new VBox(4, tf, priceHint);
            cell.setAlignment(Pos.CENTER);
            potIndex.put(id, tf);
            potCells.put(id, cell);
            potPane.getChildren().add(cell);

            if (Objects.equals(selectedPotId, id)) {
                tf.getToggle().setSelected(true);
                updatePotItemPrice(tf);
            } else {
                updatePotItemPrice(tf);
            }
        } else {
            tf.getProduct().updateProduct(p.getName(), p.getType(), p.getPrice(), p.getImage());
            updatePotItemPrice(tf);
            if (tf.getToggle().isSelected()) updatePrice();
        }
    }

    private void updateFlowerItemPrice(ToggleFlower tf) {
        if (tf == null) return;
        Label l = flowerPriceLabels.get(tf);
        if (l == null) return;
        Integer qty = selections.get(tf.getProduct().getId());
        if (qty == null || qty <= 0) {
            l.setText("");
            l.setVisible(false);
            l.setManaged(false);
            return;
        }
        double unitDisc = Sale.getDiscountedPrice(tf.getProduct(), sales);
        double total = unitDisc * qty;
        l.setText(String.format("+ ₪%.2f (x%d)", total, qty));
        l.setVisible(true);
        l.setManaged(true);
    }

    private void updatePotItemPrice(ToggleFlower tf) {
        if (tf == null) return;
        Label l = potPriceLabels.get(tf);
        if (l == null) return;
        double price = Sale.getDiscountedPrice(tf.getProduct(), sales);
        boolean selected = tf.getToggle().isSelected();
        l.setText(selected ? String.format("+ ₪%.2f", price) : String.format("₪%.2f", price));
        l.setVisible(true);
        l.setManaged(true);
    }

    private void updateAllFlowerPrices() {
        for (Map.Entry<Integer, ToggleFlower> e : flowerIndex.entrySet()) updateFlowerItemPrice(e.getValue());
    }

    private void updateAllPotPrices() {
        for (Map.Entry<Integer, ToggleFlower> e : potIndex.entrySet()) updatePotItemPrice(e.getValue());
    }

    @FXML private void onSave(ActionEvent evt) {
        String name = nameField.getText().trim();
        String style = styleBox.getValue();
        String potName = null;
        if (selectedPotId != null) {
            ToggleFlower ptf = potIndex.get(selectedPotId);
            if (ptf != null) potName = ptf.getProduct().getName();
        }

        double price = 0.0;
        for (Map.Entry<Integer,Integer> e : selections.entrySet()) {
            ToggleFlower tf = flowerIndex.get(e.getKey());
            if (tf == null) continue;
            price += tf.getProduct().getPrice() * e.getValue();
        }
        if (selectedPotId != null) {
            ToggleFlower ptf = potIndex.get(selectedPotId);
            if (ptf != null) price += ptf.getProduct().getPrice();
        }

        var items = new ArrayList<CustomBouquetItemDTO>();
        for (Map.Entry<Integer,Integer> e : selections.entrySet()) {
            items.add(new CustomBouquetItemDTO(currentCustomId, e.getKey(), e.getValue()));
        }

        var dto = new CustomBouquetDTO(
                currentCustomId,
                SceneController.loggedUsername,
                name, style, "",
                potName, price, null,
                items
        );

        try {
            String action = (currentCustomId == null) ? "CREATE_CUSTOM_BOUQUET" : "UPDATE_CUSTOM_BOUQUET";
            SimpleClient.getClient().sendToServer(new Msg(action, dto));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        var alert = new Alert(Alert.AlertType.CONFIRMATION, "Saved Bouquet! Add it to basket now?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(bt -> {
            addToBasketButton.setDisable(false);
            if (bt == ButtonType.YES) onAddToBasket(null);
        });
    }

    @Subscribe public void onCustomCreated(Msg m) {
        if ("CREATE_CUSTOM_BOUQUET_OK".equals(m.getAction())) currentCustomId = (Integer)m.getData();
    }

    @Subscribe public void onCustomUpdated(Msg m) {
        if ("UPDATE_CUSTOM_BOUQUET_OK".equals(m.getAction())) {
            currentCustomId = (Integer)m.getData();
            Platform.runLater(() -> {
                int idx = existingCombo.getSelectionModel().getSelectedIndex();
                existingCombo.getItems().set(idx, currentCustomId + ": " + nameField.getText());
            });
        }
    }

    @FXML private void onAddToBasket(ActionEvent evt) {
        if (currentCustomId == null) return;
        try {
            SimpleClient.getClient().sendToServer(new Msg("ADD_CUSTOM_TO_BASKET",
                    new Object[]{SceneController.loggedUsername, currentCustomId, 1}));
            SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername));
            new Alert(Alert.AlertType.INFORMATION, "Added \"" + nameField.getText() + "\" to basket!").showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Subscribe
    public void onProductUpdated(Msg m) {
        if (!"PRODUCT_UPDATED".equals(m.getAction())) return;
        Product upd = (Product) m.getData();
        if (upd == null || upd.getType() == null) return;
        String t = upd.getType().toLowerCase();

        Platform.runLater(() -> {
            boolean found = false;
            List<Product> copy = new ArrayList<>(catalogProducts.size());
            for (Product p : catalogProducts) {
                if (p.getId() == upd.getId()) {
                    p.updateProduct(upd.getName(), upd.getType(), upd.getPrice(), upd.getImage());
                    found = true;
                    copy.add(p);
                } else {
                    copy.add(p);
                }
            }
            if (!found) copy.add(upd);
            catalogProducts = copy;

            if ("flower".equals(t)) {
                upsertFlower(upd);
                updateAllFlowerPrices();
            } else if ("pot".equals(t) || "vase".equals(t)) {
                upsertPot(upd);
                updateAllPotPrices();
            }
            updatePrice();
        });
    }
    private void updateStylePreview(String style) {
        String file = null;
        if ("Bridal".equalsIgnoreCase(style)) file = "bridal.png";
        else if ("Tidy".equalsIgnoreCase(style)) file = "tidy.png";
        else if ("Clustered".equalsIgnoreCase(style)) file = "clustered.png";

        stylePreview.setImage(file == null ? null :
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/" + file))));
    }


    @FXML private void onBack(ActionEvent evt) {
        EventBus.getDefault().unregister(this);
        ((Stage)backButton.getScene().getWindow()).close();
    }
}

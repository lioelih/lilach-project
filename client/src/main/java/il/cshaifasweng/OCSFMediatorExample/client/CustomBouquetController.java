package il.cshaifasweng.OCSFMediatorExample.client;

import Events.CatalogEvent;
import il.cshaifasweng.Msg;
import il.cshaifasweng.CustomBouquetDTO;
import il.cshaifasweng.CustomBouquetItemDTO;
import il.cshaifasweng.OCSFMediatorExample.entities.Product;
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
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import java.util.Objects;
import java.io.IOException;
import java.util.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.Node;      // if you ever refer to Node in your streams
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import il.cshaifasweng.OCSFMediatorExample.entities.Basket;
public class CustomBouquetController {

    @FXML private ImageView       logoImage;
    @FXML private Button          backButton;
    @FXML private Button          addToBasketButton;
    @FXML private ComboBox<String> existingCombo;
    @FXML private VBox            newPane;
    @FXML private Label           limitLabel;

    @FXML private ComboBox<String> styleBox;
    @FXML private ComboBox<String> potBox;
    @FXML private FlowPane        flowerPane;
    @FXML private TextField       nameField;
    @FXML private Button          saveButton;
    @FXML private Label totalPriceLabel;

    // track the DTO state
    private final Map<ToggleFlower,Integer> selections = new LinkedHashMap<>();
    private final Map<String, Product> potProductMap = new HashMap<>();
    private List<CustomBouquetDTO>          myBouquets = List.of();
    private Integer                         currentCustomId = null;
    private List<Sale>                sales         = new ArrayList<>();
    private List<Product>             catalogProducts = List.of();

    @FXML
    public void initialize() {
        // logo
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));

        // center & spacing
        flowerPane.setAlignment(Pos.CENTER);
        flowerPane.setHgap(8);
        flowerPane.setVgap(8);

        // 50‑flower limit label
        limitLabel.setText("Up to 50 flowers allowed in total per Custom Bouquet");
        limitLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #555;");

        // existing‐bouquets dropdown
        existingCombo.getItems().add("Create New…");
        existingCombo.getSelectionModel().selectFirst();
        existingCombo.setOnAction(this::onExistingSelected);

        // style & pot selectors
        styleBox.getItems().setAll("Bridal","Tidy","Clustered");
        potBox.getItems().setAll("None");
        potBox.getSelectionModel().select("None");
        potBox.valueProperty().addListener((obs, oldPot, newPot) -> {
            updateSaveEnabled();
            updatePrice();
        });

        // Save‐enabled whenever style chosen or selections change
        styleBox.valueProperty().addListener((o,ov,nv) -> updateSaveEnabled());

        // show form initially
        newPane.setVisible(true);

        // subscribe & fetch from server
        EventBus.getDefault().register(this);
        try {
            SimpleClient.getClient().ensureConnected();
            SimpleClient.getClient().sendToServer(new Msg("LIST_CUSTOM_BOUQUETS", SceneController.loggedUsername));
            SimpleClient.getClient().sendToServer(new Msg("GET_CATALOG", null));
            SimpleClient.getClient().sendToServer(new Msg("GET_SALES",    null));
        } catch (IOException e) {
            e.printStackTrace();
        }

        updateSaveEnabled();
        addToBasketButton.setDisable(true);
    }

    /** Populate “My Bouquets” dropdown */
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

    /** Build the palette of flowers */
    @Subscribe
    @SuppressWarnings("unchecked")
    public void onCatalog(CatalogEvent event) {
        // cache the full product list
        catalogProducts = event.getProducts();

        Platform.runLater(() -> {
            // 2a) Clear previous flowers & selections
            flowerPane.getChildren().clear();
            selections.clear();

            // 2b) Build the flower palette
            catalogProducts.stream()
                    .filter(p -> "flower".equalsIgnoreCase(p.getType()))
                    .forEach(p -> {
                        ToggleFlower tf = new ToggleFlower(p);
                        tf.setOnToggled(on -> {
                            if (on) {
                                int used      = selections.values().stream().mapToInt(i->i).sum();
                                int remaining = Math.max(0, 50 - used);
                                selections.put(tf, 1);
                                tf.showQuantitySelector(
                                        1,
                                        1 + remaining,
                                        qty -> {
                                            selections.put(tf, qty);
                                            updateSaveEnabled();
                                            updateQuantityLimits();
                                            updatePrice();
                                        }
                                );
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

            // 3) Repopulate the potBox (uses potProductMap + rebuildPotBox())
            rebuildPotBox();

            // 4) Refresh your UI state
            updateSaveEnabled();
            updateQuantityLimits();
            updatePrice();
        });
    }
    private void rebuildPotBox() {
        potProductMap.clear();
        potBox.getItems().setAll("None");

        catalogProducts.stream()
                .filter(p -> {
                    String t = p.getType().toLowerCase();
                    return t.equals("pot") || t.equals("vase");
                })
                .forEach(p -> {
                    // compute sale price via Sale.calculateTotalDiscount(...)
                    double basePrice = p.getPrice();
                    Basket fake = new Basket();
                    fake.setProduct(p);
                    fake.setAmount(1);
                    fake.setPrice(basePrice);
                    double discount = Sale.calculateTotalDiscount(List.of(fake), sales);
                    double finalPrice = basePrice - discount;

                    String priceStr = String.format("₪%.2f", finalPrice);
                    String label    = p.getName()
                            + " (+"
                            + priceStr
                            + (discount>0 ? " after sale!)" : ")");

                    potProductMap.put(label, p);
                    potBox.getItems().add(label);
                });

        potBox.getSelectionModel().select("None");
    }


    /** Handle switching between “Create New…” & existing bouquets */
    private void onExistingSelected(ActionEvent evt) {
        int idx      = existingCombo.getSelectionModel().getSelectedIndex();
        boolean isNew = idx <= 0;

        newPane.setVisible(true);
        currentCustomId = null;
        saveButton.setDisable(true);
        selections.clear();

        // clear all selections
        flowerPane.getChildren().stream()
                .filter(n -> n instanceof ToggleFlower)
                .map(n -> (ToggleFlower)n)
                .forEach(ToggleFlower::deselect);

        if (isNew) {
            nameField.clear();
            styleBox.getSelectionModel().clearSelection();
            potBox.getSelectionModel().select("None");
            addToBasketButton.setDisable(true);

        } else {
            var dto = myBouquets.get(idx - 1);
            currentCustomId = dto.getId();
            addToBasketButton.setDisable(false);

            nameField.setText(dto.getName());
            styleBox.setValue(dto.getStyle());
            if (dto.getPot() == null) {
                potBox.getSelectionModel().select("None");
            } else {
                potProductMap.keySet().stream()
                        .filter(label -> label.startsWith(dto.getPot()))  // e.g. “Glass Vase”
                        .findFirst()
                        .ifPresentOrElse(
                                label -> potBox.getSelectionModel().select(label),
                                ()     -> potBox.getSelectionModel().select("None")
                        );
            }

            // re‑select existing items
            for (var item : dto.getItems()) {
                flowerPane.getChildren().stream()
                        .filter(n -> n instanceof ToggleFlower)
                        .map(n -> (ToggleFlower)n)
                        .filter(tf -> tf.getProduct().getId() == item.getProductId())
                        .findFirst()
                        .ifPresent(tf -> {
                            tf.setSelectedQuantity(item.getQuantity());
                            selections.put(tf, item.getQuantity());
                        });
            }
            updatePrice();
            updateSaveEnabled();
            updateQuantityLimits();
        }
    }

    @Subscribe
    @SuppressWarnings("unchecked")
    public void onSales(Msg m) {
        if (!"SENT_SALES".equals(m.getAction())) return;
        sales = (List<Sale>) m.getData();
        Platform.runLater(this::rebuildPotBox);
    }



    /** Enable Save only if style chosen and ≥1 flower */
    private void updateSaveEnabled() {
        boolean can = styleBox.getValue()!=null && !selections.isEmpty();
        saveButton.setDisable(!can);
    }

    /**
     * Enforce the 50‑flower max: for each selected flower spinner,
     * bump its factory’s max = current + (50−total).
     */
    private void updateQuantityLimits() {
        int used      = selections.values().stream().mapToInt(i->i).sum();
        int remaining = Math.max(0, 50 - used);

        flowerPane.getChildren().stream()
                .filter(n -> n instanceof ToggleFlower)
                .map(n -> (ToggleFlower) n)
                .filter(ToggleFlower::isSelected)
                .map(ToggleFlower::getSpinner)          // now Spinner<Integer>
                .filter(Objects::nonNull)
                .forEach(sp -> {                        // sp is Spinner<Integer>!
                    IntegerSpinnerValueFactory vf =
                            (IntegerSpinnerValueFactory) sp.getValueFactory();
                    int cur = sp.getValue();
                    vf.setMax(cur + remaining);
                });
        boolean soldOut = (remaining == 0);
        flowerPane.getChildren().stream()
                .filter(n -> n instanceof ToggleFlower)
                .map(n -> (ToggleFlower)n)
                .filter(tf -> !tf.isSelected())
                .forEach(tf -> tf.getToggle().setDisable(soldOut));
    }

    /** Build and send the CREATE/UPDATE request, then confirm “Add to Basket?” */
    @FXML
    private void onSave(ActionEvent evt) {
        String name  = nameField.getText().trim();
        String style = styleBox.getValue();
        String pot   = "None".equals(potBox.getValue()) ? null :  potBox.getValue().replaceAll("\\s*\\(\\+₪\\d+\\)", "").trim();

        // compute price = sum of flowers + pot surcharge
        double price = selections.entrySet().stream()
                .mapToDouble(e -> e.getKey().getProduct().getPrice() * e.getValue())
                .sum();
        if (pot!=null) {
            if (pot.contains("Vase"))   price += 60;
            else if (pot.contains("Pot")) price += 50;
        }

        var dto = new CustomBouquetDTO(
                currentCustomId,
                SceneController.loggedUsername,
                name,
                style,
                "",
                pot,
                price,
                null,
                selections.entrySet().stream()
                        .map(e -> new CustomBouquetItemDTO(
                                currentCustomId,
                                e.getKey().getProduct().getId(),
                                e.getValue()))
                        .toList()
        );

        // send create vs update
        try {
            if (currentCustomId == null)
                SimpleClient.getClient().sendToServer(new Msg("CREATE_CUSTOM_BOUQUET", dto));
            else
                SimpleClient.getClient().sendToServer(new Msg("UPDATE_CUSTOM_BOUQUET", dto));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // confirm pop‑up
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(null);
        alert.setContentText("Saved Bouquet!  Add it to basket now?");
        alert.getButtonTypes().setAll(
                new ButtonType("Yes", ButtonBar.ButtonData.YES),
                new ButtonType("No",  ButtonBar.ButtonData.NO)
        );
        alert.showAndWait().ifPresent(bt -> {
            addToBasketButton.setDisable(false);
            if (bt.getButtonData() == ButtonBar.ButtonData.YES) {
                try {
                    SimpleClient.getClient().sendToServer(
                            new Msg("ADD_CUSTOM_TO_BASKET",
                                    new Object[]{SceneController.loggedUsername, currentCustomId, 1})
                    );
                    SimpleClient.getClient().sendToServer(
                            new Msg("FETCH_BASKET", SceneController.loggedUsername)
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // if “No”, leave newPane visible for further edits
        });
    }

    /** After server confirms create, store the new ID */
    @Subscribe
    public void onCustomCreated(Msg m) {
        if (!"CREATE_CUSTOM_BOUQUET_OK".equals(m.getAction())) return;
        currentCustomId = (Integer)m.getData();
    }

    /** After server confirms update, update the dropdown label */
    @Subscribe
    public void onCustomUpdated(Msg m) {
        if (!"UPDATE_CUSTOM_BOUQUET_OK".equals(m.getAction())) return;
        currentCustomId = (Integer)m.getData();
        Platform.runLater(() -> {
            int idx = existingCombo.getSelectionModel().getSelectedIndex();
            existingCombo.getItems().set(idx, currentCustomId + ": " + nameField.getText());
        });
    }

    /** Manual “Add to Basket” button */
    @FXML
    private void onAddToBasket(ActionEvent evt) {
        if (currentCustomId==null) return;
        try {
            SimpleClient.getClient().sendToServer(
                    new Msg("ADD_CUSTOM_TO_BASKET",
                            new Object[]{SceneController.loggedUsername, currentCustomId, 1})
            );
            SimpleClient.getClient().sendToServer(
                    new Msg("FETCH_BASKET", SceneController.loggedUsername)
            );

            String name = nameField.getText().trim();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText("Successfully added \"" + name + "\" to Basket!");
            alert.showAndWait();

        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void updatePrice() {
        double sum = selections.entrySet().stream()
                .mapToDouble(e -> e.getKey().getProduct().getPrice() * e.getValue())
                .sum();

        String potLabel = potBox.getValue();
        if (potLabel != null && !potLabel.equals("None")) {
            Product chosenPot = potProductMap.get(potLabel);
            if (chosenPot != null) {
                sum += chosenPot.getPrice();
            }
        }

        totalPriceLabel.setText(String.format("Total Price: ₪%.2f", sum));
    }

    /** Clean up */
    @FXML
    private void onBack(ActionEvent evt) {
        EventBus.getDefault().unregister(this);
        ((Stage)backButton.getScene().getWindow()).close();
    }
}

package il.cshaifasweng.OCSFMediatorExample.client;

import Events.*;
import javafx.beans.binding.Bindings;
import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import il.cshaifasweng.OrderDTO;
import il.cshaifasweng.OCSFMediatorExample.entities.Basket;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.IntStream;

public class CheckoutController implements Initializable {

    /* ------------ FXML -------------- */
    @FXML private TableView<Basket>          basketTable;
    @FXML private TableColumn<Basket,String> nameCol;
    @FXML private TableColumn<Basket,Integer> amtCol;
    @FXML private TableColumn<Basket,Double> priceCol;
    @FXML private Label  totalLabel;
    @FXML private Label  totalAfterLabel;
    @FXML private HBox   vipBox;
    @FXML private Label  vipDiscountLabel;
    @FXML private HBox deliveryBox;
    @FXML private Label  deliveryFeeLabel;

    @FXML private RadioButton savedCardRadio, addCardRadio;
    @FXML private Button addCardButton;

    @FXML private RadioButton pickupRadio, deliveryRadio;
    @FXML private ComboBox<Branch> branchCombo;          //  <-- now Branch
    @FXML private TextField cityField, streetField, houseField, zipField;


    @FXML private RadioButton asapRadio, scheduleRadio;
    @FXML private DatePicker   deadlineDatePicker;
    @FXML private ComboBox<Integer> deadlineHourCombo;
    @FXML private HBox scheduleBox;

    @FXML private TextField recipientNameField, recipientPhoneField;

    @FXML private Button     completeBtn;
    @FXML private ImageView  logoImage;

    /* ------------ data -------------- */
    private final List<Basket>  items          = new ArrayList<>();
    private final BooleanProperty hasCardProperty = new SimpleBooleanProperty(false);
    private final List<Branch>  branchList     = new ArrayList<>();     //  <-- keep list
    private boolean isVipUser     = false;
    private double  totalBefore;      // passed in from initData
    private double  saleDiscount;     // ditto
    private String greetingText  = null;
    private String greetingColor = null;
    private String currentGreetingText = null;
    private String currentGreetingHex  = "#FFFFFF";
    /* ------------ init from BasketController -------------- */
    public void initData(List<Basket> copy, double totalBefore, double discount) {
        this.totalBefore   = totalBefore;
        this.saleDiscount  = discount;

        items.clear();
        items.addAll(copy);
        basketTable.getItems().setAll(items);

        totalLabel.setText(String.format("Total Before Discount  ₪ %.2f", totalBefore));
        totalAfterLabel.setText(String.format("Total After Discount   ₪ %.2f", totalBefore - discount));

        updateSummary();
        requestSavedCard();
        requestVipStatus();
    }

    /* ------------ controller init -------------- */
    @Override public void initialize(URL u, ResourceBundle b) {
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));

        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getProductName()));
        amtCol .setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getAmount()).asObject());
        priceCol.setCellValueFactory(c -> new javafx.beans.property.SimpleDoubleProperty(c.getValue().getPrice()).asObject());

        /* groups & enable/disable bindings */
        ToggleGroup payGrp = new ToggleGroup();
        savedCardRadio.setToggleGroup(payGrp);
        addCardRadio  .setToggleGroup(payGrp);

        ToggleGroup fulGrp = new ToggleGroup();
        pickupRadio.selectedProperty().addListener((obs, o, n) -> updateSummary());
        deliveryRadio.selectedProperty().addListener((obs, o, n) -> updateSummary());

        /* ---------- branch combo visualisation ---------- */
        branchCombo.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(Branch b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : b.getName());
            }
        });
        branchCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Branch b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : b.getName());
            }
        });

        branchCombo.disableProperty().bind(pickupRadio.selectedProperty().not());
        cityField.disableProperty()  .bind(deliveryRadio.selectedProperty().not());
        streetField.disableProperty().bind(deliveryRadio.selectedProperty().not());
        houseField.disableProperty() .bind(deliveryRadio.selectedProperty().not());
        zipField.disableProperty()   .bind(deliveryRadio.selectedProperty().not());

        addCardButton.disableProperty().bind(addCardRadio.selectedProperty().not());
        addCardButton.setOnAction(e -> openPaymentWindow());

        BooleanBinding paymentOk =
                savedCardRadio.selectedProperty().and(hasCardProperty)
                        .or(addCardRadio.selectedProperty());

        BooleanBinding fulfilOk =
                pickupRadio.selectedProperty()
                        .and(branchCombo.valueProperty().isNotNull())
                        .or(deliveryRadio.selectedProperty()
                                .and(cityField.textProperty().isNotEmpty())
                                .and(streetField.textProperty().isNotEmpty())
                                .and(houseField.textProperty().isNotEmpty())
                                .and(zipField.textProperty().isNotEmpty()));

        ToggleGroup fulGroup = new ToggleGroup();
        pickupRadio.setToggleGroup(fulGroup);
        deliveryRadio.setToggleGroup(fulGroup);

        for (int h = 8; h <= 20; h++) {
            deadlineHourCombo.getItems().add(h);
        }
        LocalDate today = LocalDate.now();
        deadlineDatePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date.isBefore(today)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #EEEEEE;");
                }
            }
        });
        scheduleBox.visibleProperty().bind(scheduleRadio.selectedProperty());
        scheduleBox.managedProperty().bind(scheduleRadio.selectedProperty());
        deadlineDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate == null) return;
            int minHour = 8;  // never before 8am
            if (newDate.equals(today)) {
                LocalDateTime asap = LocalDateTime.now().plusHours(3);
                int cutoff = asap.getHour();
                // if there’s any minutes past, bump to next hour
                if (asap.getMinute() > 0) {
                    cutoff++;
                }
                minHour = Math.max(minHour, cutoff);
            }

            // rebuild the hour list
            var valid = IntStream.rangeClosed(minHour, 20).boxed().toList();
            deadlineHourCombo.getItems().setAll(valid);
            // clear any previously-picked invalid hour
            if (!valid.contains(deadlineHourCombo.getValue())) {
                deadlineHourCombo.setValue(null);
            }
        });




        BooleanBinding timeOk = asapRadio.selectedProperty()
                .or(scheduleRadio.selectedProperty()
                        .and(deadlineDatePicker.valueProperty().isNotNull())
                        .and(deadlineHourCombo.valueProperty().isNotNull()));

        BooleanBinding ready = paymentOk
                .and(fulGroup.selectedToggleProperty().isNotNull())
                .and(fulfilOk)
                .and(timeOk);

        completeBtn.disableProperty().bind(ready.not());

        completeBtn.setOnAction(e -> submitOrder());

        /* ask server for branches & card */
        try { SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null)); }
        catch (Exception ex){ex.printStackTrace();}
        requestSavedCard();

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        requestVipStatus();
        try {
            SimpleClient.getClient().sendToServer(new Msg("FETCH_USER", SceneController.loggedUsername));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* ---------- build & send order ---------- */
    private void submitOrder() {
        try {
            SimpleClient.getClient().sendToServer(
                    new Msg("NEW_ORDER", buildDto()));
        } catch (Exception e){e.printStackTrace();}
    }

    private OrderDTO buildDto() {
        String type = pickupRadio.isSelected() ? "PICKUP" : "DELIVERY";
        String info;

        if ("PICKUP".equals(type)) {
            Branch sel = branchCombo.getValue();
            info = sel == null ? "" : String.valueOf(sel.getBranchId()); // send id
        } else {
            info = String.format("%s, %s %s (%s)",
                    cityField.getText(), streetField.getText(),
                    houseField.getText(), zipField.getText());
        }

        LocalDateTime deadline;
        if (asapRadio.isSelected()) {
            deadline = LocalDateTime.now().plusHours(3);
        } else {
            LocalDate date = deadlineDatePicker.getValue();
            Integer hour   = deadlineHourCombo.getValue();
            deadline = date.atTime(hour, 0);
        }

        return new OrderDTO(
                SceneController.loggedUsername,
                items.stream().map(Basket::getId).toList(),
                type, info,
                deadline,
                recipientNameField.getText().trim()  + " (" + recipientPhoneField.getText().trim() + ")",
                null
        );
    }

    /* ---------- helpers ---------- */
    private void requestSavedCard() {
        try { SimpleClient.getClient().sendToServer(
                new Msg("HAS_CARD", SceneController.loggedUsername)); }
        catch (Exception e) {e.printStackTrace();}
    }
    private void openPaymentWindow() {
        try {
            FXMLLoader f = new FXMLLoader(getClass().getResource("payment.fxml"));
            Stage st = new Stage();
            st.setScene(new javafx.scene.Scene(f.load()));
            st.setTitle("Add / Edit Card");
            st.showAndWait();
            requestSavedCard();
        } catch (Exception e){e.printStackTrace();}
    }

    /* ---------- server events ---------- */
    @Subscribe
    public void handleServerMsg(Msg m) {

        if ("BRANCHES_OK".equals(m.getAction())) {          // fill combo
            branchList.clear();
            branchList.addAll((List<Branch>) m.getData());

            Platform.runLater(() -> {
                branchCombo.getItems().setAll(branchList);
                branchCombo.getSelectionModel().clearSelection();
            });
            return;
        }

        Platform.runLater(() -> {
            switch (m.getAction()) {
                case "FETCH_USER" -> {
                    User user = (User) m.getData();
                    isVipUser = user.isVIP();
                    recipientNameField.setText(user.getFullName());
                    recipientPhoneField.setText(user.getPhoneNumber());
                    updateSummary();
                }

                case "HAS_CARD" -> {
                    boolean has = (Boolean) m.getData();
                    hasCardProperty.set(has);
                    savedCardRadio.setText(has ? "Use saved card" : "No card on file");
                    savedCardRadio.setDisable(!has);
                }
                case "ORDER_OK" -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> res = (Map<String, Object>) m.getData();
                    double finalTotal  = ((Number) res.get("totalPrice")).doubleValue();
                    double vipDisc     = ((Number) res.get("vipDiscount")).doubleValue();
                    double deliveryFee = ((Number) res.get("deliveryFee")).doubleValue();
                    int createdOrderId = ((Number) res.get("orderId")).intValue();

                    // 1. Show confirmation with breakdown
                    new Alert(Alert.AlertType.INFORMATION,
                            String.format(
                                    "Order completed!\nYou paid ₪%.2f\n– VIP Discount: ₪%.2f\n+ Delivery Fee: ₪%.2f",
                                    finalTotal, vipDisc, deliveryFee
                            )
                    ).showAndWait();

                    // 2. Refresh basket & close main window
                    try {
                        SimpleClient.getClient().sendToServer(
                                new Msg("FETCH_BASKET", SceneController.loggedUsername)
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    closeWindow();

                    // 3. Ask to add a greeting
                    Alert confirm = new Alert(
                            Alert.AlertType.CONFIRMATION,
                            "Add a greeting to your order?",
                            ButtonType.YES, ButtonType.NO
                    );
                    confirm.setHeaderText(null);
                    if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        // 4. Load greeting dialog
                        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                                "/il/cshaifasweng/OCSFMediatorExample/client/greeting.fxml"
                        ));
                        Parent root = null;
                        try {
                            root = loader.load();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        GreetingController gc = loader.getController();
                        gc.init(
                                /* initialText= */ null,
                                /* initialHex = */ "#FFFFFF",
                                (text, hex) -> {
                                    // 5. Send UPDATE_GREETING
                                    Map<String, Object> msg = Map.of(
                                            "orderId",  createdOrderId,
                                            "greeting", String.format("(%s)%s", hex, text)
                                    );
                                    try {
                                        SimpleClient.getClient().sendToServer(
                                                new Msg("UPDATE_GREETING", msg)
                                        );
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                        );
                        Stage popup = new Stage();
                        popup.initModality(Modality.APPLICATION_MODAL);
                        popup.setTitle("Add Greeting");
                        popup.setScene(new Scene(root));
                        popup.showAndWait();
                    }
                }

                case "ORDER_FAIL" -> new Alert(Alert.AlertType.ERROR,
                        "Order failed, please check details.").showAndWait();
            }
        });
    }

    private void requestVipStatus() {
        try {
            SimpleClient.getClient().sendToServer(
                    new Msg("FETCH_USER", SceneController.loggedUsername)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateSummary() {
        double afterSale   = totalBefore - saleDiscount;
        double vipDisc     = isVipUser    ? afterSale * 0.10 : 0.0;
        double deliveryFee = deliveryRadio.isSelected() ? 10.0 : 0.0;
        double finalTotal  = afterSale - vipDisc + deliveryFee;

        // VIP
        vipBox.setVisible(isVipUser);
        vipBox.setManaged(isVipUser);
        vipDiscountLabel.setText(String.format("-₪%.2f", vipDisc));

        // Delivery
        boolean isDel = deliveryRadio.isSelected();
        deliveryBox.setVisible(isDel);
        deliveryBox.setManaged(isDel);
        deliveryFeeLabel.setText(String.format("+₪%.2f", deliveryFee));

        // Button
        completeBtn.setText(String.format("Complete Order (₪%.2f)", finalTotal));
    }

    /* ---------- window utils ---------- */
    private void closeWindow() {
        EventBus.getDefault().unregister(this);
        ((Stage) basketTable.getScene().getWindow()).close();
    }
    @FXML private void goBack(){ closeWindow(); }
    @FXML private void onClose(){ EventBus.getDefault().unregister(this); }
}

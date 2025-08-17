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
import javafx.beans.property.SimpleStringProperty;
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
import java.util.*;
import java.util.stream.IntStream;

public class CheckoutController implements Initializable {

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
    @FXML private ComboBox<Branch> branchCombo;
    @FXML private TextField cityField, streetField, houseField, zipField;

    @FXML private RadioButton asapRadio, scheduleRadio;
    @FXML private DatePicker   deadlineDatePicker;
    @FXML private ComboBox<Integer> deadlineHourCombo;
    @FXML private HBox scheduleBox;

    @FXML private TextField recipientNameField, recipientPhoneField;

    @FXML private CheckBox useCompensationBox;
    @FXML private Label    compBalanceLabel;

    @FXML private Button     completeBtn;
    @FXML private ImageView  logoImage;

    private final List<Basket>  items             = new ArrayList<>();
    private final BooleanProperty hasCardProperty = new SimpleBooleanProperty(false);
    private final List<Branch>  branchList        = new ArrayList<>();
    private boolean isVipUser    = false;
    private double  totalBefore;      // set in initData
    private double  saleDiscount;     // set in initData
    private double  userCompBalance = 0.0;

    public void initData(List<Basket> copy, double totalBefore, double discount) {
        this.totalBefore  = totalBefore;
        this.saleDiscount = discount;

        items.clear();
        items.addAll(copy);
        basketTable.getItems().setAll(items);

        totalLabel.setText(String.format("Total Before Discount   ₪ %.2f", totalBefore));
        totalAfterLabel.setText(String.format("Total After Discount    ₪ %.2f", totalBefore - saleDiscount));

        updateSummary();

    }

    @Override public void initialize(URL location, ResourceBundle resources) {
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));

        nameCol.setCellValueFactory(c -> {
            Basket b = c.getValue();
            if (b.getCustomBouquet() != null) {
                return new SimpleStringProperty(
                        "Custom: " + b.getCustomBouquet().getName());
            } else {
                return new SimpleStringProperty(b.getProductName());
            }
        });
        amtCol .setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getAmount()).asObject());
        priceCol.setCellValueFactory(c -> new javafx.beans.property.SimpleDoubleProperty(c.getValue().getPrice()).asObject());

        // Payment toggles
        ToggleGroup payGrp = new ToggleGroup();
        savedCardRadio.setToggleGroup(payGrp);
        addCardRadio  .setToggleGroup(payGrp);
        addCardButton.disableProperty().bind(addCardRadio.selectedProperty().not());
        addCardButton.setOnAction(e -> openPaymentWindow());

        // Fulfillment toggles
        ToggleGroup fulGrp = new ToggleGroup();
        pickupRadio.setToggleGroup(fulGrp);
        deliveryRadio.setToggleGroup(fulGrp);
        pickupRadio.selectedProperty().addListener((o,oldV,newV) -> updateSummary());
        deliveryRadio.selectedProperty().addListener((o,oldV,newV) -> updateSummary());

        // Branch vs. delivery address
        branchCombo.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(Branch b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b==null ? "" : b.getName());
            }
        });
        branchCombo.setButtonCell(branchCombo.getCellFactory().call(null));
        branchCombo.disableProperty().bind(pickupRadio.selectedProperty().not());
        cityField.disableProperty() .bind(deliveryRadio.selectedProperty().not());
        streetField.disableProperty().bind(deliveryRadio.selectedProperty().not());
        houseField.disableProperty() .bind(deliveryRadio.selectedProperty().not());
        zipField.disableProperty()   .bind(deliveryRadio.selectedProperty().not());

        // Scheduling
        ToggleGroup timeGrp = new ToggleGroup();
        asapRadio.setToggleGroup(timeGrp);
        scheduleRadio.setToggleGroup(timeGrp);
        scheduleBox.visibleProperty().bind(scheduleRadio.selectedProperty());
        scheduleBox.managedProperty().bind(scheduleRadio.selectedProperty());
        LocalDate today = LocalDate.now();
        deadlineDatePicker.setDayCellFactory(dp -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date.isBefore(today)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #EEEEEE;");
                }
            }
        });
        deadlineDatePicker.valueProperty().addListener((obs,oldD,newD) -> {
            if (newD==null) return;
            int minHour=8;
            if (newD.equals(today)) {
                LocalDateTime asap = LocalDateTime.now().plusHours(3);
                int cutoff = asap.getHour() + (asap.getMinute()>0?1:0);
                minHour = Math.max(minHour, cutoff);
            }
            var hours = IntStream.rangeClosed(minHour,20).boxed().toList();
            deadlineHourCombo.getItems().setAll(hours);
            if (!hours.contains(deadlineHourCombo.getValue()))
                deadlineHourCombo.setValue(null);
        });
        deadlineHourCombo.getItems().clear();
        deadlineDatePicker.setValue(today);


        // Compensation checkbox
        useCompensationBox.selectedProperty().addListener((o,oldV,newV) -> updateSummary());

        // Bind "Complete" enablement
        BooleanBinding paymentOk = Bindings.or(
                useCompensationBox.selectedProperty()
                        .and(Bindings.createBooleanBinding(
                                () -> userCompBalance >= computeFinalTotal(),
                                useCompensationBox.selectedProperty(),
                                Bindings.createDoubleBinding(this::computeFinalTotal)
                        )),
                savedCardRadio.selectedProperty().and(hasCardProperty)
                        .or(addCardRadio.selectedProperty())
        );

        BooleanBinding fulfilOk = Bindings.or(
                pickupRadio.selectedProperty().and(branchCombo.valueProperty().isNotNull()),
                deliveryRadio.selectedProperty()
                        .and(cityField.textProperty().isNotEmpty())
                        .and(streetField.textProperty().isNotEmpty())
                        .and(houseField.textProperty().isNotEmpty())
                        .and(zipField.textProperty().isNotEmpty())
        );

        BooleanBinding timeOk = asapRadio.selectedProperty()
                .or(scheduleRadio.selectedProperty()
                        .and(deadlineDatePicker.valueProperty().isNotNull())
                        .and(deadlineHourCombo.valueProperty().isNotNull())
                );

        completeBtn.disableProperty().bind(
                paymentOk.not()
                        .or(fulfilOk.not())
                        .or(timeOk.not())
        );
        completeBtn.setOnAction(e -> submitOrder());

        // Load branches & user info
        try { SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null)); } catch(IOException ex){ex.printStackTrace();}
        try { SimpleClient.getClient().sendToServer(new Msg("FETCH_USER", SceneController.loggedUsername)); } catch(IOException ex){ex.printStackTrace();}
        try { SimpleClient.getClient().sendToServer(new Msg("HAS_CARD", SceneController.loggedUsername)); } catch(IOException ex){ex.printStackTrace();}

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    private void submitOrder() {
        try {
            SimpleClient.getClient().sendToServer(new Msg("NEW_ORDER", buildDto()));
        } catch(IOException e){e.printStackTrace();}
    }

    private OrderDTO buildDto() {
        // incase we need to remember, i wrote here step by step instructions on building an orderDTO which is used later in tables and such
        // 1) figure out fulfilment type & info
        String type = pickupRadio.isSelected() ? "PICKUP" : "DELIVERY";
        String info = type.equals("PICKUP")
                ? String.valueOf(branchCombo.getValue().getBranchId())
                : String.format("%s, %s %s (%s)",
                cityField.getText(), streetField.getText(),
                houseField.getText(), zipField.getText());

        // 2) deadline logic
        LocalDateTime deadline = asapRadio.isSelected()
                ? LocalDateTime.now().plusHours(3)
                : deadlineDatePicker.getValue().atTime(deadlineHourCombo.getValue(), 0);

        // 3) recipient
        String recipient = recipientNameField.getText().trim()
                + " (" + recipientPhoneField.getText().trim() + ")";

        // 4) greeting (if you captured it earlier)
        String greeting = null; // or null if none

        // 5) compute finalTotal exactly as in updateSummary()
        double afterSale   = totalBefore - saleDiscount;
        double vipDisc     = isVipUser ? afterSale * 0.10 : 0.0;
        double deliveryFee = deliveryRadio.isSelected() ? 10.0 : 0.0;
        double finalTotal  = afterSale - vipDisc + deliveryFee;

        // 6) decide how much store credit to use
        boolean useComp  = useCompensationBox.isSelected();
        double compToUse = useComp
                ? Math.min(userCompBalance, finalTotal)
                : 0.0;

        // 7) now call the 9‑arg constructor
        return new OrderDTO(
                SceneController.loggedUsername,
                items.stream().map(Basket::getId).toList(),
                type, info,
                deadline,
                recipient,
                greeting,
                useComp,
                compToUse
        );
    }

    private void openPaymentWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("payment.fxml"));
            Stage st = new Stage();
            st.setScene(new Scene(loader.load()));
            st.setTitle("Add / Edit Card");
            st.showAndWait();
            // refresh card status
            SimpleClient.getClient().sendToServer(new Msg("HAS_CARD", SceneController.loggedUsername));
        } catch(Exception e){e.printStackTrace();}
    }

    @Subscribe
    public void handleServerMsg(Msg m) {
        Platform.runLater(() -> {
            switch (m.getAction()) {
                case "BRANCHES_OK" -> {
                    branchList.clear();
                    branchList.addAll((List<Branch>)m.getData());
                    branchCombo.getItems().setAll(branchList);
                }
                case "FETCH_USER" -> {
                    User user = (User)m.getData();
                    userCompBalance = user.getCompensationTab();
                    compBalanceLabel.setText(String.format("You have ₪%.2f store credit", userCompBalance));
                    recipientNameField.setText(user.getFullName());
                    recipientPhoneField.setText(user.getPhoneNumber());
                    isVipUser = user.isVIP();
                    updateSummary();
                }
                case "HAS_CARD" -> {
                    boolean has = (Boolean)m.getData();
                    hasCardProperty.set(has);
                    savedCardRadio.setDisable(!has);
                    savedCardRadio.setText(has ? "Use saved card" : "No card on file");
                }
                case "ORDER_OK" -> {
                    @SuppressWarnings("unchecked") Map<String,Object> res = (Map<String,Object>)m.getData();
                    double finalTotal  = ((Number)res.get("totalPrice")).doubleValue();
                    double vipDisc     = ((Number)res.get("vipDiscount")).doubleValue();
                    double deliveryFee = ((Number)res.get("deliveryFee")).doubleValue();
                    double usedComp    = ((Number)res.get("compensationUsed")).doubleValue();
                    int createdId      = ((Number)res.get("orderId")).intValue();

                    // 1) Confirmation
                    String msg = String.format(
                            "Order completed!\nYou paid ₪%.2f\n– VIP Discount: ₪%.2f\n+ Delivery Fee: ₪%.2f",
                            finalTotal, vipDisc, deliveryFee
                    );
                    if (usedComp>0) msg += String.format("\n– Used Store Credit: ₪%.2f", usedComp);
                    new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();

                    // 2) Refresh & close
                    try { SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername)); }
                    catch(IOException ex){ex.printStackTrace();}
                    closeWindow();

                    // 3) Greeting popup
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Add a greeting to your order?", ButtonType.YES, ButtonType.NO);
                    confirm.setHeaderText(null);
                    if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        try {
                            FXMLLoader f2 = new FXMLLoader(getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/greeting.fxml"));
                            Parent root = f2.load();
                            GreetingController gc = f2.getController();
                            gc.init(null, "#FFFFFF", (text,hex) -> {
                                Map<String,Object> payload = Map.of(
                                        "orderId", createdId,
                                        "greeting", String.format("(%s)%s", hex, text)
                                );
                                try {
                                    SimpleClient.getClient().sendToServer(new Msg("UPDATE_GREETING", payload));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            Stage popup = new Stage();
                            popup.initModality(Modality.APPLICATION_MODAL);
                            popup.setTitle("Add Greeting");
                            popup.setScene(new Scene(root));
                            popup.showAndWait();
                        } catch(IOException ex){ex.printStackTrace();}
                    }
                }
                case "ORDER_FAIL" ->
                        new Alert(Alert.AlertType.ERROR, "Order failed, please check details.").showAndWait();
            }
        });
    }

    private void updateSummary() {
        double afterSale   = totalBefore - saleDiscount;
        double vipDisc     = isVipUser ? afterSale*0.10 : 0.0;
        double deliveryFee = deliveryRadio.isSelected() ? 10.0 : 0.0;
        double finalTot    = afterSale - vipDisc;
        double compUsed    = useCompensationBox.isSelected() ? Math.min(userCompBalance, finalTot) : 0.0;
        double payNow      = finalTot - compUsed + deliveryFee;

        // VIP
        vipBox.setVisible(isVipUser);
        vipBox.setManaged(isVipUser);
        vipDiscountLabel.setText(String.format("-₪%.2f", vipDisc));

        // Delivery
        deliveryBox.setVisible(deliveryRadio.isSelected());
        deliveryBox.setManaged(deliveryRadio.isSelected());
        deliveryFeeLabel.setText(String.format("+₪%.2f", deliveryFee));

        // Labels
        totalLabel.setText(String.format("Total Before Discount   ₪ %.2f", totalBefore));
        totalAfterLabel.setText(String.format("Total After Discount    ₪ %.2f", finalTot));
        if (compUsed>0) {
            compBalanceLabel.setText(String.format("Applied ₪%.2f of store credit", compUsed));
        } else {
            compBalanceLabel.setText(String.format("You have ₪%.2f store credit", userCompBalance));
        }

        completeBtn.setText(String.format("Complete Order (₪%.2f)", payNow));
    }

    private double computeFinalTotal() {
        double afterSale   = totalBefore - saleDiscount;
        double vipDisc     = isVipUser ? afterSale*0.10 : 0.0;
        double deliveryFee = deliveryRadio.isSelected() ? 10.0 : 0.0;
        return afterSale - vipDisc + deliveryFee;
    }

    private void closeWindow() {
        EventBus.getDefault().unregister(this);
        ((Stage)basketTable.getScene().getWindow()).close();
    }

    @FXML private void goBack() { closeWindow(); }
    @FXML private void onClose() { EventBus.getDefault().unregister(this); }
}

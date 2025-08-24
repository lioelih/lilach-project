package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OrderDTO;
import il.cshaifasweng.OCSFMediatorExample.entities.Basket;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

/*
 * checkout controller:
 * - shows basket lines and live totals (sales, vip, delivery, store credit)
 * - collects payment/fulfilment/schedule/recipient inputs
 * - validates inputs with bindings and submits an order dto
 * - handles server responses (branches, user/card info, order result)
 */
public class CheckoutController implements Initializable {

    // ui: basket table
    @FXML private TableView<Basket> basketTable;
    @FXML private TableColumn<Basket, Basket> imgCol;
    @FXML private TableColumn<Basket, String> nameCol;
    @FXML private TableColumn<Basket, Integer> amtCol;
    @FXML private TableColumn<Basket, Double> priceCol;

    // ui: summary
    @FXML private Label totalLabel;
    @FXML private Label discountLabel;
    @FXML private Label totalAfterLabel;

    // ui: vip + delivery rows
    @FXML private HBox vipBox;
    @FXML private Label vipDiscountLabel;
    @FXML private HBox deliveryBox;
    @FXML private Label deliveryFeeLabel;

    // ui: payment
    @FXML private RadioButton savedCardRadio, addCardRadio;
    @FXML private Button addCardButton;

    // ui: fulfilment
    @FXML private RadioButton pickupRadio, deliveryRadio;
    @FXML private ComboBox<Branch> branchCombo;
    @FXML private TextField cityField, streetField, houseField, zipField;

    // ui: timing
    @FXML private RadioButton asapRadio, scheduleRadio;
    @FXML private DatePicker deadlineDatePicker;
    @FXML private ComboBox<Integer> deadlineHourCombo;
    @FXML private HBox scheduleBox;

    // ui: recipient
    @FXML private TextField recipientNameField, recipientPhoneField;

    // ui: compensation
    @FXML private CheckBox useCompensationBox;
    @FXML private Label compBalanceLabel;

    // ui: actions/logo
    @FXML private Button completeBtn;
    @FXML private ImageView logoImage;

    // state
    private final List<Basket> items = new ArrayList<>();
    private final BooleanProperty hasCardProperty = new SimpleBooleanProperty(false);
    private final List<Branch> branchList = new ArrayList<>();
    private boolean isVipUser = false;
    private double totalBefore;
    private double saleDiscount;
    private double userCompBalance = 0.0;


    // init from basket page: snapshot of items and computed discounts

    // address validation regex
    private static final String LETTERS_NO_DIGITS = "^(?!.*\\d).*\\p{L}.*$"; // must contain a letter, no digits allowed
    private static final String HOUSE_REGEX = "^\\d{1,4}$";                  // 1-4 digits
    private static final String ZIP_REGEX = "^\\d{5,7}$";                    // 5-7 digits


    public void initData(List<Basket> copy, double totalBefore, double discount) {
        this.totalBefore = totalBefore;
        this.saleDiscount = discount;
        items.clear();
        items.addAll(copy);
        basketTable.getItems().setAll(items);
        totalLabel.setText(String.format("Total: ₪ %.2f", totalBefore));
        discountLabel.setText(String.format("Discount: ₪ %.2f", saleDiscount));
        totalAfterLabel.setText(String.format("Total After Discount: ₪ %.2f", totalBefore - saleDiscount));
        updateSummary();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // logo
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));

        // table columns (image/name/qty/price)
        imgCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        imgCol.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(48);
                iv.setFitHeight(48);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
            }
            @Override
            protected void updateItem(Basket b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) {
                    setGraphic(null);
                } else {
                    iv.setImage(buildImage(b));
                    setGraphic(iv);
                }
            }
        });
        nameCol.setCellValueFactory(c -> {
            Basket b = c.getValue();
            return (b.getCustomBouquet() != null)
                    ? new SimpleStringProperty("Custom: " + b.getCustomBouquet().getName())
                    : new SimpleStringProperty(b.getProductName());
        });
        amtCol.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getAmount()).asObject());
        priceCol.setCellValueFactory(c -> new javafx.beans.property.SimpleDoubleProperty(c.getValue().getPrice()).asObject());

        // payment selectors
        ToggleGroup payGrp = new ToggleGroup();
        savedCardRadio.setToggleGroup(payGrp);
        addCardRadio.setToggleGroup(payGrp);
        addCardButton.disableProperty().bind(addCardRadio.selectedProperty().not());
        addCardButton.setOnAction(e -> openPaymentWindow());

        // fulfilment selectors + dependent fields
        ToggleGroup fulGrp = new ToggleGroup();
        pickupRadio.setToggleGroup(fulGrp);
        deliveryRadio.setToggleGroup(fulGrp);
        pickupRadio.selectedProperty().addListener((o, a, b) -> updateSummary());
        deliveryRadio.selectedProperty().addListener((o, a, b) -> updateSummary());
        branchCombo.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(Branch b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : b.getName());
            }
        });
        branchCombo.setButtonCell(branchCombo.getCellFactory().call(null));
        branchCombo.disableProperty().bind(pickupRadio.selectedProperty().not());
        cityField.disableProperty().bind(deliveryRadio.selectedProperty().not());
        streetField.disableProperty().bind(deliveryRadio.selectedProperty().not());
        houseField.disableProperty().bind(deliveryRadio.selectedProperty().not());
        zipField.disableProperty().bind(deliveryRadio.selectedProperty().not());

        // scheduling + constraints (min hour today = now + 3h, business hours 8–20)
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
                    setStyle("-fx-background-color:#EEEEEE;");
                }
            }
        });
        deadlineDatePicker.valueProperty().addListener((obs, oldD, newD) -> {
            if (newD == null) return;
            int minHour = 8;
            if (newD.equals(today)) {
                LocalDateTime asap = LocalDateTime.now().plusHours(3);
                int cutoff = asap.getHour() + (asap.getMinute() > 0 ? 1 : 0);
                minHour = Math.max(minHour, cutoff);
            }
            var hours = IntStream.rangeClosed(minHour, 20).boxed().toList();
            deadlineHourCombo.getItems().setAll(hours);
            if (!hours.contains(deadlineHourCombo.getValue())) deadlineHourCombo.setValue(null);
        });
        deadlineHourCombo.getItems().clear();
        deadlineDatePicker.setValue(today);

        // store credit toggle re-computes summary
        useCompensationBox.selectedProperty().addListener((o, a, b) -> updateSummary());

        // validation bindings (payment, fulfilment, time)
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


        // address validation bindings for delivery
        BooleanBinding cityValid = Bindings.createBooleanBinding(
                () -> {
                    String s = cityField.getText();
                    return s != null && s.matches(LETTERS_NO_DIGITS);
                },
                cityField.textProperty()
        );

        BooleanBinding streetValid = Bindings.createBooleanBinding(
                () -> {
                    String s = streetField.getText();
                    return s != null && s.matches(LETTERS_NO_DIGITS);
                },
                streetField.textProperty()
        );

        BooleanBinding houseValid = Bindings.createBooleanBinding(
                () -> {
                    String s = houseField.getText();
                    return s != null && s.matches(HOUSE_REGEX);
                },
                houseField.textProperty()
        );

        BooleanBinding zipValid = Bindings.createBooleanBinding(
                () -> {
                    String s = zipField.getText();
                    return s != null && s.matches(ZIP_REGEX);
                },
                zipField.textProperty()
        );

        BooleanBinding pickupOk = pickupRadio.selectedProperty().and(branchCombo.valueProperty().isNotNull());
        BooleanBinding deliveryAddrOk = deliveryRadio.selectedProperty()
                .and(cityValid)
                .and(streetValid)
                .and(houseValid)
                .and(zipValid);

        BooleanBinding fulfilOk = pickupOk.or(deliveryAddrOk);

        BooleanBinding timeOk = asapRadio.selectedProperty()
                .or(scheduleRadio.selectedProperty()
                        .and(deadlineDatePicker.valueProperty().isNotNull())
                        .and(deadlineHourCombo.valueProperty().isNotNull()));

        // enable when all constraints pass
        completeBtn.disableProperty().bind(paymentOk.not().or(fulfilOk.not()).or(timeOk.not()));
        completeBtn.setOnAction(e -> submitOrder());

        // initial data fetch
        try { SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null)); } catch (IOException ex) { ex.printStackTrace(); }
        try { SimpleClient.getClient().sendToServer(new Msg("FETCH_USER", SceneController.loggedUsername)); } catch (IOException ex) { ex.printStackTrace(); }
        try { SimpleClient.getClient().sendToServer(new Msg("HAS_CARD", SceneController.loggedUsername)); } catch (IOException ex) { ex.printStackTrace(); }

        // eventbus registration
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    // send order dto to server
    private void submitOrder() {
        try { SimpleClient.getClient().sendToServer(new Msg("NEW_ORDER", buildDto())); }
        catch (IOException e) { e.printStackTrace(); }
    }

    // build the order dto from current ui state
    private OrderDTO buildDto() {
        String type = pickupRadio.isSelected() ? "PICKUP" : "DELIVERY";
        String info = type.equals("PICKUP")
                ? String.valueOf(branchCombo.getValue().getBranchId())
                : String.format("%s, %s %s (%s)", cityField.getText(), streetField.getText(), houseField.getText(), zipField.getText());
        LocalDateTime deadline = asapRadio.isSelected()
                ? LocalDateTime.now().plusHours(3)
                : deadlineDatePicker.getValue().atTime(deadlineHourCombo.getValue(), 0);
        String recipient = recipientNameField.getText().trim() + " (" + recipientPhoneField.getText().trim() + ")";
        String greeting = null;

        double afterSale = totalBefore - saleDiscount;
        double vipDisc = isVipUser ? afterSale * 0.10 : 0.0;
        double deliveryFee = deliveryRadio.isSelected() ? 10.0 : 0.0;
        double finalTotal = afterSale - vipDisc + deliveryFee;

        boolean useComp = useCompensationBox.isSelected();
        double compToUse = useComp ? Math.min(userCompBalance, finalTotal) : 0.0;

        return new OrderDTO(
                SceneController.loggedUsername,
                items.stream().map(Basket::getId).toList(),
                type, info, deadline, recipient, greeting, useComp, compToUse
        );
    }

    // open the add/edit card window and refresh saved-card state
    private void openPaymentWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("payment.fxml"));
            Stage st = new Stage();
            st.setScene(new Scene(loader.load()));
            st.setTitle("Add / Edit Card");
            st.showAndWait();
            SimpleClient.getClient().sendToServer(new Msg("HAS_CARD", SceneController.loggedUsername));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // central handler for server messages needed in checkout
    @Subscribe
    public void handleServerMsg(Msg m) {
        Platform.runLater(() -> {
            switch (m.getAction()) {
                case "BRANCHES_OK" -> {
                    branchList.clear();
                    branchList.addAll((List<Branch>) m.getData());
                    branchCombo.getItems().setAll(branchList);
                }
                case "FETCH_USER" -> {
                    User user = (User) m.getData();
                    userCompBalance = user.getCompensationTab();
                    compBalanceLabel.setText(String.format("You have ₪%.2f store credit", userCompBalance));
                    recipientNameField.setText(user.getFullName());
                    recipientPhoneField.setText(user.getPhoneNumber());
                    isVipUser = user.isVIP();
                    updateSummary();
                }
                case "HAS_CARD" -> {
                    boolean has = (Boolean) m.getData();
                    hasCardProperty.set(has);
                    savedCardRadio.setDisable(!has);
                    savedCardRadio.setText(has ? "Use saved card" : "No card on file");
                }
                case "ORDER_OK" -> {
                    @SuppressWarnings("unchecked") Map<String, Object> res = (Map<String, Object>) m.getData();
                    double finalTotal = ((Number) res.get("totalPrice")).doubleValue();
                    double vipDisc = ((Number) res.get("vipDiscount")).doubleValue();
                    double deliveryFee = ((Number) res.get("deliveryFee")).doubleValue();
                    double usedComp = ((Number) res.get("compensationUsed")).doubleValue();
                    int createdId = ((Number) res.get("orderId")).intValue();

                    String msg = String.format(
                            "Order completed!\nYou paid ₪%.2f\n– VIP Discount: ₪%.2f\n+ Delivery Fee: ₪%.2f",
                            finalTotal, vipDisc, deliveryFee);
                    if (usedComp > 0) msg += String.format("\n– Used Store Credit: ₪%.2f", usedComp);
                    new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();

                    try { SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername)); }
                    catch (IOException ex) { ex.printStackTrace(); }
                    closeWindow();

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Add a greeting to your order?", ButtonType.YES, ButtonType.NO);
                    confirm.setHeaderText(null);
                    if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        try {
                            FXMLLoader f2 = new FXMLLoader(getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/greeting.fxml"));
                            Parent root = f2.load();
                            GreetingController gc = f2.getController();
                            gc.init(null, "#FFFFFF", (text, hex) -> {
                                Map<String, Object> payload = Map.of(
                                        "orderId", createdId,
                                        "greeting", String.format("(%s)%s", hex, text)
                                );
                                try { SimpleClient.getClient().sendToServer(new Msg("UPDATE_GREETING", payload)); }
                                catch (IOException e) { throw new RuntimeException(e); }
                            });
                            Stage popup = new Stage();
                            popup.initModality(Modality.APPLICATION_MODAL);
                            popup.setTitle("Add Greeting");
                            popup.setScene(new Scene(root));
                            popup.showAndWait();
                        } catch (IOException ex) { ex.printStackTrace(); }
                    }
                }
                case "ORDER_FAIL" ->
                        new Alert(Alert.AlertType.ERROR, "Order failed, please check details.").showAndWait();
            }
        });
    }

    // recompute labels, visibility and button text when state changes
    private void updateSummary() {
        double afterSale = totalBefore - saleDiscount;
        double vipDisc = isVipUser ? afterSale * 0.10 : 0.0;
        double deliveryFee = deliveryRadio.isSelected() ? 10.0 : 0.0;
        double totalDisc = saleDiscount + vipDisc;
        double finalTot = totalBefore - totalDisc;
        double compUsed = useCompensationBox.isSelected() ? Math.min(userCompBalance, finalTot) : 0.0;
        double payNow = finalTot - compUsed + deliveryFee;

        vipBox.setVisible(isVipUser);  vipBox.setManaged(isVipUser);
        vipDiscountLabel.setText(String.format("-₪%.2f", vipDisc));

        boolean showDel = deliveryRadio.isSelected();
        deliveryBox.setVisible(showDel); deliveryBox.setManaged(showDel);
        deliveryFeeLabel.setText(String.format("+₪%.2f", deliveryFee));

        totalLabel.setText(String.format("Total: ₪ %.2f", totalBefore));
        discountLabel.setText(String.format("Discount: ₪ %.2f", totalDisc));
        totalAfterLabel.setText(String.format("Total After Discount: ₪ %.2f", finalTot));

        if (compUsed > 0) {
            compBalanceLabel.setText(String.format("Applied ₪%.2f of store credit", compUsed));
        } else {
            compBalanceLabel.setText(String.format("You have ₪%.2f store credit", userCompBalance));
        }

        completeBtn.setText(String.format("Complete Order (₪%.2f)", payNow));
    }

    // compute final total (used by bindings to validate payment coverage)
    private double computeFinalTotal() {
        double afterSale = totalBefore - saleDiscount;
        double vipDisc = isVipUser ? afterSale * 0.10 : 0.0;
        double deliveryFee = deliveryRadio.isSelected() ? 10.0 : 0.0;
        return afterSale - vipDisc + deliveryFee;
    }

    // image loader with graceful fallbacks
    private Image buildImage(Basket b) {
        try {
            if (b.getCustomBouquet() != null) return loadRes("/image/custom.png");
            Object raw = (b.getProduct() != null) ? b.getProduct().getImage() : null;
            return loadFromProductField(raw, "/image/custom.png");
        } catch (Exception ignored) {
            return new Image(new ByteArrayInputStream(new byte[0]), 1, 1, true, true);
        }
    }

    // tries bytes/url/resource paths; falls back to provided resource
    private Image loadFromProductField(Object raw, String fallbackRes) {
        if (raw instanceof byte[] bytes && bytes.length > 0) return new Image(new ByteArrayInputStream(bytes));
        String s = (raw instanceof String) ? ((String) raw).trim() : null;
        if (s != null && !s.isEmpty()) {
            if (s.startsWith("http://") || s.startsWith("https://")) return new Image(s, true);
            if (s.startsWith("/")) {
                InputStream is = getClass().getResourceAsStream(s);
                if (is != null) return new Image(is);
            }
            for (String tryPath : new String[]{"/image/" + s, "/images/" + s, "/" + s}) {
                InputStream is = getClass().getResourceAsStream(tryPath);
                if (is != null) return new Image(is);
            }
        }
        return loadRes(fallbackRes);
    }

    // loads a classpath resource or returns a 1x1 empty image
    private Image loadRes(String path) {
        InputStream is = getClass().getResourceAsStream(path);
        if (is != null) return new Image(is);
        return new Image(new ByteArrayInputStream(new byte[0]), 1, 1, true, true);
    }

    // close and unregister
    private void closeWindow() {
        EventBus.getDefault().unregister(this);
        ((Stage) basketTable.getScene().getWindow()).close();
    }

    @FXML private void goBack() { closeWindow(); }
    @FXML private void onClose() { EventBus.getDefault().unregister(this); }
}

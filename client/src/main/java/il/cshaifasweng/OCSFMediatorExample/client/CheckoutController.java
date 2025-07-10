package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OrderDTO;
import il.cshaifasweng.OCSFMediatorExample.entities.Basket;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CheckoutController implements Initializable {

    /* ------------ FXML -------------- */
    @FXML private TableView<Basket> basketTable;
    @FXML private TableColumn<Basket,String> nameCol;
    @FXML private TableColumn<Basket,Integer> amtCol;
    @FXML private TableColumn<Basket,Double> priceCol;
    @FXML private Label  totalLabel;

    @FXML private RadioButton savedCardRadio, addCardRadio;
    @FXML private Button addCardButton;

    @FXML private RadioButton pickupRadio, deliveryRadio;
    @FXML private ComboBox<String> branchCombo;
    @FXML private TextField cityField, streetField, houseField, zipField;

    @FXML private Button completeBtn;
    @FXML private ImageView logoImage;

    /* ------------ data -------------- */
    private final List<Basket> items = new ArrayList<>();
    private final BooleanProperty hasCardProperty = new SimpleBooleanProperty(false);

    /* called from BasketController */
    public void initData(List<Basket> copy) {
        items.clear();
        items.addAll(copy);
        basketTable.getItems().setAll(items);
        updateTotal();
        requestSavedCard();
    }

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
        pickupRadio   .setToggleGroup(fulGrp);
        deliveryRadio .setToggleGroup(fulGrp);

        branchCombo.getItems().addAll("Jerusalem","Haifa","Tel-Aviv");
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
                // PICK-UP → branch must be chosen
                pickupRadio.selectedProperty()
                        .and(branchCombo.valueProperty().isNotNull())
                        // DELIVERY → all address boxes non-empty
                        .or(deliveryRadio.selectedProperty()
                                .and(cityField.textProperty().isNotEmpty())
                                .and(streetField.textProperty().isNotEmpty())
                                .and(houseField.textProperty().isNotEmpty())
                                .and(zipField.textProperty().isNotEmpty()));

        BooleanBinding ready =
                paymentOk
                        .and(fulGrp.selectedToggleProperty().isNotNull())
                        .and(fulfilOk);

        completeBtn.disableProperty().bind(ready.not());
        completeBtn.setOnAction(e -> submitOrder());

        EventBus.getDefault().register(this);
    }

    /* -------------- utils -------------- */
    private BooleanBinding addressOrBranchValid() {
        return pickupRadio.selectedProperty().or(
                cityField.textProperty().isNotEmpty()
                        .and(streetField.textProperty().isNotEmpty())
                        .and(houseField.textProperty().isNotEmpty())
                        .and(zipField.textProperty().isNotEmpty()));
    }
    private void updateTotal() {
        double total = items.stream().mapToDouble(Basket::getPrice).sum();
        totalLabel.setText(String.format("₪ %.2f", total));
    }
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

    /* -------------- submit -------------- */
    private void submitOrder() {
        try {
            SimpleClient.getClient().sendToServer(
                    new Msg("NEW_ORDER", buildDto()));
        } catch (Exception e){e.printStackTrace();}
    }
    private OrderDTO buildDto() {
        String type  = pickupRadio.isSelected()?"PICKUP":"DELIVERY";
        String info  = pickupRadio.isSelected()? branchCombo.getValue()
                : String.format("%s, %s %s (%s)",
                cityField.getText(), streetField.getText(),
                houseField.getText(), zipField.getText());
        return new OrderDTO(SceneController.loggedUsername,
                items.stream().map(Basket::getId).toList(),
                type, info);
    }

    /* -------------- server msgs -------------- */
    @Subscribe
    public void handleServerMsg(Msg m) {
        Platform.runLater(() -> {
            switch (m.getAction()) {
                case "HAS_CARD" -> {
                    boolean has = (Boolean) m.getData();
                    hasCardProperty.set(has);
                    savedCardRadio.setText(has? "Use saved card" : "No card on file");
                    savedCardRadio.setDisable(!has);
                }

                case "ORDER_OK" -> {
                    Alert ok = new Alert(Alert.AlertType.INFORMATION,"Order completed!");
                    ok.showAndWait();

                    // ask server for fresh basket (will come back empty)
                    try { SimpleClient.getClient().sendToServer(
                            new Msg("FETCH_BASKET", SceneController.loggedUsername)); }
                    catch(Exception e){e.printStackTrace();}

                    closeWindow();
                }

                case "ORDER_FAIL" -> {
                    new Alert(Alert.AlertType.ERROR,"Order failed, please check details.")
                            .showAndWait();
                }
            }
        });
    }

    /* -------------- misc -------------- */
    private void closeWindow() {
        ((Stage) basketTable.getScene().getWindow()).close();
    }
    @FXML private void goBack(){ closeWindow(); }
    @FXML private void onClose(){ EventBus.getDefault().unregister(this); }
}

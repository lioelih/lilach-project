package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Basket;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BasketController {

    @FXML private TableView<Basket> basketTable;
    @FXML private TableColumn<Basket, String> nameColumn;
    @FXML private TableColumn<Basket, Integer> amountColumn;
    @FXML private TableColumn<Basket, Double> priceColumn;
    @FXML private TableColumn<Basket, Void> removeColumn;

    @FXML private Label totalLabel;
    @FXML private Button confirmButton;

    @FXML
    public void initialize() {
        System.out.println("[BasketController] Initializing...");

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getProduct().getName()));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        addRemoveButtonToTable();

        try {
            System.out.println("[BasketController] Sending FETCH_BASKET for user: " + SceneController.loggedUsername);
            SimpleClient.getClient().ensureConnected();
            SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername));
        } catch (IOException e) {
            System.err.println("[BasketController] Failed to send FETCH_BASKET");
            e.printStackTrace();
        }

        basketTable.setEditable(true); // Enables editing globally
        amountColumn.setEditable(true); // Enables editing just for this column
        amountColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

        amountColumn.setOnEditCommit(event -> {
            Basket basketItem = event.getRowValue();
            int newAmount = event.getNewValue();
            basketItem.setAmount(newAmount);
            basketItem.setPrice(newAmount * basketItem.getProduct().getPrice()); // update total price

            updateTotal();
            try {
                SimpleClient.getClient().sendToServer(new Msg("UPDATE_BASKET_AMOUNT", basketItem));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        confirmButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("checkout.fxml"));
                Scene scene = new Scene(loader.load());

                CheckoutController cc = loader.getController();
                cc.initData(new ArrayList<>(basketTable.getItems()));   // pass copy

                Stage st = new Stage();
                st.setTitle("Checkout");
                st.setScene(scene);

                /* centre in safe visual bounds */
                Rectangle2D vb = Screen.getPrimary().getVisualBounds();
                st.setX(vb.getMinX() + (vb.getWidth() - 900)/2);   // 900 = prefWidth
                st.setY(vb.getMinY() + (vb.getHeight() - 600)/2);  // 600 = prefHeight

                st.show();
            } catch (IOException ex) { ex.printStackTrace(); }
        });


    }

    private void addRemoveButtonToTable() {
        removeColumn.setCellFactory(param -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");

            {
                removeBtn.setOnAction(e -> {
                    Basket basketItem = getTableView().getItems().get(getIndex());
                    try {
                        SimpleClient.getClient().sendToServer(new Msg("REMOVE_BASKET_ITEM", basketItem));
                        getTableView().getItems().remove(basketItem);
                        updateTotal();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });
    }

    @Subscribe
    public void handleBasketFetched(Msg msg) {
        if (!msg.getAction().equals("BASKET_FETCHED")) return;

        System.out.println("[BasketController] Received BASKET_FETCHED from server");

        Platform.runLater(() -> {
            List<Basket> basketItems = (List<Basket>) msg.getData();
            System.out.println("[BasketController] Populating table with " + basketItems.size() + " items");
            basketTable.getItems().setAll(basketItems);
            updateTotal();
        });
    }

    private void updateTotal() {
        double total = basketTable.getItems().stream()
                .mapToDouble(Basket::getPrice)
                .sum();
        totalLabel.setText("Total: " + total + " NIS");
    }

    @FXML
    private void onClose() {
        EventBus.getDefault().unregister(this);
    }
}

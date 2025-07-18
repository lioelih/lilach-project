package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Basket;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
    @FXML private Label discountLabel;
    @FXML private Label afterDiscountLabel;
    @FXML private Button confirmButton;

    private List<Basket> basketItems;
    private List<Sale> sales;
    private double total, discount;

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

        basketTable.setEditable(true);
        amountColumn.setEditable(true);
        amountColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

        amountColumn.setOnEditCommit(event -> {
            Basket basketItem = event.getRowValue();
            int newAmount = event.getNewValue();
            basketItem.setAmount(newAmount);
            basketItem.setPrice(newAmount * basketItem.getProduct().getPrice());
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
                cc.initData(new ArrayList<>(basketItems), total, discount); // Pass a copy

                Stage st = new Stage();
                st.setTitle("Checkout");
                st.setScene(scene);

                Rectangle2D vb = Screen.getPrimary().getVisualBounds();
                st.setX(vb.getMinX() + (vb.getWidth() - 900) / 2);
                st.setY(vb.getMinY() + (vb.getHeight() - 600) / 2);

                st.show();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void addRemoveButtonToTable() {
        removeColumn.setCellFactory(param -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");

            {
                removeBtn.setOnAction(e -> {
                    Basket basketItem = getTableView().getItems().get(getIndex());
                    try {
                        basketItems.remove(basketItem); // remove from the field
                        SimpleClient.getClient().sendToServer(new Msg("REMOVE_BASKET_ITEM", basketItem));
                        refreshTable();
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
        this.basketItems = (List<Basket>) msg.getData();
        System.out.println("[BasketController] Received BASKET_FETCHED from server");

        Platform.runLater(() -> {
            System.out.println("[BasketController] Populating table with " + basketItems.size() + " items");
            refreshTable();
            updateTotal();
        });
    }

    private void refreshTable() {
        basketTable.setItems(FXCollections.observableList(basketItems));
    }

    private void updateTotal() {
        total = basketItems.stream()
                .mapToDouble(Basket::getPrice)
                .sum();
        discount = Sale.calculateTotalDiscount(basketItems, sales);
        double totalAfter = total - discount;
        totalLabel.setText("Total: " + total + " NIS");
        discountLabel.setText("Discount: " + discount + " NIS");
        afterDiscountLabel.setText("Total After Discount: " + totalAfter + " NIS");

    }

    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }

    @FXML
    private void onClose() {
        EventBus.getDefault().unregister(this);
    }
}

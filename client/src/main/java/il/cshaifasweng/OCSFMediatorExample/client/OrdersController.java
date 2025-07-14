package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class OrdersController {

    @FXML private RadioButton rbMine, rbAllUsers, rbAllOrders;
    @FXML private ToggleGroup scopeGroup;
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> colId;
    @FXML private TableColumn<Order, Integer> colUser;
    @FXML private TableColumn<Order, String> colFulfil;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, Double> colPrice;
    @FXML private TableColumn<Order, Void> colViewProducts;
    @FXML private TableColumn<Order, Void> colMarkReceived;

    private List<Order> allOrders;

    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);

        // Table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colFulfil.setCellValueFactory(new PropertyValueFactory<>("fulfilInfo"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusString"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        // TODO: Add custom cell factories for buttons later

        // Default view: my orders
        fetchOrders("MY_ORDERS");

        // Toggle group logic
        scopeGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
            if (newToggle == rbMine) fetchOrders("MY_ORDERS");
            else if (newToggle == rbAllUsers) fetchOrders("ALL_USER_ORDERS");
            else if (newToggle == rbAllOrders) fetchOrders("ALL_ORDERS");
        });
    }

    private void fetchOrders(String scope) {
        try {
            Msg msg = new Msg("FETCH_ORDERS", scope);
            SimpleClient.getClient().sendToServer(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goHome() {
        EventBus.getDefault().unregister(this);
        SceneController.switchScene("home");
    }
}

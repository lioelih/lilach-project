package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.List;

public class OrdersController {

    @FXML private RadioButton rbMyOrders;
    @FXML private RadioButton rbAllOrders;
    @FXML private ToggleGroup scopeGroup;

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> colId;
    @FXML private TableColumn<Order, Integer> colUser;
    @FXML private TableColumn<Order, String> colFulfil;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, Double> colPrice;

    @FXML private Button goHomeButton;
    @FXML
    public void initialize() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        colId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colFulfil.setCellValueFactory(new PropertyValueFactory<>("fulfilInfo"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusString"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        // request initial load
        requestOrders();

        // radio button change -> refresh
        scopeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> requestOrders());

        goHomeButton.setOnAction(e -> closeWindow());
    }

    private void requestOrders() {
        String scope = rbAllOrders.isSelected() ? "ALL" : "MINE";
        try {
            SimpleClient.ensureConnected();
            SimpleClient.getClient().sendToServer(
                    new Msg("FETCH_ORDERS",
                            new Object[]{SceneController.loggedUsername, scope}));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void handleOrders(Msg msg) {
        if (!"FETCH_ORDERS_OK".equals(msg.getAction())) return;
        System.out.println("[Client] Orders received");
        List<Order> orders = (List<Order>) msg.getData();
        Platform.runLater(() -> ordersTable.getItems().setAll(orders));
    }

    private void closeWindow() {
        // Ensure we clean up the EventBus registration
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        // Close only this window (stage)
        goHomeButton.getScene().getWindow().hide();
    }

    @FXML
    private void onClose() {
        EventBus.getDefault().unregister(this);
    }
}

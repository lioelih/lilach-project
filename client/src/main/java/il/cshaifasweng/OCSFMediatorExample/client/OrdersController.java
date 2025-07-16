package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OrderDisplayDTO;
import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.List;

public class OrdersController {

    @FXML private ImageView logoImage;
    @FXML private Button goHomeButton;
    @FXML private RadioButton rbMine;
    @FXML private RadioButton rbAllOrders;

    @FXML private TableView<OrderDisplayDTO> ordersTable;
    @FXML private TableColumn<OrderDisplayDTO, Integer> colId;
    @FXML private TableColumn<OrderDisplayDTO, String> colUser;
    @FXML private TableColumn<OrderDisplayDTO, String> colFulfil;
    @FXML private TableColumn<OrderDisplayDTO, String> colStatus;
    @FXML private TableColumn<OrderDisplayDTO, Double> colPrice;
    @FXML private TableColumn<OrderDisplayDTO, Void> colProducts;
    @FXML private TableColumn<OrderDisplayDTO, Void> colActions;

    @FXML
    public void initialize() {
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        setupColumns();

        goHomeButton.setOnAction(e -> {
            EventBus.getDefault().unregister(this);
            SceneController.switchScene("home");
        });

        rbMine.setOnAction(e -> requestOrders("MINE"));
        rbAllOrders.setOnAction(e -> requestOrders("ALL"));

        requestOrders("MINE");
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFulfil.setCellValueFactory(new PropertyValueFactory<>("fulfilment"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        colProducts.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");

            {
                viewBtn.setOnAction(e -> {
                    OrderDisplayDTO order = getTableView().getItems().get(getIndex());
                    fetchProductsForOrder(order.getId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button markBtn = new Button("Mark as Received");
            private final Button cancelBtn = new Button("Cancel Order");
            private final HBox buttons = new HBox(5, markBtn, cancelBtn);

            {
                markBtn.setOnAction(e -> {
                    OrderDisplayDTO order = getTableView().getItems().get(getIndex());
                    markAsReceived(order.getId());
                });

                cancelBtn.setOnAction(e -> {
                    OrderDisplayDTO order = getTableView().getItems().get(getIndex());
                    cancelOrder(order.getId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void requestOrders(String scope) {
        if (!SimpleClient.ensureConnected()) return;
        try {
            SimpleClient.getClient().sendToServer(new Msg("FETCH_ORDERS", new String[]{SceneController.loggedUsername, scope}));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void markAsReceived(int orderId) {
        try {
            SimpleClient.getClient().sendToServer(new Msg("MARK_ORDER_RECEIVED", orderId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cancelOrder(int orderId) {
        System.out.println("[Client] Cancel order requested for order ID: " + orderId);
        // Implement cancellation logic later
    }

    private void fetchProductsForOrder(int orderId) {
        try {
            SimpleClient.getClient().sendToServer(new Msg("FETCH_ORDER_PRODUCTS", orderId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void handleOrdersFetched(Msg msg) {
        if (!"FETCH_ORDERS_OK".equals(msg.getAction())) return;

        List<OrderDisplayDTO> orders = (List<OrderDisplayDTO>) msg.getData();
        Platform.runLater(() -> ordersTable.getItems().setAll(orders));
    }

    @Subscribe
    public void handleProductsFetched(Msg msg) {
        if (!"FETCH_ORDER_PRODUCTS_OK".equals(msg.getAction())) return;

        List<Product> products = (List<Product>) msg.getData();
        Platform.runLater(() -> showProductsWindow(products));
    }

    @Subscribe
    public void handleOrderMarkedAsReceived(Msg msg) {
        if ("MARK_ORDER_RECEIVED_OK".equals(msg.getAction())) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Order Received");
                alert.setHeaderText(null);
                alert.setContentText("The order has been marked as received.");
                alert.showAndWait();

                // Refresh the table after marking as received
                requestOrders(rbMine.isSelected() ? "MINE" : "ALL");
            });
        }
    }

    private void showProductsWindow(List<Product> products) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Products in Order");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        for (Product product : products) {
            Label label = new Label(product.getName() + " (" + product.getType() + ") - " +
                    String.format("%.2f₪", product.getPrice()));
            layout.getChildren().add(label);
        }

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> popup.close());
        layout.getChildren().add(closeBtn);

        popup.setScene(new Scene(layout, 300, 400));
        popup.showAndWait();
    }
}

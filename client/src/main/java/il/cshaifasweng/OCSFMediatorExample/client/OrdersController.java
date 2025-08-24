package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OrderDetailsDTO;
import il.cshaifasweng.OrderDisplayDTO;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/*
 * orders controller:
 * - fetches orders for the current user or (if worker+) all/branch orders
 * - renders a table with status, pricing, recipient, deadline, and actions
 * - supports viewing line items, marking as received, and canceling with refund logic
 * - reacts to server pushes to keep the table fresh
 */
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
    @FXML private TableColumn<OrderDisplayDTO, Double> colCompUsed;
    @FXML private TableColumn<OrderDisplayDTO, Double> colPrice;
    @FXML private TableColumn<OrderDisplayDTO, LocalDateTime> colDeadline;
    @FXML private TableColumn<OrderDisplayDTO, String> colRecipient;
    @FXML private TableColumn<OrderDisplayDTO, String> colGreeting;
    @FXML private TableColumn<OrderDisplayDTO, Void> colProducts;
    @FXML private TableColumn<OrderDisplayDTO, Void> colActions;

    private String currentScope = "MINE";

    private static boolean isManagerOrAdmin() {
        return SceneController.hasPermission(User.Role.MANAGER)
                || SceneController.hasPermission(User.Role.ADMIN);
    }

    private static String effectiveAllScope() {
        return isManagerOrAdmin() ? "ALL" : "ALL_USERS";
    }

    /* init ui, wire filters, and fetch initial data */
    @FXML
    public void initialize() {
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);

        setupColumns();

        goHomeButton.setOnAction(e -> {
            EventBus.getDefault().unregister(this);
            SceneController.switchScene("home");
        });

        rbMine.setOnAction(e -> {
            currentScope = "MINE";
            requestOrders(currentScope);
        });
        rbAllOrders.setOnAction(e -> {
            currentScope = effectiveAllScope();
            requestOrders(currentScope);
        });

        boolean canWorker = SceneController.hasPermission(User.Role.WORKER);
        rbAllOrders.setVisible(canWorker);
        rbAllOrders.setManaged(canWorker);

        currentScope = "MINE";
        requestOrders(currentScope);
    }

    /* configure table columns, row styling, and per-row action buttons */
    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFulfil.setCellValueFactory(new PropertyValueFactory<>("fulfilment"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        colRecipient.setCellValueFactory(new PropertyValueFactory<>("recipient"));
        colGreeting.setCellValueFactory(new PropertyValueFactory<>("greeting"));
        colCompUsed.setCellValueFactory(new PropertyValueFactory<>("compensationUsed"));

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

        colStatus.setCellValueFactory(cell -> {
            int s = cell.getValue().getStatus();
            String txt = s == Order.STATUS_RECEIVED ? "Received"
                    : s == Order.STATUS_CANCELLED ? "Cancelled"
                    : "Pending";
            return new SimpleStringProperty(txt);
        });

        // color rows by status (cancelled = red tint, received = green tint)
        ordersTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(OrderDisplayDTO o, boolean empty) {
                super.updateItem(o, empty);
                if (empty || o == null) {
                    setStyle("");
                } else if (o.isCancelled()) {
                    setStyle("-fx-background-color: #f8d7da;");
                } else if (o.isReceived()) {
                    setStyle("-fx-background-color: #d4edda;");
                } else {
                    setStyle("");
                }
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button markBtn = new Button("Mark Received");
            private final Button cancelBtn = new Button("Cancel Order");
            private final HBox box = new HBox(5, markBtn, cancelBtn);

            {
                // mark received: send the order id to the server
                markBtn.setOnAction(e -> {
                    OrderDisplayDTO o = getCurrent();
                    markBtn.setDisable(true);
                    cancelBtn.setDisable(true);
                    markAsReceived(o.getId());
                });
                // cancel: confirm, then send cancel for the selected order
                cancelBtn.setOnAction(e -> {
                    OrderDisplayDTO o = getCurrent();
                    cancelBtn.setDisable(true);
                    markBtn.setDisable(true);
                    confirmCancel(o);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    OrderDisplayDTO o = getCurrent();
                    boolean pending = o.getStatus() == Order.STATUS_PENDING;
                    markBtn.setVisible(pending && SceneController.hasPermission(User.Role.WORKER));
                    cancelBtn.setVisible(pending);
                    setGraphic(box);
                }
            }

            private OrderDisplayDTO getCurrent() {
                return getTableView().getItems().get(getIndex());
            }
        });
    }

    /* send fetch request for the chosen scope */
    private void requestOrders(String scope) {
        if (!SimpleClient.ensureConnected()) return;
        try {
            SimpleClient.getClient().sendToServer(new Msg("FETCH_ORDERS", new String[]{SceneController.loggedUsername, scope}));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* send mark-as-received request for a given order id */
    private void markAsReceived(int orderId) {
        try {
            SimpleClient.getClient().sendToServer(new Msg("MARK_ORDER_RECEIVED", orderId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* confirm cancellation and compute refund percentage by time remaining */
    private void confirmCancel(OrderDisplayDTO o) {
        LocalDateTime now = LocalDateTime.now();
        long minsLeft = Duration.between(now, o.getDeadline()).toMinutes();

        if (minsLeft <= 0) {
            new Alert(Alert.AlertType.INFORMATION, "Order is past deadline, please contact support.").showAndWait();
            return;
        }

        double pct = minsLeft >= 180 ? 1.0 : minsLeft >= 60 ? 0.5 : 0.0;
        double refund = o.getTotalPrice() * pct;
        String msg = String.format("Once you cancel this order, you’ll receive ₪%.2f (%.0f%%). Are you sure?", refund, pct * 100);

        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                SimpleClient.getClient().sendToServer(new Msg("CANCEL_ORDER", o.getId()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /* receive orders list and refresh table */
    @Subscribe
    public void handleOrdersFetched(Msg msg) {
        if (!"FETCH_ORDERS_OK".equals(msg.getAction())) return;
        List<OrderDisplayDTO> orders = (List<OrderDisplayDTO>) msg.getData();
        Platform.runLater(() -> {
            ordersTable.getItems().setAll(orders);
            ordersTable.refresh();
        });
    }

    /* ask server for the products in a selected order */
    private void fetchProductsForOrder(int orderId) {
        try {
            SimpleClient.getClient().sendToServer(new Msg("FETCH_ORDER_PRODUCTS", orderId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* handle product details fetch responses (ok/fail) */
    @Subscribe
    public void handleProductsFetched(Msg msg) {
        if ("FETCH_ORDER_PRODUCTS_OK".equals(msg.getAction())) {
            OrderDetailsDTO details = (OrderDetailsDTO) msg.getData();
            Platform.runLater(() -> showOrderDetailsWindow(details));
        } else if ("FETCH_ORDER_PRODUCTS_FAIL".equals(msg.getAction())) {
            Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, (String) msg.getData()).showAndWait());
        }
    }

    /* inform user on successful receipt and refresh the current scope */
    @Subscribe
    public void handleOrderMarkedAsReceived(Msg msg) {
        if ("MARK_ORDER_RECEIVED_OK".equals(msg.getAction())) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Order Received");
                alert.setHeaderText(null);
                alert.setContentText("The order has been marked as received.");
                alert.showAndWait();
                requestOrders(rbMine.isSelected() ? "MINE" : effectiveAllScope());
            });
        }
    }

    /* inform user on cancel result and refresh the current scope */
    @Subscribe
    public void handleCancelResponse(Msg m) {
        if ("CANCEL_OK".equals(m.getAction())) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) m.getData();
            Platform.runLater(() -> {
                new Alert(Alert.AlertType.INFORMATION,
                        String.format("Order %d cancelled. ₪%.2f added to your balance.",
                                data.get("orderId"), data.get("refundAmt"))).showAndWait();
                requestOrders(rbMine.isSelected() ? "MINE" : effectiveAllScope());
            });
        } else if ("CANCEL_FAIL".equals(m.getAction())) {
            Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, (String) m.getData()).showAndWait());
        }
    }

    /* build and show a modal window with order line items and a cost summary */
    private void showOrderDetailsWindow(OrderDetailsDTO d) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Order Details");

        // table of line items
        TableView<OrderDetailsDTO.Line> table = new TableView<>();
        table.setItems(FXCollections.observableList(d.getLines()));

        TableColumn<OrderDetailsDTO.Line, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductName()));
        nameCol.setCellFactory(tc -> new TableCell<>() {
            private final Text text = new Text();
            {
                // wrap product names within the column width
                text.wrappingWidthProperty().bind(nameCol.widthProperty().subtract(10));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    text.setText(item);
                    setGraphic(text);
                }
            }
        });

        TableColumn<OrderDetailsDTO.Line, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantity()).asObject());

        TableColumn<OrderDetailsDTO.Line, Double> priceCol = new TableColumn<>("Line ₪");
        priceCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getLinePrice()).asObject());

        table.getColumns().addAll(nameCol, qtyCol, priceCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // summary box
        VBox summary = new VBox(4,
                new Label(String.format("Subtotal:       ₪%.2f", d.getSubtotal())),
                new Label(String.format("Sale Discount: - ₪%.2f", d.getSaleDiscount())),
                new Label(String.format("VIP Discount:  - ₪%.2f", d.getVipDiscount())),
                new Label(String.format("Delivery Fee:  + ₪%.2f", d.getDeliveryFee())),
                new Separator(),
                new Label(String.format("Total Paid:     ₪%.2f", d.getTotal())),
                new Label(String.format("Comp Used:      –₪%.2f", d.getCompensationUsed()))
        );
        summary.setPadding(new Insets(10));
        summary.setStyle("-fx-font-weight: bold;");

        // close button
        Button close = new Button("Close");
        close.setOnAction(e -> popup.close());

        // layout
        VBox root = new VBox(10, table, summary, close);
        root.setPadding(new Insets(10));
        popup.setScene(new Scene(root, 450, 550));
        popup.showAndWait();
    }

    /* adapt ui and scope to role/vip changes and refresh data */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLocalRoleVipChanged(Msg msg) {
        if (!"LOCAL_ROLE_VIP_CHANGED".equals(msg.getAction())) return;

        boolean canWorker = SceneController.hasPermission(User.Role.WORKER);
        rbAllOrders.setVisible(canWorker);
        rbAllOrders.setManaged(canWorker);
        rbAllOrders.setText(isManagerOrAdmin() ? "All Orders" : "Branch Orders");

        if (!canWorker && rbAllOrders.isSelected()) {
            rbMine.setSelected(true);
            currentScope = "MINE";
        } else {
            currentScope = rbAllOrders.isSelected() ? effectiveAllScope() : "MINE";
        }

        requestOrders(currentScope);
        ordersTable.refresh();
    }

    /* refresh table when server signals orders data changed */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrdersDirty(Msg msg) {
        if (!"ORDERS_DIRTY".equals(msg.getAction())) return;
        requestOrders(currentScope);
    }
}

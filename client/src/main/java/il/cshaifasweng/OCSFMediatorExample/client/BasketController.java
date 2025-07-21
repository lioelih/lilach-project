package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Basket;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
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
import javafx.beans.binding.Bindings;
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

    private boolean isVip;        // ← set this at initialize()
    private static final double VIP_THRESHOLD = 50.0;
    private static final double VIP_RATE      = 0.10;
    @FXML
    public void initialize() {
        System.out.println("[BasketController] Initializing...");

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        nameColumn.setCellValueFactory(cellData -> {
            Basket b = cellData.getValue();
            if (b.getCustomBouquet() != null) {
                return new SimpleStringProperty(
                        "Custom: " + b.getCustomBouquet().getName()
                );
            } else {
                return new SimpleStringProperty(b.getProduct().getName());
            }
        });
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
            if (newAmount < 1) {
                // reset to 1 and warn
                newAmount = 1;
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "Quantity must be at least 1. Resetting to 1.");
                alert.setHeaderText(null);
                alert.showAndWait();
            }
            basketItem.setAmount(newAmount);
            basketItem.setPrice(newAmount * basketItem.getProduct().getPrice());
            basketTable.refresh();    // make sure table shows the clamped value
            updateTotal();
            try {
                SimpleClient.getClient().sendToServer(
                        new Msg("UPDATE_BASKET_AMOUNT", basketItem)
                );
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
                // 1) recompute subtotal (all lines)
                double subtotal = basketItems.stream()
                        .mapToDouble(Basket::getPrice)
                        .sum();

                // 2) recompute sale discount (only on real products, not customs)
                List<Basket> productLines = basketItems.stream()
                        .filter(b -> b.getProduct() != null)
                        .toList();
                double saleDiscount = Sale.calculateTotalDiscount(productLines, sales);

                // 3) pass those real values into initData()
                cc.initData(
                        new ArrayList<>(basketItems),  // copy of the list
                        subtotal,                      // total before discounts
                        saleDiscount                   // sale discount amount
                );
                Stage st = new Stage();
                st.setTitle("Checkout");
                st.setScene(scene);

                Rectangle2D vb = Screen.getPrimary().getVisualBounds();
                st.setX(vb.getMinX() + (vb.getWidth() - 900) / 2);
                st.setY(vb.getMinY() + (vb.getHeight() - 600) / 2);
                st.setMaximized(true);
                st.show();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        try {
            SimpleClient.getClient().sendToServer(
                    new Msg("FETCH_USER", SceneController.loggedUsername));
        } catch(IOException ex){ ex.printStackTrace(); }
    }

    @Subscribe
    public void onUserFetched(Msg m) {
        if (!"FETCH_USER".equals(m.getAction())) return;
        User me = (User) m.getData();
        isVip = me.isVIP();
        // now that we know VIP status, we can recalc totals in case
        Platform.runLater(this::updateTotal);
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

    @Subscribe
    public void onBasketUpdated(Msg msg) {
        if (!"BASKET_UPDATED".equals(msg.getAction())) return;
        try {
            SimpleClient.getClient().sendToServer(
                    new Msg("FETCH_BASKET", SceneController.loggedUsername));
        } catch(IOException e) { e.printStackTrace(); }
    }

    private void refreshTable() {
        basketTable.setItems(FXCollections.observableList(basketItems));
    }

    private void updateTotal() {
        // 1) raw subtotal, all lines:
        double subtotal = basketItems.stream()
                .mapToDouble(Basket::getPrice)
                .sum();

        // 2) sale discount only applies to product‐lines:
        List<Basket> productLines = basketItems.stream()
                .filter(b -> b.getProduct() != null)   // skip custom bouquets
                .toList();
        double saleDiscount = Sale.calculateTotalDiscount(productLines, sales);

        // 3) amount after sale discount:
        double afterSale = subtotal - saleDiscount;

        // 4) VIP discount only if VIP + afterSale ≥ 50:
        double vipDiscount = 0;
        if (isVip && afterSale >= VIP_THRESHOLD) {
            vipDiscount = afterSale * VIP_RATE;
        }

        // 5) combine discounts, compute final total:
        double totalDiscount = saleDiscount + vipDiscount;
        double finalTotal    = subtotal - totalDiscount;

        // 6) push to UI
        totalLabel.setText(         String.format("Total: %.2f NIS", subtotal));
        discountLabel.setText(      String.format("Discount: %.2f NIS", totalDiscount));
        afterDiscountLabel.setText(
                String.format("Total After Discount: %.2f NIS", finalTotal));
        confirmButton.setDisable(basketItems.isEmpty());
        this.total    = subtotal;
        this.discount = saleDiscount;
    }

    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }

    @FXML
    private void onClose() {
        EventBus.getDefault().unregister(this);
    }
}

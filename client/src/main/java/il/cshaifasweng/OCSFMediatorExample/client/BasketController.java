package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Basket;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    @FXML private TableColumn<Basket, ImageView> imageColumn;
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
    private boolean isVip;
    private int userBranchId;

    private static final double VIP_THRESHOLD = 50.0;
    private static final double VIP_RATE = 0.10;

    @FXML
    public void initialize() {
        try { EventBus.getDefault().unregister(this); } catch (Exception ignored) {}
        EventBus.getDefault().register(this);

        imageColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(buildImage(cell.getValue())));
        nameColumn.setCellValueFactory(cellData -> {
            Basket b = cellData.getValue();
            if (b.getCustomBouquet() != null) {
                return new SimpleStringProperty("Custom: " + b.getCustomBouquet().getName());
            } else {
                return new SimpleStringProperty(b.getProduct().getName());
            }
        });
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        addRemoveButtonToTable();
        basketItems = new ArrayList<>();

        try {
            SimpleClient.getClient().ensureConnected();
            SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername));
            if (SceneController.loggedUsername != null) {
                SimpleClient.getClient().sendToServer(new Msg("FETCH_USER", SceneController.loggedUsername));
            }
        } catch (IOException e) { e.printStackTrace(); }

        basketTable.setEditable(true);
        amountColumn.setEditable(true);
        amountColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        amountColumn.setOnEditCommit(event -> {
            Basket item = event.getRowValue();
            int newAmt = event.getNewValue() < 1 ? 1 : event.getNewValue();
            if (event.getNewValue() < 1) {
                new Alert(Alert.AlertType.WARNING,"Quantity must be at least 1. Resetting to 1.").showAndWait();
            }
            item.setAmount(newAmt);
            double unitPrice = (item.getProduct() != null) ? item.getProduct().getPrice() : item.getCustomBouquet().getTotalPrice();
            item.setPrice(newAmt * unitPrice);
            basketTable.refresh();
            updateTotal();
            try { SimpleClient.getClient().sendToServer(new Msg("UPDATE_BASKET_AMOUNT", item)); } catch (IOException ex) { ex.printStackTrace(); }
        });

        confirmButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("checkout.fxml"));
                Scene scene = new Scene(loader.load());

                CheckoutController cc = loader.getController();
                double subtotal = basketItems.stream().mapToDouble(Basket::getPrice).sum();
                var productLines = basketItems.stream().filter(b -> b.getProduct() != null).toList();
                double saleDisc = Sale.calculateTotalDiscount(productLines, sales);
                cc.initData(new ArrayList<>(basketItems), subtotal, saleDisc);

                Stage st = new Stage();
                st.initOwner(confirmButton.getScene().getWindow());
                st.initModality(javafx.stage.Modality.WINDOW_MODAL);
                st.setTitle("Checkout");
                st.setScene(scene);

                Rectangle2D vb = Screen.getPrimary().getVisualBounds();
                st.setX(vb.getMinX() + (vb.getWidth() - 900) / 2);
                st.setY(vb.getMinY() + (vb.getHeight() - 600) / 2);
                st.setMaximized(true);


                confirmButton.setDisable(true);
                st.setOnHidden(ev -> confirmButton.setDisable(basketItems.isEmpty()));

                st.showAndWait();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

    }

    private ImageView buildImage(Basket b) {
        Image img;
        try {
            // If it's a custom bouquet, show the generic icon
            if (b.getCustomBouquet() != null) {
                img = new Image(getClass().getResourceAsStream("/image/custom.png"));
            } else {
                Object raw = (b.getProduct() != null) ? b.getProduct().getImage() : null;

                if (raw instanceof byte[] bytes && bytes.length > 0) {
                    img = new Image(new java.io.ByteArrayInputStream(bytes));
                } else {
                    String path = (raw instanceof String) ? ((String) raw).trim() : null;

                    if (path != null && !path.isEmpty()) {
                        if (path.startsWith("http://") || path.startsWith("https://")) {
                            img = new Image(path, true);
                        } else if (path.startsWith("/")) {
                            img = new Image(getClass().getResourceAsStream(path));
                        } else {
                            img = new Image(getClass().getResourceAsStream("/image/" + path));
                        }
                    } else {
                        img = new Image(getClass().getResourceAsStream("/image/custom.png"));
                    }
                }
            }
        } catch (Exception ex) {
            img = new Image(getClass().getResourceAsStream("/image/custom.png"));
        }

        ImageView iv = new ImageView(img);
        iv.setFitWidth(48);
        iv.setFitHeight(48);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        return iv;
    }


    @Subscribe
    public void onUserFetched(Msg m) {
        if (!"FETCH_USER".equals(m.getAction())) return;
        User me = (User) m.getData();
        isVip = me.isVIP();
        boolean isPrivileged = isVip || SceneController.hasPermission(SceneController.Role.WORKER);
        userBranchId = me.getBranch().getBranchId();
        if (!isPrivileged) {
            try { SimpleClient.getClient().sendToServer(new Msg("STOCK_BY_BRANCH", userBranchId)); } catch(IOException ex) { ex.printStackTrace(); }
        }
    }

    @Subscribe
    public void handleBasketFetched(Msg m) {
        if (!"BASKET_FETCHED".equals(m.getAction())) return;
        basketItems = new ArrayList<>((List<Basket>) m.getData());
        Platform.runLater(() -> { refreshTable(); updateTotal(); });
    }

    @Subscribe
    public void onBasketUpdated(Msg m) {
        if (!"BASKET_UPDATED".equals(m.getAction())) return;
        try { SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername)); } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void addRemoveButtonToTable() {
        removeColumn.setCellFactory(tv -> new TableCell<>() {
            private final Button btn = new Button("Remove");
            { btn.setOnAction(e -> {
                Basket b = getTableView().getItems().get(getIndex());
                try {
                    basketItems.remove(b);
                    SimpleClient.getClient().sendToServer(new Msg("REMOVE_BASKET_ITEM", b));
                    refreshTable();
                    updateTotal();
                } catch (IOException ex) { ex.printStackTrace(); }
            }); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void refreshTable() {
        basketTable.setItems(FXCollections.observableList(basketItems));
    }

    private void updateTotal() {
        double subtotal = basketItems.stream().mapToDouble(Basket::getPrice).sum();
        List<Basket> productLines = basketItems.stream().filter(b -> b.getProduct() != null).toList();
        double saleDisc = Sale.calculateTotalDiscount(productLines, sales);
        double afterSale = subtotal - saleDisc;
        double vipDisc = (isVip && afterSale >= VIP_THRESHOLD) ? afterSale * VIP_RATE : 0;
        double totDisc = saleDisc + vipDisc;
        double finalTotal = subtotal - totDisc;

        totalLabel.setText(String.format("Total: %.2f NIS", subtotal));
        discountLabel.setText(String.format("Discount: %.2f NIS", totDisc));
        afterDiscountLabel.setText(String.format("Total After Discount: %.2f NIS", finalTotal));
        confirmButton.setDisable(basketItems.isEmpty());
    }

    public void setSales(List<Sale> sales) { this.sales = sales; }

    @Subscribe
    public void onUserUpdated(Msg m) {
        if (!"USER_UPDATED".equals(m.getAction())) return;
        User updated = (User) m.getData();
        if (updated.getUsername().equals(SceneController.loggedUsername)) {
            try {
                SimpleClient.getClient().sendToServer(new Msg("FETCH_USER", updated.getUsername()));
                SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", updated.getUsername()));
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void onClose() { EventBus.getDefault().unregister(this); }
}

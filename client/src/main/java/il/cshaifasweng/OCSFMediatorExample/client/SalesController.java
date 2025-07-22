package il.cshaifasweng.OCSFMediatorExample.client;

import Events.CatalogEvent;
import Events.SalesEvent;
import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class SalesController {

    @FXML private ImageView logoImage;
    @FXML private Button addSaleButton;
    @FXML private TableView<Sale> salesTable;

    @FXML private TableColumn<Sale, Integer> colId;
    @FXML private TableColumn<Sale, String> colName;
    @FXML private TableColumn<Sale, String> colType;
    @FXML private TableColumn<Sale, Double> colValue;
    @FXML private TableColumn<Sale, String> colStart;
    @FXML private TableColumn<Sale, String> colEnd;
    @FXML private TableColumn<Sale, Integer> colBuyQty;
    @FXML private TableColumn<Sale, Integer> colGetQty;
    @FXML private TableColumn<Sale, String> colProducts;
    @FXML private TableColumn<Sale, Void> colActions;

    private List<Product> products;
    private List<Sale> sales;

    @FXML
    public void initialize() {
        try {
            SimpleClient.getClient().sendToServer(new Msg("GET_CATALOG", null));
            SimpleClient.getClient().sendToServer(new Msg("GET_SALES", null));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        setupTable();
        highlightActiveRows();

        addSaleButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("add_sale_page.fxml"));
                Scene scene = new Scene(loader.load());

                // Get the controller and pass the product list
                AddSaleController controller = loader.getController();
                controller.setProducts(products);
                controller.setSales(sales);

                Stage stage = new Stage();
                stage.setTitle("Add Sale");
                stage.setScene(scene);
                stage.show();
            } catch (IOException err) {
                err.printStackTrace();
            }
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDiscountType().name()));
        colValue.setCellValueFactory(new PropertyValueFactory<>("discountValue"));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        colStart.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStartDate() != null ? c.getValue().getStartDate().format(fmt) : ""));
        colEnd.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEndDate() != null ? c.getValue().getEndDate().format(fmt) : ""));

        colBuyQty.setCellValueFactory(new PropertyValueFactory<>("buyQuantity"));
        colGetQty.setCellValueFactory(new PropertyValueFactory<>("getQuantity"));

        colProducts.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProductIds().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "))
        ));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");

            {
                editBtn.setOnAction(e -> {
                    Sale selectedSale = getTableView().getItems().get(getIndex());
                    openEditPopup(selectedSale);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, editBtn));
            }
        });
    }

    private void highlightActiveRows() {
        salesTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(Sale sale, boolean empty) {
                super.updateItem(sale, empty);
                if (sale == null || empty) {
                    setStyle("");
                } else if (isSaleActive(sale)) {
                    setStyle("-fx-background-color: #d4edda;"); // Light green for active
                } else {
                    setStyle("-fx-background-color: #f8d7da;"); // Light red for inactive
                }
            }
        });
    }

    private boolean isSaleActive(Sale sale) {
        if (sale.getStartDate() == null || sale.getEndDate() == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return !sale.getStartDate().isAfter(now) && !sale.getEndDate().isBefore(now);
    }


    @Subscribe
    public void handleSalesFetched(SalesEvent event) {
        sales = event.getSales();
        System.out.println("[SalesController] fetched sales");
        Platform.runLater(() -> salesTable.getItems().setAll(sales));
    }

    @Subscribe
    public void handleItemsFetched(CatalogEvent event) {
        products = event.getProducts();
    }

    private void openEditPopup(Sale sale) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("view_update_sale.fxml"));
            Parent root = loader.load();

            ViewUpdateSaleController controller = loader.getController();
            controller.setSale(sale, products);

            Stage stage = new Stage();
            stage.setTitle("View/Update Sale");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.List;

public class CatalogController {

    @FXML private Button homeButton;
    @FXML private Button refreshButton;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> typeColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, String> imageColumn;

    @FXML
    public void initialize() { // Initialization of the catalog
        EventBus.getDefault().register(this);

        nameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        typeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));
        priceColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getPrice()));
        imageColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getImage())); // Sets each variable with the given data type from the database

        imageColumn.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitHeight(60);
                imageView.setFitWidth(100);
                imageView.setPreserveRatio(true);
            } // Loads each image individually with the given ratio

            @Override
            protected void updateItem(String path, boolean empty) { // Updates an item (usually for display)
                super.updateItem(path, empty);
                if (empty || path == null || path.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        imageView.setImage(new Image(path, true));
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                }
            }
        });

        addViewButtonColumn(); // << Add this column

        homeButton.setOnAction(e -> { // Sends us to home
            EventBus.getDefault().unregister(this);
            SceneController.switchScene("home");
        });

        refreshButton.setOnAction(e -> { // Refreshes the catalog by sending another "GET_CATALOG"
            try {
                SimpleClient.getClient().sendToServer("GET_CATALOG");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        try { // Displays catalog on initialization
            SimpleClient.getClient().sendToServer("GET_CATALOG");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addViewButtonColumn() { // Made for the View of product and its description
        TableColumn<Product, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(100);

        Callback<TableColumn<Product, Void>, TableCell<Product, Void>> cellFactory = param -> new TableCell<>() {
            private final Button viewBtn = new Button("View");

            {
                viewBtn.setOnAction(e -> {
                    Product product = getTableView().getItems().get(getIndex());
                    openProductPage(product);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
            }
        };

        actionCol.setCellFactory(cellFactory);
        productTable.getColumns().add(actionCol);
    }

    @Subscribe
    public void onCatalogReceived(CatalogEvent event) { // Will take a List received from the server, then split it into products using @Subscribe
        updateCatalog(event.getProducts());
    }

    public void updateCatalog(List<Product> products) { // Updates the Catalog
        Platform.runLater(() -> {
            productTable.getItems().setAll(products);
        });
    }

    private void openProductPage(Product product) { //Our Product_View window
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("product_view.fxml"));
            Scene scene = new Scene(loader.load());
            ProductViewController controller = loader.getController();
            controller.setProduct(product);

            Stage stage = new Stage();
            stage.setTitle("Product Details");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onClose() {
        EventBus.getDefault().unregister(this);
    }
}

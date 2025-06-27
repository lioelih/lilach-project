package il.cshaifasweng.OCSFMediatorExample.client;

import Events.CatalogEvent;
import Events.WarningEvent;
import il.cshaifasweng.Msg;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CatalogController {

    @FXML private Button homeButton;
    @FXML private Button refreshButton;
    @FXML private Button addProductButton;
    @FXML private Button filterButton;
    @FXML private TextField stringSearchField;
    @FXML private TextField minPrice;
    @FXML private TextField maxPrice;
    @FXML private ComboBox<String> typeBox;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> typeColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, byte[]> imageColumn;

    private List<Product> products;

    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);
        try {
            Msg msg = new Msg("GET_CATALOG",null);
            SimpleClient.getClient().sendToServer(msg);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        //set the min max value in filter to only accept integers
        minPrice.setTextFormatter(new TextFormatter<>(change -> {
            return change.getControlNewText().matches("\\d*") ? change : null;
        }));
        maxPrice.setTextFormatter(new TextFormatter<>(change -> {
            return change.getControlNewText().matches("\\d*") ? change : null;
        }));

        // Set up column bindings
        nameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        typeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));
        priceColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getPrice()));

        // Set up image column (image is stored as byte[])
        imageColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getImage()));
        imageColumn.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitHeight(60);
                imageView.setFitWidth(100);
                imageView.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(byte[] imageData, boolean empty) {
                super.updateItem(imageData, empty);
                if (empty || imageData == null || imageData.length == 0) {
                    setGraphic(null);
                }
                else {
                    try {
                        Image image = new Image(new ByteArrayInputStream(imageData));
                        imageView.setImage(image);
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(null);
                        e.printStackTrace();
                    }
                }
            }
        });

        addViewButtonColumn();

        // Buttons
        homeButton.setOnAction(e -> {
            EventBus.getDefault().unregister(this);
            SceneController.switchScene("home");
        });

        addProductButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("add_product_page.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = new Stage();
                stage.setTitle("Add Product");
                stage.setScene(scene);
                stage.show();
            } catch (IOException err) {
                err.printStackTrace();
            }
        });

        refreshButton.setOnAction(e -> {
            try {
                Msg msg = new Msg("GET_CATALOG", null);
                SimpleClient.getClient().sendToServer(msg);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        filterButton.setOnAction(e -> {
            List<Product> filteredProducts = new ArrayList<>(products);
            if(!Objects.equals(typeBox.getValue(), "All Types")) {
                filteredProducts.removeIf(product -> !Objects.equals(product.getType(), typeBox.getValue()));
            }
            if(!Objects.equals(minPrice.getText(), "")) {
                filteredProducts.removeIf(product -> Double.parseDouble(minPrice.getText()) > product.getPrice());
            }
            if(!Objects.equals(maxPrice.getText(), "")) {
                filteredProducts.removeIf(product -> Double.parseDouble(maxPrice.getText()) < product.getPrice());
            }
            if(!Objects.equals(stringSearchField.getText(),"")) {
                filteredProducts.removeIf(product -> !product.getName().toLowerCase().contains(stringSearchField.getText().toLowerCase()));
            }
            productTable.getItems().setAll(filteredProducts);
        });
    }

    private void addViewButtonColumn() {
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
    public void onCatalogReceived(CatalogEvent event) {
        products = event.getProducts();
        Platform.runLater(() -> {
            productTable.getItems().setAll(products);
            // set up filer section
            if (!typeBox.getItems().contains("All Types")) {
                typeBox.getItems().add("All Types");
            }
            typeBox.setValue("All Types");
            for (Product product : products) {
                if (!typeBox.getItems().contains(product.getType())) {
                    typeBox.getItems().add(product.getType());
                }
            }
            minPrice.setText("");
            maxPrice.setText("");
            stringSearchField.setText("");
        });
    }

    private void openProductPage(Product product) {
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

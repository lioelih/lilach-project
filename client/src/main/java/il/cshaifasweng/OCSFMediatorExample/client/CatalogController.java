package il.cshaifasweng.OCSFMediatorExample.client;

import Events.CatalogEvent;
import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Basket;
import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
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
    @FXML private Button basketIcon;
    @FXML private Label basketCountLabel;
    @FXML private TilePane productGrid;
    @FXML private ImageView logoImage;
    private List<Product> products;

    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);
        try {
            Msg msg = new Msg("GET_CATALOG", null);
            SimpleClient.getClient().sendToServer(msg);

            // Also fetch the user's current basket
            Msg fetchBasket = new Msg("FETCH_BASKET", SceneController.loggedUsername);
            SimpleClient.getClient().sendToServer(fetchBasket);
            System.out.println("Client instance: " + SimpleClient.getClient());
            System.out.println("Connected? " + (SimpleClient.getClient() != null && SimpleClient.getClient().isConnected()));

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }


        // Accept only integers for price filters
        minPrice.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null
        ));
        maxPrice.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null
        ));

        // Button handlers
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
            if (!Objects.equals(typeBox.getValue(), "All Types")) {
                filteredProducts.removeIf(product -> !Objects.equals(product.getType(), typeBox.getValue()));
            }
            if (!Objects.equals(minPrice.getText(), "")) {
                filteredProducts.removeIf(product -> Double.parseDouble(minPrice.getText()) > product.getPrice());
            }
            if (!Objects.equals(maxPrice.getText(), "")) {
                filteredProducts.removeIf(product -> Double.parseDouble(maxPrice.getText()) < product.getPrice());
            }
            if (!Objects.equals(stringSearchField.getText(), "")) {
                filteredProducts.removeIf(product -> !product.getName().toLowerCase().contains(stringSearchField.getText().toLowerCase()));
            }
            displayProducts(filteredProducts);
        });
        basketIcon.setOnAction(e -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("basket.fxml"));
            Scene scene = null;
            try {
                scene = new Scene(loader.load());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            Stage basketStage = new Stage();
            basketStage.setTitle("Your Basket");
            basketStage.setScene(scene);
            basketStage.show();
        });
        Image logo = new Image(getClass().getResourceAsStream("/image/logo.png"));
        logoImage.setImage(logo);
    }

    @Subscribe
    public void onCatalogReceived(CatalogEvent event) {
        products = event.getProducts();
        Platform.runLater(() -> {
            updateFilterBox();
            displayProducts(products);
            clearFilters();
        });
    }

    @Subscribe
    public void handleBasketMessages(Msg msg) {
        if (msg.getAction().equals("BASKET_FETCHED")) {
            List<Basket> items = (List<Basket>) msg.getData();
            int total = items.stream().mapToInt(Basket::getAmount).sum();
            Platform.runLater(() -> basketCountLabel.setText(total > 99 ? "99+" : String.valueOf(total)));
        } else if (msg.getAction().equals("BASKET_UPDATED")) {
            try {
                SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void displayProducts(List<Product> productList) {
        productGrid.getChildren().clear();
        for (Product product : productList) {
            VBox card = new VBox(5);
            card.setStyle("-fx-border-color: lightgray; -fx-border-radius: 10; -fx-padding: 10; -fx-background-color: white;");
            card.setPrefWidth(200);
            card.setPrefHeight(250);
            card.setAlignment(Pos.CENTER);

            // Image
            ImageView imageView = new ImageView();
            imageView.setFitWidth(180);
            imageView.setFitHeight(120);
            imageView.setPreserveRatio(true);
            if (product.getImage() != null && product.getImage().length > 0) {
                try {
                    imageView.setImage(new Image(new ByteArrayInputStream(product.getImage())));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Name
            Label nameLabel = new Label(product.getName());
            nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
            // Type
            Label typeLabel = new Label(product.getType());
            // Price
            Label priceLabel = new Label(String.format("₪%.2f", product.getPrice()));
            priceLabel.setStyle("-fx-text-fill: darkgreen; -fx-font-size: 12;");

            // View Button
            Button viewButton = new Button("View");
            viewButton.setOnAction(e -> openProductPage(product));

            // Add to Basket Button
            Button addToBasketButton = new Button("Add to Basket");
            try{
                SimpleClient.getClient().openConnection();
            }
            catch(IOException e){
                e.printStackTrace();
            }
            addToBasketButton.setOnAction(e -> {
                System.out.println("Preparing to send product: " + product.getName());
                if (SimpleClient.ensureConnected()) {
                    try {
                        Msg msg = new Msg("ADD_TO_BASKET", new Object[]{SceneController.loggedUsername, product});
                        SimpleClient.getClient().sendToServer(msg);
                        System.out.println("Sent ADD_TO_BASKET: " + product.getName());
                    } catch (IOException ex) {
                        System.err.println("Failed to send ADD_TO_BASKET");
                        ex.printStackTrace();
                    }
                } else {
                    System.err.println("Client is not connected and reconnection failed.");
                }
            });


            card.getChildren().addAll(imageView, nameLabel, typeLabel, priceLabel, viewButton, addToBasketButton);

            productGrid.getChildren().add(card);
        }
    }

    private void updateFilterBox() {
        if (!typeBox.getItems().contains("All Types")) {
            typeBox.getItems().add("All Types");
        }
        typeBox.setValue("All Types");

        for (Product product : products) {
            if (!typeBox.getItems().contains(product.getType())) {
                typeBox.getItems().add(product.getType());
            }
        }
    }

    private void clearFilters() {
        minPrice.setText("");
        maxPrice.setText("");
        stringSearchField.setText("");
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

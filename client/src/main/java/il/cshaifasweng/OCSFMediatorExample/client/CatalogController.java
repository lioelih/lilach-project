package il.cshaifasweng.OCSFMediatorExample.client;

import Events.CatalogEvent;
import Events.SalesEvent;
import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Basket;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import il.cshaifasweng.StockLineDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
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
    @FXML private ComboBox<Branch> branchFilter;
    private List<Product> products;
    private  List<Sale> sales;
    private List<Product> fullCatalog = new ArrayList<>();
    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);
        try {
            SimpleClient.ensureConnected();
            SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null));
            SimpleClient.getClient().sendToServer(new Msg("GET_SALES", null));
            SimpleClient.getClient().sendToServer(new Msg("GET_CATALOG", null));
            // Also fetch the user's current basket
            SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername));
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
                SimpleClient.getClient().sendToServer(new Msg("GET_CATALOG", null));
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
            ((BasketController)loader.getController()).setSales(sales);
            Stage basketStage = new Stage();
            basketStage.setTitle("Your Basket");
            basketStage.setScene(scene);
            basketStage.show();
        });
        Image logo = new Image(getClass().getResourceAsStream("/image/logo.png"));
        logoImage.setImage(logo);
        Image img = new Image(getClass().getResourceAsStream("/image/basket_icon.png"));
        ImageView iv = new ImageView(img);
        iv.setFitWidth(24);   // optional sizing
        iv.setPreserveRatio(true);
        basketIcon.setGraphic(iv);

        branchFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Branch b, boolean empty) {
                super.updateItem(b,empty);
                setText(empty || b==null ? "" : b.getName());
            }
        });
        branchFilter.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Branch b, boolean empty) {
                super.updateItem(b,empty);
                setText(empty || b==null ? "" : b.getName());
            }
        });

        filterButton.setOnAction(e -> {

            Branch sel = branchFilter.getValue();

            // -----------  “All Products”  -----------
            if (sel == null || sel.getBranchId() == 0) {
                products = new ArrayList<>(fullCatalog);   // full list you cached
                applyLocalFilters();                       // only local filters
                return;
            }

            // -----------  real branch  -----------
            try {
                SimpleClient.getClient()
                        .sendToServer(new Msg("STOCK_BY_BRANCH", sel.getBranchId()));
            } catch (IOException ex) { ex.printStackTrace(); }

            applyLocalFilters();
        });




    }

    @Subscribe
    public void onCatalogReceived(CatalogEvent event) {
        products = event.getProducts();
        fullCatalog = new ArrayList<>(products);
        Platform.runLater(() -> {
            updateFilterBox();
            displayProducts(products);
            clearFilters();
        });
    }

    @Subscribe
    public void onSalesReceived(SalesEvent event) {
        sales = event.getSales();
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
    @Subscribe
    public void handleBranches(Msg m) {
        if (!"BRANCHES_OK".equals(m.getAction())) return;

        List<Branch> list = new ArrayList<>((List<Branch>) m.getData());

        // build a fake “All Products” branch
        Branch all = new Branch();
        all.setName("All Products");
        all.setBranchId(0);           // never exists in DB
        list.add(0, all);

        Platform.runLater(() -> {
            branchFilter.getItems().setAll(list);
            branchFilter.getSelectionModel().selectFirst();   // default = “All Products”
        });
    }



    private void displayProducts(List<Product> productList) {
        productGrid.getChildren().clear();
        for (Product product : productList) {
            VBox card = new VBox(5);
            card.setStyle("-fx-border-color: lightgray; -fx-border-radius: 10; -fx-padding: 10; -fx-background-color: white;");
            card.setPrefWidth(200);
            card.setPrefHeight(250);
            card.setAlignment(Pos.CENTER);

            // Create StackPane to hold product image and optional sale badge
            StackPane imageStack = new StackPane();
            imageStack.setPrefSize(180, 120);

            // Product ImageView
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

            imageStack.getChildren().add(imageView);

            //check if product isOnSale
            boolean isOnSale = Sale.onSale(product,this.sales);


            // Price Label
            Text price = new Text(String.format("₪%.2f", product.getPrice()));
            price.setFill(Color.web("#2E8B57")); // SeaGreen color
            price.setStyle("-fx-font-size: 12;");
            TextFlow priceFlow = new TextFlow(price);
            priceFlow.setTextAlignment(TextAlignment.CENTER);

            if (isOnSale) {
                // Sale badge ImageView
                ImageView saleBadge = new ImageView(new Image(getClass().getResourceAsStream("/image/sale_icon.png")));
                saleBadge.setFitWidth(40);
                saleBadge.setFitHeight(40);

                // Align saleBadge to top-right corner
                StackPane.setAlignment(saleBadge, Pos.TOP_RIGHT);
                StackPane.setMargin(saleBadge, new Insets(5));

                imageStack.getChildren().add(saleBadge);

                //change the price according to the discount
                double finalPrice = Sale.getDiscountedPrice(product, sales);
                if(finalPrice != product.getPrice()) {
                    Text originalPrice = new Text(String.format("₪%.2f ", product.getPrice()));
                    originalPrice.setFill(Color.RED);
                    originalPrice.setStyle("-fx-font-size: 12; -fx-strikethrough: true;");

                    // Discounted price (green)
                    Text discountedPrice = new Text(String.format("₪%.2f", finalPrice));
                    discountedPrice.setFill(Color.web("#2E8B57")); // SeaGreen
                    discountedPrice.setStyle("-fx-font-size: 12;");

                    priceFlow.getChildren().clear();
                    priceFlow.getChildren().addAll(originalPrice, discountedPrice);

                }
            }

            // Name Label
            Label nameLabel = new Label(product.getName());
            nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

            // Type Label
            Label typeLabel = new Label(product.getType());


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
                    List<Sale> productSales = Sale.getProductSales(product, sales);
                    if(productSales != null) {
                        for (Sale sale : productSales) {
                            if (sale.getDiscountType() == Sale.DiscountType.BUNDLE) {
                                Product bundledProduct = Sale.getBundledProduct(product, products, sale);
                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                alert.setTitle("Bundle Offer");
                                alert.setHeaderText("Special Bundle Offer!");
                                alert.setContentText(sale.getDescription());

                                // Load product image
                                Image image = new Image(new ByteArrayInputStream(bundledProduct.getImage()));
                                ImageView alertImageView = new ImageView(image);
                                alertImageView.setFitWidth(100);
                                alertImageView.setFitHeight(100);
                                alertImageView.setPreserveRatio(true);

                                // Add image to dialog
                                alert.setGraphic(alertImageView);

                                // Add Yes/No buttons
                                ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                                ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
                                alert.getButtonTypes().setAll(yesButton, noButton);

                                // Show dialog and handle result
                                alert.showAndWait().ifPresent(response -> {
                                    if (response == yesButton) {
                                        try {
                                            Msg msg = new Msg("ADD_TO_BASKET", new Object[]{SceneController.loggedUsername, bundledProduct});
                                            SimpleClient.getClient().sendToServer(msg);
                                            System.out.println("Sent ADD_TO_BASKET: " + product.getName());
                                        } catch (IOException ex) {
                                            System.err.println("Failed to send ADD_TO_BASKET");
                                            ex.printStackTrace();
                                        }
                                    }
                                });
                            }
                            if (sale.getDiscountType() == Sale.DiscountType.BUY_X_GET_Y) {
                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                alert.setTitle("Bundle Offer");
                                alert.setHeaderText("Special Bundle Offer!");
                                alert.setContentText(sale.getDescription());

                                // Load product image
                                Image image = new Image(new ByteArrayInputStream(product.getImage()));
                                ImageView alertImageView = new ImageView(image);
                                alertImageView.setFitWidth(100);
                                alertImageView.setFitHeight(100);
                                alertImageView.setPreserveRatio(true);

                                // Add image to dialog
                                alert.setGraphic(alertImageView);

                                // Add Yes/No buttons
                                ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                                ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
                                alert.getButtonTypes().setAll(yesButton, noButton);

                                // Show dialog and handle result
                                alert.showAndWait().ifPresent(response -> {
                                    if (response == yesButton) {
                                        try {
                                            Msg msg = new Msg("ADD_TO_BASKET", new Object[]{SceneController.loggedUsername, product});
                                            SimpleClient.getClient().sendToServer(msg);
                                            System.out.println("Sent ADD_TO_BASKET: " + product.getName());
                                        } catch (IOException ex) {
                                            System.err.println("Failed to send ADD_TO_BASKET");
                                            ex.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }
                    }
                } else {
                    System.err.println("Client is not connected and reconnection failed.");
                }
            });


            // Add everything to the card VBox
            card.getChildren().addAll(imageStack, nameLabel, typeLabel, priceFlow, viewButton, addToBasketButton);
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

    private void applyLocalFilters() {
        List<Product> base = new ArrayList<>(products);   // ‘products’ = whole catalog

        if (!"All Types".equals(typeBox.getValue()))
            base.removeIf(p -> !p.getType().equals(typeBox.getValue()));

        if (!minPrice.getText().isBlank())
            base.removeIf(p -> p.getPrice() < Double.parseDouble(minPrice.getText()));

        if (!maxPrice.getText().isBlank())
            base.removeIf(p -> p.getPrice() > Double.parseDouble(maxPrice.getText()));

        if (!stringSearchField.getText().isBlank())
            base.removeIf(p -> !p.getName().toLowerCase()
                    .contains(stringSearchField.getText().toLowerCase()));

        displayProducts(base);
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

    @Subscribe
    public void handleStock(Msg m) {
        if (!"STOCK_OK".equals(m.getAction())) return;

        List<StockLineDTO> rows = (List<StockLineDTO>) m.getData();

        // keep only product-ids that are in this stock list
        List<Integer> allowedIds = rows.stream()
                .map(StockLineDTO::product_id)
                .distinct()
                .toList();

        List<Product> allowed = products.stream()
                .filter(p -> allowedIds.contains(p.getId()))
                .toList();

        // after server filter, apply local UI filters too
        Platform.runLater(() -> {
            products = new ArrayList<>(allowed);  // limit ‘products’ pool temporarily
            applyLocalFilters();
        });
    }


    @FXML private void filterButtonFire() {    // called by both Filter-btn and combo
        filterButton.fire();
    }

    @FXML
    private void onClose() {
        EventBus.getDefault().unregister(this);
    }
}

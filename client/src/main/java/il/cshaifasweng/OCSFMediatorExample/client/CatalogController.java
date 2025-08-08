package il.cshaifasweng.OCSFMediatorExample.client;

import Events.CatalogEvent;
import Events.SalesEvent;
import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
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

    @FXML private Label promoLabel;
    @FXML private Button homeButton;
    @FXML private Button refreshButton;
    @FXML private Button addProductButton;
    @FXML private Button viewSalesButton;
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
    @FXML private Button addCustomBtn;

    private List<Product> products;
    private List<Sale> sales;
    private List<Product> fullCatalog = new ArrayList<>();

    private boolean isVip;
    private Branch userBranch;

    private List<Integer> pendingAllowedIds;

    @FXML
    public void initialize() {
        try {
            EventBus.getDefault().unregister(this);
        } catch (Exception ignored) {}
        EventBus.getDefault().register(this);

        try {
            SimpleClient.ensureConnected();
            SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null));
            SimpleClient.getClient().sendToServer(new Msg("GET_SALES", null));
            SimpleClient.getClient().sendToServer(new Msg("GET_CATALOG", null));
            if (SceneController.loggedUsername != null) {
                SimpleClient.getClient().sendToServer(new Msg("FETCH_USER", SceneController.loggedUsername));
                SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        minPrice.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d*") ? c : null));
        maxPrice.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d*") ? c : null));

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
        addSaleButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("add_sale_page.fxml"));
                Scene scene = new Scene(loader.load());
                AddSaleController controller = loader.getController();
                controller.setProducts(fullCatalog);
                controller.setSales(sales);
                Stage stage = new Stage();
                stage.setTitle("Add Sale");
                stage.setScene(scene);
                stage.show();
            } catch (IOException err) {
                err.printStackTrace();
            }
        });

        addCustomBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("custom_bouquet.fxml"));
                Scene scene = new Scene(loader.load());
                Stage popup = new Stage();
                popup.setTitle("Create Custom Bouquet");
                popup.initModality(Modality.APPLICATION_MODAL);
                popup.setScene(scene);
                popup.setWidth(1200);
                popup.setHeight(800);
                popup.centerOnScreen();
                popup.setMaximized(true);
                popup.showAndWait();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        viewSalesButton.setOnAction(e -> {
            EventBus.getDefault().unregister(this);
            SceneController.switchScene("view_sales");
        });
        refreshButton.setOnAction(e -> initialize());

        basketIcon.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("basket.fxml"));
                Scene scene = new Scene(loader.load());
                BasketController controller = loader.getController();
                controller.setSales(sales);
                Stage basketStage = new Stage();
                basketStage.setTitle("Your Basket");
                basketStage.setScene(scene);
                basketStage.show();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        Image logo = new Image(getClass().getResourceAsStream("/image/logo.png"));
        logoImage.setImage(logo);
        Image img = new Image(getClass().getResourceAsStream("/image/basket_icon.png"));
        ImageView iv = new ImageView(img);
        iv.setFitWidth(24);
        iv.setPreserveRatio(true);
        basketIcon.setGraphic(iv);

        branchFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Branch b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : b.getName());
            }
        });

        branchFilter.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Branch b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : b.getName());
            }
        });

        branchFilter.setOnAction(e -> {
            Branch sel = branchFilter.getValue();
            if (sel == null || sel.getBranchId() == 0) {
                products = new ArrayList<>(fullCatalog);
                applyLocalFilters();
            } else {
                try {
                    SimpleClient.getClient().sendToServer(new Msg("STOCK_BY_BRANCH", sel.getBranchId()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        filterButton.setOnAction(e -> {
            Branch sel = branchFilter.getValue();
            if (sel == null || sel.getBranchId() == 0) {
                products = new ArrayList<>(fullCatalog);
                applyLocalFilters();
                return;
            }
            try {
                SimpleClient.getClient().sendToServer(new Msg("STOCK_BY_BRANCH", sel.getBranchId()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        boolean canWorker = SceneController.hasPermission(SceneController.Role.WORKER);
        addProductButton.setVisible(canWorker);
        addProductButton.setManaged(canWorker);
        addSaleButton.setVisible(canWorker);
        addSaleButton.setManaged(canWorker);
        viewSalesButton.setVisible(canWorker);
        viewSalesButton.setManaged(canWorker);
    }

    @Subscribe
    public void onCatalogReceived(CatalogEvent event) {
        products = event.getProducts();
        fullCatalog = new ArrayList<>(products);

        boolean privileged = isVip || SceneController.hasPermission(SceneController.Role.WORKER);
        if (!privileged && pendingAllowedIds != null) {
            products = fullCatalog.stream()
                    .filter(p -> pendingAllowedIds.contains(p.getId()))
                    .toList();
            pendingAllowedIds = null;
        }

        Platform.runLater(() -> {
            updateFilterBox();
            displayProducts(products);
            clearFilters();
        });
    }

    @Subscribe
    public void onSalesReceived(SalesEvent event) {
        sales = event.getSales();
        if ("SALE_ADDED".equals(event.getUseCase())) displayProducts(fullCatalog);
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
    public void onUserFetched(Msg m) {
        if (!"FETCH_USER".equals(m.getAction())) return;
        User me = (User) m.getData();
        if (me == null) return;

        isVip = me.isVIP();
        userBranch = new Branch();
        userBranch.setBranchId(me.getBranch().getBranchId());
        userBranch.setName(me.getBranch().getBranchName());

        boolean privileged = isVip || SceneController.hasPermission(SceneController.Role.WORKER);

        Platform.runLater(() -> {
            if (privileged) {
                promoLabel.setVisible(false);
                branchFilter.setDisable(false);
                try {
                    SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null));
                } catch (IOException ignore) {}
                displayProducts(fullCatalog);
            } else {
                promoLabel.setVisible(true);
                branchFilter.getItems().setAll(userBranch);
                branchFilter.getSelectionModel().selectFirst();
                branchFilter.setDisable(true);
                try {
                    SimpleClient.getClient().sendToServer(new Msg("STOCK_BY_BRANCH", userBranch.getBranchId()));
                } catch (IOException ignore) {}
            }
            try {
                SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername));
            } catch (IOException ignore) {}
        });
    }

    @Subscribe
    public void handleBranches(Msg m) {
        if (!"BRANCHES_OK".equals(m.getAction())) return;
        List<Branch> fromServer = (List<Branch>) m.getData();

        List<Branch> safe = new ArrayList<>();
        for (Branch b : fromServer) {
            Branch copy = new Branch();
            copy.setBranchId(b.getBranchId());
            copy.setName(b.getBranchName());
            safe.add(copy);
        }

        Branch all = new Branch();
        all.setBranchId(0);
        all.setName("All Products");
        safe.add(0, all);

        Platform.runLater(() -> {
            if (!branchFilter.isDisabled()) {
                branchFilter.getItems().setAll(safe);
                branchFilter.getSelectionModel().selectFirst();
            }
        });
    }

    @Subscribe
    public void handleStock(Msg m) {
        if (!"STOCK_OK".equals(m.getAction())) return;

        List<StockLineDTO> rows = (List<StockLineDTO>) m.getData();
        List<Integer> allowedIds = rows.stream().map(StockLineDTO::product_id).distinct().toList();

        if (fullCatalog == null || fullCatalog.isEmpty()) {
            pendingAllowedIds = allowedIds;
            return;
        }

        products = fullCatalog.stream()
                .filter(p -> allowedIds.contains(p.getId()))
                .toList();

        Platform.runLater(() -> {
            updateFilterBox();
            applyLocalFilters();
        });

    }

    private void displayProducts(List<Product> productList) {
        productGrid.getChildren().clear();
        for (Product product : productList) {
            VBox card = new VBox(5);
            card.setStyle("-fx-border-color: lightgray; -fx-border-radius: 10; -fx-padding: 10; -fx-background-color: white;");
            card.setPrefWidth(225);
            card.setPrefHeight(275);
            card.setAlignment(Pos.CENTER);

            StackPane imageStack = new StackPane();
            imageStack.setPrefSize(180, 120);

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

            boolean isOnSale = Sale.onSale(product, this.sales);

            Text price = new Text(String.format("₪%.2f", product.getPrice()));
            price.setFill(Color.web("#2E8B57"));
            price.setStyle("-fx-font-size: 12;");
            TextFlow priceFlow = new TextFlow(price);
            priceFlow.setTextAlignment(TextAlignment.CENTER);

            if (isOnSale) {
                ImageView saleBadge = new ImageView(new Image(getClass().getResourceAsStream("/image/sale_icon.png")));
                saleBadge.setFitWidth(40);
                saleBadge.setFitHeight(40);
                StackPane.setAlignment(saleBadge, Pos.TOP_RIGHT);
                StackPane.setMargin(saleBadge, new Insets(5));
                imageStack.getChildren().add(saleBadge);

                double finalPrice = Sale.getDiscountedPrice(product, sales);
                if (finalPrice != product.getPrice()) {
                    Text originalPrice = new Text(String.format("₪%.2f ", product.getPrice()));
                    originalPrice.setFill(Color.RED);
                    originalPrice.setStyle("-fx-font-size: 12; -fx-strikethrough: true;");
                    Text discountedPrice = new Text(String.format("₪%.2f", finalPrice));
                    discountedPrice.setFill(Color.web("#2E8B57"));
                    discountedPrice.setStyle("-fx-font-size: 12;");
                    priceFlow.getChildren().clear();
                    priceFlow.getChildren().addAll(originalPrice, discountedPrice);
                }
            }

            Label nameLabel = new Label(product.getName());
            nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

            Label typeLabel = new Label(product.getType());

            Button viewButton = new Button("View");
            viewButton.setOnAction(e -> openProductPage(product));

            Button addToBasketButton = new Button("Add to Basket");
            addToBasketButton.setOnAction(e -> {
                if (SimpleClient.ensureConnected()) {
                    try {
                        Msg msg = new Msg("ADD_TO_BASKET", new Object[]{SceneController.loggedUsername, product});
                        SimpleClient.getClient().sendToServer(msg);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    List<Sale> productSales = Sale.getProductSales(product, sales);
                    if (productSales != null) {
                        for (Sale sale : productSales) {
                            if (sale.getDiscountType() == Sale.DiscountType.BUNDLE) {
                                Product bundledProduct = Sale.getBundledProduct(product, products, sale);
                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                alert.setTitle("Bundle Offer");
                                alert.setHeaderText("Special Bundle Offer!");
                                alert.setContentText(sale.getDescription());
                                Image image = new Image(new ByteArrayInputStream(bundledProduct.getImage()));
                                ImageView alertImageView = new ImageView(image);
                                alertImageView.setFitWidth(100);
                                alertImageView.setFitHeight(100);
                                alertImageView.setPreserveRatio(true);
                                alert.setGraphic(alertImageView);
                                ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                                ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
                                alert.getButtonTypes().setAll(yesButton, noButton);
                                alert.showAndWait().ifPresent(response -> {
                                    if (response == yesButton) {
                                        try {
                                            Msg msg2 = new Msg("ADD_TO_BASKET", new Object[]{SceneController.loggedUsername, bundledProduct});
                                            SimpleClient.getClient().sendToServer(msg2);
                                        } catch (IOException ex) {
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
                                Image image = new Image(new ByteArrayInputStream(product.getImage()));
                                ImageView alertImageView = new ImageView(image);
                                alertImageView.setFitWidth(100);
                                alertImageView.setFitHeight(100);
                                alertImageView.setPreserveRatio(true);
                                alert.setGraphic(alertImageView);
                                ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                                ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
                                alert.getButtonTypes().setAll(yesButton, noButton);
                                alert.showAndWait().ifPresent(response -> {
                                    if (response == yesButton) {
                                        try {
                                            Msg msg2 = new Msg("ADD_TO_BASKET_X_AMOUNT", new Object[]{SceneController.loggedUsername, product, sale.getBuyQuantity() + sale.getGetQuantity() - 1});
                                            SimpleClient.getClient().sendToServer(msg2);
                                        } catch (IOException ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            });

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
        List<Product> base = new ArrayList<>(products);
        if (!"All Types".equals(typeBox.getValue()))
            base.removeIf(p -> !p.getType().equals(typeBox.getValue()));
        if (!minPrice.getText().isBlank())
            base.removeIf(p -> p.getPrice() < Double.parseDouble(minPrice.getText()));
        if (!maxPrice.getText().isBlank())
            base.removeIf(p -> p.getPrice() > Double.parseDouble(maxPrice.getText()));
        if (!stringSearchField.getText().isBlank())
            base.removeIf(p -> !p.getName().toLowerCase().contains(stringSearchField.getText().toLowerCase()));
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
    public void onUserUpdated(Msg m) {
        if (!"USER_UPDATED".equals(m.getAction())) return;
        User updated = (User) m.getData();
        if (updated.getUsername().equals(SceneController.loggedUsername)) {
            try {
                SimpleClient.getClient().sendToServer(new Msg("FETCH_USER", updated.getUsername()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML private void filterButtonFire() {
        filterButton.fire();
    }

    @FXML
    private void onClose() {
        EventBus.getDefault().unregister(this);
    }
}

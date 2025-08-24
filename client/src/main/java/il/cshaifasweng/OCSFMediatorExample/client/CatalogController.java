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
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    private List<Product> products = new ArrayList<>();
    private List<Sale> sales;
    private List<Product> fullCatalog = new ArrayList<>();

    private boolean isVip;
    private Branch userBranch;
    private boolean actionsDisabled = false;

    private Integer lastStockBranchId = null;
    private List<Integer> lastAllowedIds = null;
    private boolean applyWaitingForStock = false;

    @FXML
    public void initialize() {
        try { EventBus.getDefault().unregister(this); } catch (Exception ignored) {}
        EventBus.getDefault().register(this);
        boolean loggedIn = SceneController.loggedUsername != null;
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
                showAlert(err.getMessage());
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
                showAlert(ex.getMessage());
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

        logoImage.setImage(new Image(
                Objects.requireNonNull(getClass().getResource("/image/logo.png"), "Logo image not found")
                        .toExternalForm()
        ));
        ImageView iv = new ImageView(new Image(
                Objects.requireNonNull(getClass().getResource("/image/basket_icon.png"), "Basket icon not found")
                        .toExternalForm()
        ));
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

        // prevent auto-apply from FXML bindings; only the button applies
        branchFilter.setOnAction(e -> {});
        typeBox.setOnAction(e -> {});
        stringSearchField.setOnAction(e -> {});
        minPrice.setOnAction(e -> {});
        maxPrice.setOnAction(e -> {});

        filterButton.setOnAction(e -> apply());

        boolean canWorker = SceneController.hasPermission(User.Role.WORKER);
        addProductButton.setVisible(canWorker);
        addProductButton.setManaged(canWorker);
        viewSalesButton.setVisible(canWorker);
        viewSalesButton.setManaged(canWorker);
        basketIcon.setVisible(loggedIn);
        basketIcon.setManaged(loggedIn);
        basketCountLabel.setVisible(loggedIn);
        basketCountLabel.setManaged(loggedIn);
        addCustomBtn.setVisible(loggedIn);
        addCustomBtn.setManaged(loggedIn);
    }

    @Subscribe
    public void onCatalogReceived(CatalogEvent event) {
        fullCatalog = new ArrayList<>(event.getProducts());

        Platform.runLater(() -> {
            if (applyWaitingForStock) return;

            Branch sel = branchFilter.getValue();

            if (sel != null && sel.getBranchId() != 0
                    && lastAllowedIds != null
                    && Objects.equals(lastStockBranchId, sel.getBranchId())) {
                List<Product> base = fullCatalog.stream()
                        .filter(p -> lastAllowedIds.contains(p.getId()))
                        .toList();
                doLocal(base);
            } else {
                refreshTypeChoices();
                products = new ArrayList<>(fullCatalog);
                displayProducts(products);
            }

        });
    }

    @Subscribe
    public void onSalesReceived(SalesEvent event) {
        sales = event.getSales();
        Platform.runLater(() -> maybeDisplayProducts(fullCatalog));
    }

    //helper func to help avoid sales and catalog event collisions
    private void maybeDisplayProducts(List<Product> productList) {
        if (sales != null && !productList.isEmpty()) {
            displayProducts(productList);
        }
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
                showAlert(e.getMessage());
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

        boolean privileged = isVip || SceneController.hasPermission(User.Role.WORKER);

        Platform.runLater(() -> {
            if (privileged) {
                promoLabel.setVisible(false);
                branchFilter.setDisable(false);
                try { SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null)); } catch (IOException ignore) {}
                displayProducts(fullCatalog);
            } else {
                promoLabel.setVisible(true);

                Branch all = new Branch();
                all.setBranchId(0);
                all.setName("All Products");

                branchFilter.setDisable(false);
                branchFilter.getItems().setAll(all, userBranch);
                branchFilter.getSelectionModel().select(userBranch);
                actionsDisabled = false;
                apply();
            }

            try { SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername)); } catch (IOException ignore) {}
        });
    }

    @Subscribe
    public void handleBranches(Msg m) {
        if (!"BRANCHES_OK".equals(m.getAction())) return;
        List<Branch> fromServer = (List<Branch>) m.getData();

        List<Branch> safe = fromServer.stream().map(b -> {
            Branch copy = new Branch();
            copy.setBranchId(b.getBranchId());
            copy.setName(b.getBranchName());
            return copy;
        }).collect(Collectors.toCollection(ArrayList::new));

        Branch all = new Branch();
        all.setBranchId(0);
        all.setName("All Products");
        safe.add(0, all);

        Platform.runLater(() -> {
            boolean privileged = isVip || SceneController.hasPermission(User.Role.WORKER);
            if (privileged) {
                branchFilter.getItems().setAll(safe);
                branchFilter.getSelectionModel().selectFirst();
            }
        });
    }

    @Subscribe
    public void handleStock(Msg m) {
        if (!"STOCK_OK".equals(m.getAction())) return;

        List<StockLineDTO> rows = (List<StockLineDTO>) m.getData();
        lastAllowedIds = rows.stream().map(StockLineDTO::product_id).distinct().toList();

        Branch sel = branchFilter.getValue();
        lastStockBranchId = (sel != null) ? sel.getBranchId() : null;

        if (applyWaitingForStock) {
            applyWaitingForStock = false;
            Platform.runLater(this::apply);
        }
    }

    private void apply() {
        Branch sel = branchFilter.getValue();
        boolean privileged = isVip || SceneController.hasPermission(User.Role.WORKER);

        if (sel == null || sel.getBranchId() == 0) {
            actionsDisabled = !privileged;
            doLocal(new ArrayList<>(fullCatalog));
            return;
        }

        actionsDisabled = false;

        if (Objects.equals(lastStockBranchId, sel.getBranchId()) && lastAllowedIds != null) {
            List<Product> base = fullCatalog.stream()
                    .filter(p -> lastAllowedIds.contains(p.getId()))
                    .toList();
            doLocal(base);
        } else {
            applyWaitingForStock = true;
            try {
                SimpleClient.getClient().sendToServer(new Msg("STOCK_BY_BRANCH", sel.getBranchId()));
            } catch (IOException ex) {
                showAlert(ex.getMessage());
            }
        }
    }

    private void doLocal(List<Product> base) {
        refreshTypeChoices();

        List<Product> out = new ArrayList<>(base);
        String selectedType = typeBox.getValue();

        if (selectedType != null && !"All Types".equals(selectedType))
            out.removeIf(p -> !p.getType().equals(selectedType));
        if (!minPrice.getText().isBlank())
            out.removeIf(p -> p.getPrice() < Double.parseDouble(minPrice.getText()));
        if (!maxPrice.getText().isBlank())
            out.removeIf(p -> p.getPrice() > Double.parseDouble(maxPrice.getText()));
        if (!stringSearchField.getText().isBlank())
            out.removeIf(p -> !p.getName().toLowerCase().contains(stringSearchField.getText().toLowerCase()));

        products = out;
        displayProducts(out);
    }

    private void refreshTypeChoices() {
        String prevType = typeBox.getValue();

        Set<String> types = fullCatalog.stream()
                .map(Product::getType)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));

        java.util.List<String> items = new java.util.ArrayList<>();
        items.add("All Types");
        items.addAll(types);

        typeBox.getItems().setAll(items);

        if (prevType != null && items.contains(prevType)) {
            typeBox.setValue(prevType);
        } else if (typeBox.getValue() == null || !items.contains(typeBox.getValue())) {
            typeBox.setValue("All Types");
        }
    }


    private void openProductPage(Product product) {
        if (!canOpenProductDetails()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Restricted");
            a.setHeaderText(null);
            a.setContentText("Only workers and managers can view product details.");
            a.showAndWait();
            return;
        }
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
            showAlert(e.getMessage());
        }
    }

    @Subscribe
    public void onUserUpdated(Msg msg) {
        if (!"USER_UPDATED".equals(msg.getAction())) return;
        if (SceneController.loggedUsername == null) return;
        try {
            SimpleClient.getClient().sendToServer(new Msg("FETCH_USER", SceneController.loggedUsername));
        } catch (IOException e) {
            showAlert(e.getMessage());
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLocalRoleVipChanged(Msg msg) {
        if (!"LOCAL_ROLE_VIP_CHANGED".equals(msg.getAction())) return;

        boolean wasVip = isVip;
        boolean wasWorkerPlus = SceneController.hasPermission(User.Role.WORKER);
        isVip = SceneController.isVIP;
        boolean nowWorkerPlus = SceneController.hasPermission(User.Role.WORKER);

        addProductButton.setVisible(nowWorkerPlus);
        addProductButton.setManaged(nowWorkerPlus);
        viewSalesButton.setVisible(nowWorkerPlus);
        viewSalesButton.setManaged(nowWorkerPlus);

        if (wasVip != isVip || wasWorkerPlus != nowWorkerPlus) {
            try { SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null)); } catch (IOException ignore) {}
        }
        boolean loggedIn = SceneController.loggedUsername != null;
        basketIcon.setVisible(loggedIn);
        basketIcon.setManaged(loggedIn);
        basketCountLabel.setVisible(loggedIn);
        basketCountLabel.setManaged(loggedIn);
        addCustomBtn.setVisible(loggedIn);
        addCustomBtn.setManaged(loggedIn);
        addCustomBtn.setVisible(loggedIn);
        addCustomBtn.setManaged(loggedIn);

        apply();
        if (loggedIn) {
            try { SimpleClient.getClient().sendToServer(new Msg("FETCH_BASKET", SceneController.loggedUsername)); } catch (IOException ignore) {}
        }
    }



    @FXML private void filterButtonFire() { filterButton.fire(); }

    private static boolean canOpenProductDetails() {
        return SceneController.hasPermission(User.Role.WORKER)
                || SceneController.hasPermission(User.Role.MANAGER)
                || SceneController.hasPermission(User.Role.ADMIN);
    }

    private void displayProducts(List<Product> productList) {
        productGrid.getChildren().clear();
        boolean loggedIn = SceneController.loggedUsername != null;
        if (productList == null || productList.isEmpty()) {
            Label empty = new Label("No items have been found.");
            empty.setStyle("-fx-font-size: 16; -fx-text-fill: #6b7280;");
            empty.setWrapText(true);
            empty.setAlignment(Pos.CENTER);

            // simple centered-ish placeholder inside the grid
            StackPane wrap = new StackPane(empty);
            wrap.setPrefSize(600, 300); // gives it some presence in the grid
            StackPane.setAlignment(empty, Pos.CENTER);

            productGrid.getChildren().add(wrap);
            return;
        }

        for (Product product : productList) {
            VBox card = new VBox(8);
            card.setStyle("-fx-border-color: lightgray; -fx-border-radius: 10; -fx-padding: 12; -fx-background-color: white;");
            card.setPrefWidth(225);
            card.setPrefHeight(320);
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
                } catch (Exception ignored) {}
            }
            imageStack.getChildren().add(imageView);

            boolean isOnSale = Sale.onSale(product, this.sales);

            Text price = new Text(String.format("₪%.2f", product.getPrice()));
            price.setFill(Color.web("#2E8B57"));
            price.setStyle("-fx-font-size: 12;");
            TextFlow priceFlow = new TextFlow(price);
            priceFlow.setTextAlignment(TextAlignment.CENTER);

            if (isOnSale) {
                ImageView saleBadge = new ImageView(new Image(
                        Objects.requireNonNull(getClass().getResource("/image/sale_icon.png"), "sale_icon.png not found")
                                .toExternalForm()
                ));
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
                    priceFlow.getChildren().setAll(originalPrice, discountedPrice);
                }
            }

            Label nameLabel = new Label(Objects.toString(product.getName(), ""));
            nameLabel.setWrapText(true);
            nameLabel.setMaxWidth(180);
            nameLabel.setAlignment(Pos.CENTER);
            nameLabel.setTextAlignment(TextAlignment.CENTER);
            nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

            Label typeLabel = new Label(Objects.toString(product.getType(), ""));
            typeLabel.setWrapText(true);
            typeLabel.setMaxWidth(180);
            typeLabel.setAlignment(Pos.CENTER);
            typeLabel.setTextAlignment(TextAlignment.CENTER);
            typeLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");

            Button viewButton = new Button("View");
            viewButton.setOnAction(e -> openProductPage(product));

            Button addToBasketButton = new Button("Add to Basket");
            addToBasketButton.setOnAction(e -> {
                if (SimpleClient.ensureConnected()) {
                    try {
                        Msg msg = new Msg("ADD_TO_BASKET", new Object[]{SceneController.loggedUsername, product});
                        SimpleClient.getClient().sendToServer(msg);
                    } catch (IOException ex) {
                        showAlert(ex.getMessage());
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
                                assert bundledProduct != null;
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
                                        } catch (IOException ignored) {}
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
                                        } catch (IOException ignored) {}
                                    }
                                });
                            }
                        }
                    }
                }
            });
            if (!loggedIn) {
                // Guests: button is not visible at all
                addToBasketButton.setVisible(false);
                addToBasketButton.setManaged(false);
            } else {
                // Logged-in: keep your existing enable/disable behavior
                addToBasketButton.setDisable(actionsDisabled);
            }
            boolean canDetails = canOpenProductDetails();
            if (!canDetails) {
                viewButton.setVisible(false);
                viewButton.setManaged(false);
            } else {
                viewButton.setDisable(actionsDisabled);
            }
            addToBasketButton.setDisable(actionsDisabled);

            card.getChildren().addAll(imageStack, nameLabel, typeLabel, priceFlow, viewButton, addToBasketButton);
            productGrid.getChildren().add(card);
        }
    }

    private void showAlert(String txt) {
        Alert alert = new Alert(Alert.AlertType.WARNING, txt, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}

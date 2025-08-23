package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AddSaleController {

    @FXML private TextField nameField;
    @FXML private TextField descriptionField;
    @FXML private ComboBox<String> discountTypeBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private VBox dynamicContainer;
    @FXML private Button cancelButton;
    @FXML private Button addButton;

    private List<Product> products;
    private List<Sale> sales;

    private ComboBox<Product> productSelector1;
    private ComboBox<Product> productSelector2;
    private TextField valueField;
    private TextField buyXField = null;
    private TextField getYField = null;
    private ImageView productImage1;
    private ImageView productImage2;

    @FXML
    public void initialize() {
        discountTypeBox.setItems(FXCollections.observableArrayList(
                "Percentage", "Fixed", "Bundle", "Buy_X_Get_Y"
        ));

        discountTypeBox.setOnAction(e -> handleDiscountTypeChange());

        // Date validation: disable past dates
        LocalDate today = LocalDate.now();
        startDatePicker.setDayCellFactory(picker -> new DateCell() {
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(today));
            }
        });
        endDatePicker.setDayCellFactory(picker -> new DateCell() {
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(today));
            }
        });

        cancelButton.setOnAction(e -> ((Stage) cancelButton.getScene().getWindow()).close());

        addButton.setOnAction(e -> {
            Sale newSale = buildSaleFromInputs();
            if (newSale == null) return; // build shows alerts

            if (saleAlreadyExists(newSale)) {
                showAlert(Alert.AlertType.WARNING, "Overlap", "There already exists a sale with overlapping dates for the same products and type.");
                return;
            }

            try {
                Msg message = new Msg("ADD_SALE", newSale);
                SimpleClient.getClient().sendToServer(message);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Sale added!");
                ((Stage) addButton.getScene().getWindow()).close();
            } catch (IOException err) {
                showAlert(Alert.AlertType.ERROR, "Network Error", err.getMessage());
            }
        });
    }

    private void handleDiscountTypeChange() {
        dynamicContainer.getChildren().clear();

        String type = discountTypeBox.getValue();
        if (type == null) return;

        switch (type) {
            case "Fixed", "Percentage" -> showSingleProductDiscount(type);
            case "Buy_X_Get_Y" -> showBuyXGetY();
            case "Bundle" -> showBundleDiscount();
        }
    }

    private void showSingleProductDiscount(String type) {
        VBox wrapper = new VBox(10);
        wrapper.setAlignment(Pos.CENTER);

        productSelector1 = createProductSelector();
        productSelector1.setOnAction(e -> updateImage(productSelector1, productImage1));

        valueField = new TextField();
        valueField.setPromptText("Enter discount value");

        if (type.equals("Percentage")) {
            valueField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) return;
                String raw = newVal.replace("%", "");
                if (raw.isEmpty()) {
                    valueField.setText("");
                } else if (!newVal.endsWith("%")) {
                    valueField.setText(raw + "%");
                }
            });
        }

        productImage1 = createImageView();

        wrapper.getChildren().addAll(productSelector1, valueField, productImage1);
        dynamicContainer.getChildren().add(wrapper);
    }

    private void showBuyXGetY() {
        VBox wrapper = new VBox(10);
        wrapper.setAlignment(Pos.CENTER);

        productSelector1 = createProductSelector();
        productSelector1.setOnAction(e -> updateImage(productSelector1, productImage1));

        HBox quantityBox = new HBox(10);
        quantityBox.setAlignment(Pos.CENTER);
        buyXField = new TextField();
        buyXField.setPromptText("Buy X");
        getYField = new TextField();
        getYField.setPromptText("Get Y");
        quantityBox.getChildren().addAll(buyXField, getYField);

        productImage1 = createImageView();

        wrapper.getChildren().addAll(productSelector1, quantityBox, productImage1);
        dynamicContainer.getChildren().add(wrapper);
    }

    private void showBundleDiscount() {
        VBox wrapper = new VBox(15);
        wrapper.setAlignment(Pos.CENTER_LEFT);

        HBox selectorsBox = new HBox(20);
        selectorsBox.setAlignment(Pos.CENTER);

        productSelector1 = createProductSelector();
        productSelector2 = createProductSelector();

        productSelector1.setOnAction(e -> updateImage(productSelector1, productImage1));
        productSelector2.setOnAction(e -> updateImage(productSelector2, productImage2));

        selectorsBox.getChildren().addAll(productSelector1, productSelector2);

        valueField = new TextField();
        valueField.setPromptText("Enter bundle discount value");

        HBox imagesBox = new HBox(20);
        imagesBox.setAlignment(Pos.CENTER);
        productImage1 = createImageView();
        productImage2 = createImageView();
        imagesBox.getChildren().addAll(productImage1, productImage2);

        wrapper.getChildren().addAll(selectorsBox, valueField, imagesBox);
        dynamicContainer.getChildren().add(wrapper);
    }

    private ComboBox<Product> createProductSelector() {
        ComboBox<Product> selector = new ComboBox<>();
        selector.setPromptText("Select a product");
        selector.setItems(FXCollections.observableArrayList(products));

        selector.setCellFactory(param -> new ListCell<>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitHeight(30);
                imageView.setFitWidth(30);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    imageView.setImage(product.getImage() != null
                            ? new Image(new java.io.ByteArrayInputStream(product.getImage()))
                            : null);
                    setGraphic(imageView);
                    setText(product.getName() + " - " + product.getPrice() + " NIS");
                }
            }
        });

        selector.setButtonCell(selector.getCellFactory().call(null));
        return selector;
    }

    private ImageView createImageView() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);
        VBox.setMargin(imageView, new Insets(5));
        return imageView;
    }

    private void updateImage(ComboBox<Product> selector, ImageView imageView) {
        Product selected = selector.getValue();
        if (selected != null && selected.getImage() != null) {
            try {
                Image image = new Image(new java.io.ByteArrayInputStream(selected.getImage()));
                imageView.setImage(image);
            } catch (Exception e) {
                imageView.setImage(null);
                System.err.println("Failed to load product image: " + e.getMessage());
            }
        } else {
            imageView.setImage(null);
        }
    }

    private Sale buildSaleFromInputs() {
        if (!validInput()) return null;

        String name = nameField.getText().trim();
        String description = descriptionField.getText().trim();
        String discountTypeStr = discountTypeBox.getValue().toUpperCase();
        Sale.DiscountType discountType = Sale.DiscountType.valueOf(discountTypeStr);

        // Dates
        LocalDateTime startDate = startDatePicker.getValue().atStartOfDay();
        LocalDateTime endDate = endDatePicker.getValue().atTime(23, 59, 59);

        // Products
        List<Integer> productIds = new ArrayList<>();
        if (productSelector1 != null && productSelector1.getValue() != null) {
            productIds.add(productSelector1.getValue().getId());
        }
        if (productSelector2 != null && productSelector2.getValue() != null) {
            productIds.add(productSelector2.getValue().getId());
        }

        // Parse numeric inputs
        Double discountValue = null;
        Integer buyQty = null;
        Integer getQty = null;

        if (discountType == Sale.DiscountType.BUY_X_GET_Y) {
            try {
                buyQty = Integer.parseInt(buyXField.getText().trim());
                getQty = Integer.parseInt(getYField.getText().trim());
                if (buyQty <= 0 || getQty <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Quantities", "Buy X and Get Y must be positive integers.");
                    return null;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Invalid Quantities", "Please enter valid integers for Buy X and Get Y.");
                return null;
            }
        } else {
            try {
                String rawValue = valueField.getText().replace("%", "").trim();
                discountValue = Double.parseDouble(rawValue);
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Invalid Discount", "Please enter a valid number for the discount value.");
                return null;
            }
        }

        // Type-specific validation (the two checks you asked to add)
        switch (discountType) {
            case FIXED -> {
                if (productIds.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Missing Product", "Please select a product for a fixed discount.");
                    return null;
                }
                int productId = productIds.get(0);
                Product product = products.stream()
                        .filter(p -> p.getId() == productId)
                        .findFirst()
                        .orElse(null);
                if (product == null) {
                    showAlert(Alert.AlertType.ERROR, "Product Error", "Selected product was not found.");
                    return null;
                }
                if (discountValue == null || discountValue <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Discount", "Fixed discount must be greater than 0.");
                    return null;
                }
                if (discountValue > product.getPrice()) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Discount",
                            "Fixed discount (" + discountValue + ") cannot be more than the product price (" + product.getPrice() + ").");
                    return null;
                }
            }
            case PERCENTAGE -> {
                if (discountValue == null || discountValue < 0 || discountValue > 100) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Discount", "Percentage discount must be between 0 and 100.");
                    return null;
                }
            }
            case BUNDLE, BUY_X_GET_Y -> {
                // No extra monetary checks here unless you want bundle cap checks, etc.
            }
        }

        // Build sale
        Sale sale = new Sale();
        sale.setName(name);
        sale.setDescription(description);
        sale.setDiscountType(discountType);
        sale.setDiscountValue(discountValue);
        sale.setStartDate(startDate);
        sale.setEndDate(endDate);
        sale.setBuyQuantity(buyQty);
        sale.setGetQuantity(getQty);
        sale.setProductIds(productIds);

        return sale;
    }

    public boolean validInput() {
        if (nameField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Name", "Please enter a name.");
            return false;
        }
        if (descriptionField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Description", "Please enter a description.");
            return false;
        }
        if (discountTypeBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Discount Type", "Please choose a discount type.");
            return false;
        }
        if (startDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Start Date", "Please choose a start date.");
            return false;
        }
        if (endDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Missing End Date", "Please choose an end date.");
            return false;
        }
        if (startDatePicker.getValue().atStartOfDay()
                .isAfter(endDatePicker.getValue().atTime(23, 59, 59))) {
            showAlert(Alert.AlertType.WARNING, "Invalid Dates", "Start date should be before end date.");
            return false;
        }

        // Product selection rules
        List<Integer> productIds = new ArrayList<>();
        if (productSelector1 != null && productSelector1.getValue() != null)
            productIds.add(productSelector1.getValue().getId());
        if (productSelector2 != null && productSelector2.getValue() != null)
            productIds.add(productSelector2.getValue().getId());

        Sale.DiscountType selectedType = Sale.DiscountType.valueOf(discountTypeBox.getValue().toUpperCase());

        if (selectedType != Sale.DiscountType.BUNDLE && productIds.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Product", "Please select a product.");
            return false;
        }
        if (selectedType == Sale.DiscountType.BUNDLE && productIds.size() != 2) {
            showAlert(Alert.AlertType.WARNING, "Bundle Requires Two Products", "Please select two products for a bundle discount.");
            return false;
        }

        // For percentage/fixed/bundle types, make sure valueField exists (UI state)
        if (selectedType != Sale.DiscountType.BUY_X_GET_Y && (valueField == null || valueField.getText().trim().isEmpty())) {
            showAlert(Alert.AlertType.WARNING, "Missing Discount Value", "Please enter a discount value.");
            return false;
        }

        // For buy_x_get_y, make sure fields exist
        if (selectedType == Sale.DiscountType.BUY_X_GET_Y && (buyXField == null || getYField == null)) {
            showAlert(Alert.AlertType.WARNING, "Missing Quantities", "Please enter Buy X and Get Y.");
            return false;
        }

        return true;
    }

    public boolean saleAlreadyExists(Sale newSale) {
        for (Sale existing : sales) {
            // Compare types: treat FIXED and PERCENTAGE as equal
            boolean sameType =
                    newSale.getDiscountType() == existing.getDiscountType()
                            || ((newSale.getDiscountType() == Sale.DiscountType.PERCENTAGE || newSale.getDiscountType() == Sale.DiscountType.FIXED)
                            && (existing.getDiscountType() == Sale.DiscountType.PERCENTAGE || existing.getDiscountType() == Sale.DiscountType.FIXED));

            if (!sameType) continue;

            // Compare product IDs: exact match (same elements, order doesn't matter)
            List<Integer> newProducts = new ArrayList<>(newSale.getProductIds());
            List<Integer> existingProducts = new ArrayList<>(existing.getProductIds());
            newProducts.sort(Integer::compareTo);
            existingProducts.sort(Integer::compareTo);
            boolean sameProducts = newProducts.equals(existingProducts);
            if (!sameProducts) continue;

            // Check for date overlap
            boolean datesOverlap = existing.isActiveBetween(newSale.getStartDate(), newSale.getEndDate());
            if (datesOverlap) {
                return true;
            }
        }
        return false;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

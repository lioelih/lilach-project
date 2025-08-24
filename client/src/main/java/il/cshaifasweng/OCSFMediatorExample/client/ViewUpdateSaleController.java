package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/*
 * viewupdatesalecontroller
 * - shows a read-only summary of the sale and the involved products
 * - lets a worker update dates and discount value with type-specific validation
 * - confirms before updating or deleting, then notifies the server
 * - keeps ui compact: small product preview (image + name/type/price)
 */
public class ViewUpdateSaleController {

    @FXML private Label nameLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label typeLabel;
    @FXML private TextField valueField;
    @FXML private TextField buyQtyField;
    @FXML private TextField getQtyField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private VBox productBox1;
    @FXML private VBox productBox2;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button deleteButton;

    private Sale currentSale;
    private List<Product> allProducts;

    public void setSale(Sale sale, List<Product> products) {
        this.currentSale = sale;
        this.allProducts = products;

        // populate static fields
        nameLabel.setText(sale.getName());
        descriptionLabel.setText(sale.getDescription());
        typeLabel.setText(sale.getDiscountType().name());

        // dates
        if (sale.getStartDate() != null) startDatePicker.setValue(sale.getStartDate().toLocalDate());
        if (sale.getEndDate() != null) endDatePicker.setValue(sale.getEndDate().toLocalDate());

        // numeric fields
        valueField.setText(sale.getDiscountValue() != null ? sale.getDiscountValue().toString() : "");
        buyQtyField.setText(sale.getBuyQuantity() != null ? sale.getBuyQuantity().toString() : "");
        getQtyField.setText(sale.getGetQuantity() != null ? sale.getGetQuantity().toString() : "");

        // enable/disable relevant inputs based on type
        boolean enableValue = sale.getDiscountType() == Sale.DiscountType.PERCENTAGE
                || sale.getDiscountType() == Sale.DiscountType.FIXED
                || sale.getDiscountType() == Sale.DiscountType.BUNDLE;
        valueField.setDisable(!enableValue);

        boolean isBuyXGetY = sale.getDiscountType() == Sale.DiscountType.BUY_X_GET_Y;
        buyQtyField.setVisible(isBuyXGetY);
        getQtyField.setVisible(isBuyXGetY);

        // product previews
        productBox1.getChildren().clear();
        productBox2.getChildren().clear();
        loadProductDisplay(sale.getProductIds().get(0), productBox1);
        if (sale.getProductIds().size() > 1) {
            productBox2.setVisible(true);
            loadProductDisplay(sale.getProductIds().get(1), productBox2);
        } else {
            productBox2.setVisible(false);
        }

        setupListeners();
    }

    // builds a compact product preview (image + basic details)
    private void loadProductDisplay(int productId, VBox container) {
        Product product = allProducts.stream()
                .filter(p -> p.getId() == productId)
                .findFirst().orElse(null);
        if (product == null) return;

        container.getChildren().clear();

        Label name = new Label("Name: " + product.getName());
        Label type = new Label("Type: " + product.getType());
        Label price = new Label("Price: " + product.getPrice());
        ImageView imageView = new ImageView();

        if (product.getImage() != null) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(product.getImage());
                imageView.setImage(new Image(bis, 100, 100, true, true));
            } catch (Exception e) {
                imageView.setImage(null);
            }
        }

        container.getChildren().addAll(imageView, name, type, price);
    }

    // wires up save/cancel/delete; includes date & discount validations
    private void setupListeners() {
        cancelButton.setOnAction(e -> ((Stage) cancelButton.getScene().getWindow()).close());

        saveButton.setOnAction(e -> {
            // validate dates
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            if (start == null || end == null || end.isBefore(start)) {
                showAlert("Invalid Dates", "End date must be after start date.");
                return;
            }

            // commit dates to model
            currentSale.setStartDate(start.atStartOfDay());
            currentSale.setEndDate(end.atTime(23, 59));

            // validate value (when applicable), with type-specific rules
            if (!valueField.isDisabled()) {
                try {
                    double discount = Double.parseDouble(valueField.getText().replace("%", "").trim());
                    currentSale.setDiscountValue(discount);

                    // validate by discount type
                    switch (currentSale.getDiscountType()) {
                        case FIXED -> {
                            int productId = currentSale.getProductIds().get(0);
                            Product product = allProducts.stream()
                                    .filter(p -> p.getId() == productId)
                                    .findFirst()
                                    .orElse(null);

                            if (product != null && discount > product.getPrice()) {
                                showAlert("Invalid Discount",
                                        "Fixed discount (" + discount + ") cannot be more than the product price (" + product.getPrice() + ").");
                                return; // stop saving
                            }
                            if (discount <= 0) {
                                showAlert("Invalid Discount", "Fixed discount can't be less than 0.");
                                return; // stop saving
                            }
                        }
                        case PERCENTAGE -> {
                            if (discount < 0 || discount > 100) {
                                showAlert("Invalid Discount", "Percentage discount must be between 0 and 100.");
                                return; // stop saving
                            }
                        }
                    }
                } catch (NumberFormatException ex) {
                    showAlert("Invalid Discount", "Discount must be a valid number.");
                    return;
                }
            }

            // confirm and send update
            try {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirmation");
                alert.setHeaderText("Are you sure?");
                alert.setContentText("Do you really want to update " + currentSale.getName() + "?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    SimpleClient.getClient().sendToServer(new Msg("UPDATE_SALE", currentSale));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            ((Stage) saveButton.getScene().getWindow()).close();
        });

        deleteButton.setOnAction(e -> {
            // confirm and send delete
            try {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirmation");
                alert.setHeaderText("Are you sure?");
                alert.setContentText("Do you really want to delete " + currentSale.getName() + " from the store?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    SimpleClient.getClient().sendToServer(new Msg("DELETE_SALE", currentSale));
                }
                deleteButton.getScene().getWindow().hide();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    // small error helper
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

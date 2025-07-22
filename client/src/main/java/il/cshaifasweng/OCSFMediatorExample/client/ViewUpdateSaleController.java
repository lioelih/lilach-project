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

        nameLabel.setText(sale.getName());
        descriptionLabel.setText(sale.getDescription());
        typeLabel.setText(sale.getDiscountType().name());

        if (sale.getStartDate() != null) startDatePicker.setValue(sale.getStartDate().toLocalDate());
        if (sale.getEndDate() != null) endDatePicker.setValue(sale.getEndDate().toLocalDate());

        valueField.setText(sale.getDiscountValue() != null ? sale.getDiscountValue().toString() : "");
        buyQtyField.setText(sale.getBuyQuantity() != null ? sale.getBuyQuantity().toString() : "");
        getQtyField.setText(sale.getGetQuantity() != null ? sale.getGetQuantity().toString() : "");

        boolean enableValue = sale.getDiscountType() == Sale.DiscountType.PERCENTAGE ||
                sale.getDiscountType() == Sale.DiscountType.FIXED ||
                sale.getDiscountType() == Sale.DiscountType.BUNDLE;
        valueField.setDisable(!enableValue);

        boolean isBuyXGetY = sale.getDiscountType() == Sale.DiscountType.BUY_X_GET_Y;
        buyQtyField.setVisible(isBuyXGetY);
        getQtyField.setVisible(isBuyXGetY);

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

    private void setupListeners() {
        cancelButton.setOnAction(e -> ((Stage) cancelButton.getScene().getWindow()).close());

        saveButton.setOnAction(e -> {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            if (start == null || end == null || end.isBefore(start)) {
                showAlert("Invalid Dates", "End date must be after start date.");
                return;
            }

            currentSale.setStartDate(start.atStartOfDay());
            currentSale.setEndDate(end.atTime(23, 59));

            if (!valueField.isDisabled()) {
                try {
                    double discount = Double.parseDouble(valueField.getText().replace("%", "").trim());
                    currentSale.setDiscountValue(discount);
                } catch (NumberFormatException ex) {
                    showAlert("Invalid Discount", "Discount must be a valid number.");
                    return;
                }
            }

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

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

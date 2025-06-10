package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ProductViewController {

    @FXML
    private Label nameLabel;

    @FXML
    private Label typeLabel;

    @FXML
    private ImageView imageView;

    @FXML
    private TextField priceField;

    @FXML
    private Button updatePriceButton;

    private Product product;

    public void setProduct(Product product) {
        this.product = product;

        nameLabel.setText("Name: " + product.getName());
        typeLabel.setText("Type: " + product.getType());
        priceField.setText(String.format("%.2f", product.getPrice()));

        try {
            if (product.getImage() != null && !product.getImage().isEmpty()) {
                imageView.setImage(new Image(product.getImage(), true));
            }
        } catch (Exception e) {
            imageView.setImage(null);
        }

        updatePriceButton.setOnAction(e -> { // This will help us update the price whenever we view page
            try {
                double newPrice = Double.parseDouble(priceField.getText());
                product.setPrice(newPrice);
                SimpleClient.getClient().sendToServer(product); // send updated product
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Price updated!");
                alert.showAndWait();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid price format!").showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}

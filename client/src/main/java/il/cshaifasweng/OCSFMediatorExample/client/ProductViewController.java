package il.cshaifasweng.OCSFMediatorExample.client;

import Events.WarningEvent;
import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import org.greenrobot.eventbus.Subscribe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public class ProductViewController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField typeField;

    @FXML
    private ImageView imageView;

    @FXML
    private TextField priceField;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;



    private File droppedImageFile = null; // holds the file temporarily


    public void setProduct(Product product) {

        nameField.setText(product.getName());
        typeField.setText(product.getType());
        priceField.setText(String.format("%.2f", product.getPrice()));

        try {
            byte[] imageBytes = product.getImage();
            if (imageBytes != null && imageBytes.length > 0) {
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                imageView.setImage(image);
            } else {
                imageView.setImage(null);
            }
        } catch (Exception e) {
            imageView.setImage(null);
            e.printStackTrace();
        }

        imageView.setOnDragOver(event -> {
            if (event.getGestureSource() != imageView && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        imageView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".png")) {
                    droppedImageFile = file;
                    imageView.setImage(new Image(file.toURI().toString()));
                    success = true;
                } else {
                    showAlert("Only PNG files are allowed.");
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        updateButton.setOnAction(e -> {
            try {
                String newName = nameField.getText().trim();
                String newType = typeField.getText().trim();
                String stringPrice = priceField.getText().trim();
                if (newName.isEmpty() || newType.isEmpty() || stringPrice.isEmpty()) {
                    showAlert("Please fill in all fields.");
                    return;
                }
                double newPrice;
                try {
                    newPrice = Double.parseDouble(stringPrice);
                } catch (NumberFormatException err) {
                    showAlert("Price must be a valid number.");
                    return;
                }

                byte[] newImage;
                try {
                    if (droppedImageFile == null) newImage = product.getImage();
                    else newImage = Files.readAllBytes(droppedImageFile.toPath());
                } catch (IOException err) {
                    showAlert("Failed to read image file.");
                    return;
                }
                product.updateProduct(newName,newType,newPrice,newImage);

                Msg msg = new Msg("UPDATE_PRODUCT", product);
                SimpleClient.getClient().sendToServer(msg);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Product updated!");
                alert.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        deleteButton.setOnAction(e -> {
            try {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirmation");
                alert.setHeaderText("Are you sure?");
                alert.setContentText("Do you really want to delete" + product.getName() + "from the store?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    Msg msg = new Msg("DELETE_PRODUCT", product);
                    SimpleClient.getClient().sendToServer(msg);
                } else {
                    // User clicked Cancel
                    System.out.println("Cancelled.");
                }
            } catch (IOException err) {
                err.printStackTrace();
            }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Input Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}

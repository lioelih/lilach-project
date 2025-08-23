package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import javafx.stage.Stage;

public class AddProductController {

    @FXML public Button cancelButton;
    @FXML private TextField nameField;
    @FXML private TextField typeField;
    @FXML private TextField priceField;
    @FXML private Button addButton;
    @FXML private ImageView imageDrop;

    private File droppedImageFile = null; // holds the file temporarily (image)

    @FXML
    public void initialize() {
        imageDrop.setImage(new Image("/image/drag_drop.png")); // placeholder image
        imageDrop.setOnDragOver(event -> {
            if (event.getGestureSource() != imageDrop && event.getDragboard().hasFiles()) {
                File file = event.getDragboard().getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".png")) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
            }
            event.consume();
        });
        imageDrop.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".png")) {
                    droppedImageFile = file;
                    imageDrop.setImage(new Image(file.toURI().toString()));
                    success = true;
                } else {
                    showAlert("Only PNG files are allowed.");
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        cancelButton.setOnAction(e -> ((Stage) cancelButton.getScene().getWindow()).close());
        addButton.setOnAction(event -> handleAddProduct());
    }

    private void handleAddProduct() {
        String name = nameField.getText().trim();
        String type = typeField.getText().trim();
        String priceStr = priceField.getText().trim();

        if (name.isEmpty() || type.isEmpty() || priceStr.isEmpty()) { // Must surpass all necessary checks before input
            showAlert("Please fill in all fields.");
            return;
        }
        if (droppedImageFile == null) {
            showAlert("Please drag and drop a PNG image.");
            return;
        }
        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            showAlert("Price must be a valid number.");
            return;
        }
        byte[] imageBytes;
        try {
            imageBytes = Files.readAllBytes(droppedImageFile.toPath());
        } catch (IOException e) {
            showAlert("Failed to read image file.");
            return;
        }
        try {
            Product product = new Product(name, type, price, imageBytes);
            Msg massage = new Msg("ADD_PRODUCT", product);
            SimpleClient.getClient().sendToServer(massage);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Product Added!");
            alert.showAndWait();
            Stage stage = (Stage) addButton.getScene().getWindow();
            stage.close();
        } catch (IOException e) {
            showAlert("Failed to send product to server.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Input Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

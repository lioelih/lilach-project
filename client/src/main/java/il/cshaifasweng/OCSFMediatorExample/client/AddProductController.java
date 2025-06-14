package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import il.cshaifasweng.OCSFMediatorExample.entities.Product;

public class AddProductController {

    @FXML private TextField nameField;
    @FXML private TextField typeField;
    @FXML private TextField priceField;
    @FXML private Button addButton;
    @FXML private ImageView imageDrop;

    private File droppedImageFile = null;  // holds image until saved

    @FXML
    public void initialize() {
        imageDrop.setImage(new Image("/image/drag_drop.png"));

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
                    droppedImageFile = file; // store for later save
                    imageDrop.setImage(new Image(file.toURI().toString()));
                    success = true;
                } else {
                    showAlert("Only PNG files are allowed.");
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });

        addButton.setOnAction(event -> handleAddProduct());
    }

    private void handleAddProduct() {
        String name = nameField.getText().trim();
        String type = typeField.getText().trim();
        String price = priceField.getText().trim();

        if (name.isEmpty() || type.isEmpty() || price.isEmpty()) {
            showAlert("Please fill in all fields.");
            return;
        }

        if (droppedImageFile == null) {
            showAlert("Please drag and drop a PNG image.");
            return;
        }

        try {
            saveImage(name, droppedImageFile);
            System.out.println("Image saved as: " + name + ".png");
            // Proceed to save product in database (if you want)

        } catch (IOException e) {
            showAlert("Failed to save image.");
        }

        try {
            SimpleClient.getClient().sendToServer(new Product(name, type, Double.parseDouble(price),"/image/" + name + ".png")); // send new product
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Product Added!");
            alert.showAndWait();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void saveImage(String productName, File sourceFile) throws IOException {
        File destDir = new File("client/src/main/resources/image");
        if (!destDir.exists()) destDir.mkdirs();

        File destFile = new File(destDir, productName + ".png");

        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Input Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

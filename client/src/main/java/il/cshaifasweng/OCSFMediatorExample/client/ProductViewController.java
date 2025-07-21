package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public class ProductViewController {

    @FXML private TextField nameField, typeField, priceField, qtyField;
    @FXML private ImageView imageView;
    @FXML private Button updateButton, deleteButton, saveStockBtn;
    @FXML private ComboBox<Branch> branchBox;

    private Product product;
    private File droppedImageFile;

    @FXML private void initialize() {
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().register(this);

        branchBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Branch b, boolean empty){
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : b.getName());
            }
        });
        branchBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Branch b, boolean empty){
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : b.getName());
            }
        });

        saveStockBtn.setDisable(true);
        branchBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldV, newV) -> saveStockBtn.setDisable(newV == null));
    }

    public void setProduct(Product product) {
        this.product = product;

        nameField.setText(product.getName());
        typeField.setText(product.getType());
        priceField.setText(String.format("%.2f", product.getPrice()));

        if (product.getImage() != null && product.getImage().length > 0) {
            ByteArrayInputStream bis = new ByteArrayInputStream(product.getImage());
            Image image = new Image(bis);
            if (image.isError()) {
                System.out.println("Image load error: " + image.getException());
                imageView.setImage(new Image("/image/drag_drop.png"));
                imageView.setFitWidth(300);
                imageView.setFitHeight(200);
                imageView.setPreserveRatio(true);
            }
            else {
                imageView.setImage(image);
                imageView.setFitWidth(300);
                imageView.setFitHeight(200);
                imageView.setPreserveRatio(true);
            }
        } else {
            imageView.setImage(null);
            imageView.setImage(new Image("/image/drag_drop.png"));
            imageView.setFitWidth(300);
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(true);
        }

        // Drag and drop image logic
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
                droppedImageFile = file;
                imageView.setImage(new Image(file.toURI().toString()));
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Ask server for branches
        try {
            SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null));
        } catch (IOException ex) { ex.printStackTrace(); }

        branchBox.valueProperty().addListener((obs, oldB, newB) -> {
            if (newB == null) return;
            try {
                SimpleClient.getClient().sendToServer(
                        new Msg("FETCH_STOCK_SINGLE", new Object[]{ product.getId(), newB.getBranchId() }));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        saveStockBtn.setOnAction(ev -> {
            Branch br = branchBox.getValue();
            if (br == null) {
                showAlert("Choose a branch first.");
                return;
            }

            if (qtyField.getText().isBlank()) {
                showAlert("Enter a quantity.");
                return;
            }

            int qty;
            try {
                qty = Integer.parseInt(qtyField.getText().trim());
            } catch (NumberFormatException ex) {
                showAlert("Quantity must be a whole number.");
                return;
            }

            if (qty < 0) {
                showAlert("Quantity can’t be negative.");
                return;
            }

            int[] payload = { product.getId(), br.getBranchId(), qty };

            try {
                SimpleClient.getClient().sendToServer(new Msg("ADD_STOCK", payload));
                saveStockBtn.setDisable(true);
            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert("Failed to contact server.");
            }
        });

        // Restore update button functionality
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
                    if (droppedImageFile == null)
                        newImage = product.getImage();
                    else
                        newImage = Files.readAllBytes(droppedImageFile.toPath());
                } catch (IOException err) {
                    showAlert("Failed to read image file.");
                    return;
                }

                product.updateProduct(newName, newType, newPrice, newImage);
                SimpleClient.getClient().sendToServer(new Msg("UPDATE_PRODUCT", product));
                Alert alert = new Alert(AlertType.INFORMATION, "Product updated!");
                alert.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Restore delete button functionality
        deleteButton.setOnAction(e -> {
            try {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Confirmation");
                alert.setHeaderText("Are you sure?");
                alert.setContentText("Do you really want to delete " + product.getName() + " from the store?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    SimpleClient.getClient().sendToServer(new Msg("DELETE_PRODUCT", product));
                }
                deleteButton.getScene().getWindow().hide();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    @Subscribe
    public void onBranches(Msg m) {
        if (!"BRANCHES_OK".equals(m.getAction())) return;
        List<Branch> branches = (List<Branch>) m.getData();
        Platform.runLater(() -> {
            branchBox.getItems().setAll(branches);
            branchBox.getSelectionModel().clearSelection();
        });
    }

    @Subscribe
    public void onStockSaved(Msg m) {
        if (!"ADD_STOCK_OK".equals(m.getAction())) return;
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION, "Quantity saved to storage!", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            qtyField.clear();
        });
    }

    @Subscribe
    public void handleSingleStock(Msg m) {
        if (!"STOCK_SINGLE_OK".equals(m.getAction())) return;
        Integer qty = (Integer) m.getData();
        Platform.runLater(() -> qtyField.setText(String.valueOf(qty)));
    }

    @FXML private void onClose() {
        EventBus.getDefault().unregister(this);
        // then close the Stage, e.g.
        ((Stage) nameField.getScene().getWindow()).close();
    }

    private void showAlert(String txt) {
        Alert alert = new Alert(AlertType.WARNING, txt, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
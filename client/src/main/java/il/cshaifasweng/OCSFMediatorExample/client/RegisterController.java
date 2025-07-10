package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import il.cshaifasweng.Msg;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneNumberField;
    @FXML private TextField nameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> branchComboBox;
    @FXML private Button submitButton;
    @FXML private Button backButton;
    @FXML private ImageView logoImage;
    @FXML
    public void initialize() {
        backButton.setOnAction(e -> SceneController.switchScene("home"));
        submitButton.setOnAction(event -> {
            String[] fields = new String[]{
                    usernameField.getText(),
                    emailField.getText(),
                    phoneNumberField.getText(),
                    nameField.getText(),
                    passwordField.getText(),
                    branchComboBox.getValue()
            };

            if (!isValidInput(fields)) return;

            try {
                SimpleClient.getClient().sendToServer(new Msg("REGISTER", fields));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Image logo = new Image(getClass().getResourceAsStream("/image/logo.png"));
        logoImage.setImage(logo);
    }

    private boolean isValidInput(String[] f) {
        if (f[0].isBlank() || f[1].isBlank() || f[2].isBlank() || f[3].isBlank() || f[4].isBlank() || f[5] == null) {
            showAlert("All fields are required.");
            return false;
        }
        if (f[0].contains(" ") || f[4].contains(" ")) {
            showAlert("Username or password cannot contain spaces.");
            return false;
        }
        if (!f[1].matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showAlert("Invalid email format.");
            return false;
        }
        if (!f[2].matches("\\d{10}")) {
            showAlert("Phone must be 10 digits.");
            return false;
        }
        if (f[4].length() < 4 || f[4].length() > 16) {
            showAlert("Password must be 4–16 characters.");
            return false;
        }
        return true;
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}

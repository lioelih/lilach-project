package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;

/*
 * login controller:
 * - validates credentials locally and sends a login request to the server
 * - ensures the client is connected before sending
 * - shows user-friendly error messages on failures
 * - sets the app logo on load
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button submitButton;
    @FXML private Button backButton;
    @FXML private ImageView logoImage;

    /* initialize ui bindings: back navigation, submit action, and logo image */
    @FXML
    public void initialize() {
        backButton.setOnAction(e -> SceneController.switchScene("home"));
        submitButton.setOnAction(event -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            if (!isValidInput(username, password)) return;

            try {
                if (!SimpleClient.ensureConnected()) {
                    showAlert("Connection to server was lost. Please try again.");
                    return;
                }
                SimpleClient.getClient().sendToServer(new Msg("LOGIN", new String[]{username, password}));
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Failed to send login. " + e.getMessage());
            }
        });

        Image logo = new Image(getClass().getResourceAsStream("/image/logo.png"));
        logoImage.setImage(logo);
    }

    /* client-side validation before sending login */
    private boolean isValidInput(String username, String password) {
        if (username.isBlank() || password.isBlank()) {
            showAlert("Username and password are required.");
            return false;
        }
        if (username.contains(" ")) {
            showAlert("Username cannot contain spaces.");
            return false;
        }
        if (password.length() < 4 || password.length() > 16) {
            showAlert("Password must be 4–16 characters.");
            return false;
        }
        return true;
    }

    /* utility: show an error alert on the fx thread */
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

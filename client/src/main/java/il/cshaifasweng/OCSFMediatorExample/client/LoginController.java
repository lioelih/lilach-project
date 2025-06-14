package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import java.sql.*;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button submitButton;

    @FXML
    public void initialize() {
        submitButton.setOnAction(event -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (!isValidInput(username, password)) return;

            try (Connection conn = DBUtil.getConnection()) {
                String query = "SELECT * FROM users WHERE username = ? AND password = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, username);
                stmt.setString(2, password);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    SceneController.loggedUsername = username;
                    SceneController.switchScene("home");
                } else {
                    showAlert("Invalid credentials");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

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
            showAlert("Password must be at least 4 characters long and at most 16 characters.");
            return false;
        }
        return true;
    }

    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

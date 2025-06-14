package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import java.sql.*;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField nameField;
    @FXML private PasswordField passwordField;
    @FXML private Button submitButton;

    @FXML
    public void initialize() {
        submitButton.setOnAction(event -> {
            String username = usernameField.getText();
            String email = emailField.getText();
            String fullName = nameField.getText();
            String password = passwordField.getText();

            if (!isValidInput(username, email, fullName, password)) return;

            try (Connection conn = DBUtil.getConnection()) {
                String checkQuery = "SELECT * FROM users WHERE username = ? OR email = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                checkStmt.setString(1, username);
                checkStmt.setString(2, email);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    showAlert("User Already Exists");
                    return;
                }

                String query = "INSERT INTO users (username, password, email, full_name, role) VALUES (?, ?, ?, ?, 'USER')";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setString(3, email);
                stmt.setString(4, fullName);

                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    showSuccessWindow("Registration successful! You can now login.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isValidInput(String username, String email, String fullName, String password) {
        if (username.isBlank() || password.isBlank() || email.isBlank() || fullName.isBlank()) {
            showAlert("All fields are required.");
            return false;
        }
        if (username.contains(" ")) {
            showAlert("Username cannot contain spaces.");
            return false;
        }
        if (password.contains(" ")) {
            showAlert("Password cannot contain spaces.");
            return false;
        }
        if (!email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showAlert("Invalid email format.");
            return false;
        }
        if (password.length() < 4 || password.length() > 16) {
            showAlert("Password must be at least 8 characters long and at most 16 characters.");
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
    private void showSuccessWindow(String message) { // Only for registeration
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("Success");

        javafx.scene.control.Label label = new javafx.scene.control.Label(message);
        label.setStyle("-fx-text-fill: green; -fx-font-size: 16px;");

        javafx.scene.control.Button okButton = new javafx.scene.control.Button("OK");
        okButton.setOnAction(e -> {
            dialog.close();
            SceneController.switchScene("login");
        });

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10, label, okButton);
        vbox.setStyle("-fx-padding: 20; -fx-alignment: center;");

        dialog.setScene(new javafx.scene.Scene(vbox));
        dialog.show();
    }
}

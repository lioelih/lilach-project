package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import java.sql.*;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField nameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button submitButton;

    @FXML
    public void initialize() {
        submitButton.setOnAction(event -> {
            String username = usernameField.getText();
            String email = emailField.getText();
            String fullName = nameField.getText();
            String password = passwordField.getText();

            try (Connection conn = DBUtil.getConnection()) {
                String query = "INSERT INTO users (username, password, email, full_name, role) VALUES (?, ?, ?, ?, 'USER')";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setString(3, email);
                stmt.setString(4, fullName);

                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    SceneController.switchScene("login");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
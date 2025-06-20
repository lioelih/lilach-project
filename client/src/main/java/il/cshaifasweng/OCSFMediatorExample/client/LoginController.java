package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import java.sql.*;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;

import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.entities.LoginRequest;
import il.cshaifasweng.OCSFMediatorExample.entities.LoginResponse;

import java.io.IOException;


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

            try {
                SimpleClient.getClient().sendToServer(new LoginRequest(username, password));
                SceneController.loggedUsername = username;
                System.out.println("Login button clicked, sending LoginRequest for: " + username);
            } catch (IOException e) {
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
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}

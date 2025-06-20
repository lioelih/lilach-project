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
import il.cshaifasweng.OCSFMediatorExample.entities.RegisterRequest;
import il.cshaifasweng.OCSFMediatorExample.entities.RegisterResponse;


import java.io.IOException;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneNumberField;
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
            String phoneNumber = phoneNumberField.getText();
            String fullName = nameField.getText();
            String password = passwordField.getText();

            if (!isValidInput(username, email,phoneNumber, fullName, password)) return;

            try {
                RegisterRequest request = new RegisterRequest(username, email, phoneNumber, fullName, password);
                SimpleClient.getClient().sendToServer(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isValidInput(String username, String email, String phoneNumber, String fullName, String password) {
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
            showAlert("Password must be at least 4 characters long and at most 16 characters.");
            return false;
        }
        if (phoneNumber == null || !phoneNumber.matches("\\d{10}")) {
            showAlert("Phone number must be exactly 10 digits.");
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

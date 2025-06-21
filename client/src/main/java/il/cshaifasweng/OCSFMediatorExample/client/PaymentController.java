package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.io.IOException;
import java.util.function.UnaryOperator;
import org.greenrobot.eventbus.*;
import javafx.application.Platform;
import il.cshaifasweng.OCSFMediatorExample.entities.PaymentPrefillRequest;
import il.cshaifasweng.OCSFMediatorExample.entities.PaymentPrefillResponse;

import il.cshaifasweng.OCSFMediatorExample.entities.PaymentInfoRequest;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;

public class PaymentController {

    @FXML private TextField idNumberField;
    @FXML private TextField cardNumberField;
    @FXML private TextField expDateField;
    @FXML private TextField cvvField;
    @FXML private Button confirmButton;
    @FXML private Button cancelButton;

    private Runnable onSuccess;  // Callback for VIP/catalog-specific success handling

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    public void initialize() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        setupExpirationField();

        if (SceneController.loggedUsername != null) {
            try {
                SimpleClient.getClient().sendToServer(new PaymentPrefillRequest(SceneController.loggedUsername));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        confirmButton.setOnAction(e -> {
            if (!validateFields()) return;

            try {
                PaymentInfoRequest request = new PaymentInfoRequest(
                        SceneController.loggedUsername,
                        getIdNumber(),
                        getCardNumber(),
                        getExpDate(),
                        getCVV(),
                        true // assumes VIP; adapt this if used for catalog too
                );
                SimpleClient.getClient().sendToServer(request);
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Failed to send payment info to server.", Alert.AlertType.ERROR);
                return;
            }

            showAlert("Purchase Successful!", Alert.AlertType.INFORMATION);

            if (onSuccess != null) {
                onSuccess.run();
            }

            closeWindow();
        });

        cancelButton.setOnAction(e -> closeWindow());
    }

    private void setupExpirationField() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            if (text.matches("\\d{0,2}")) return change;
            if (text.matches("\\d{2}/\\d{0,2}")) return change;
            return null;
        };
        TextFormatter<String> formatter = new TextFormatter<>(filter);
        expDateField.setTextFormatter(formatter);

        expDateField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() == 2 && !newVal.contains("/")) {
                expDateField.setText(newVal + "/");
            }
        });
    }

    private boolean validateFields() {
        if (idNumberField.getText().length() != 9 || !idNumberField.getText().matches("\\d+")) {
            showAlert("Invalid identification number.", Alert.AlertType.ERROR);
            return false;
        }
        if (!cardNumberField.getText().matches("\\d{16}")) {
            showAlert("Invalid credit card number.", Alert.AlertType.ERROR);
            return false;
        }
        if (!expDateField.getText().matches("\\d{2}/\\d{2}")) {
            showAlert("Invalid expiration date format.", Alert.AlertType.ERROR);
            return false;
        }
        if (!cvvField.getText().matches("\\d{3,4}")) {
            showAlert("Invalid CVV.", Alert.AlertType.ERROR);
            return false;
        }
        return true;
    }

    private void showAlert(String msg, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(type == Alert.AlertType.ERROR ? "Error" : "Success");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private void closeWindow() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        stage.close();
    }

    // Getters used by VIPController or CatalogController
    public String getIdNumber() {
        return idNumberField.getText();
    }

    public String getCardNumber() {
        return cardNumberField.getText();
    }

    public String getExpDate() {
        return expDateField.getText();
    }

    public String getCVV() {
        return cvvField.getText();
    }
    @Subscribe
    public void handlePrefill(PaymentPrefillResponse response) {
        System.out.println("CLIENT: Received PaymentPrefillResponse, filling fields...");
        Platform.runLater(() -> {
            idNumberField.setText(response.getIdNumber());
            cardNumberField.setText(response.getCardNumber());
            expDateField.setText(response.getExpDate());
            cvvField.setText(response.getCvv());
        });
    }
    public void onClose() {
        EventBus.getDefault().unregister(this);
    }
}

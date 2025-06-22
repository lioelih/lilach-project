package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.io.IOException;
import java.util.function.UnaryOperator;
import org.greenrobot.eventbus.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.*;

public class PaymentController {

    @FXML private TextField idNumberField;
    @FXML private TextField cardNumberField;
    @FXML private TextField expDateField;
    @FXML private TextField cvvField;
    @FXML private Button confirmButton;
    @FXML private Button cancelButton;

    private Runnable onSuccess;

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    public void initialize() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        setupExpirationField();
        setupCardNumberField();
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
                        true
                );
                SimpleClient.getClient().sendToServer(request);
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Failed to send payment info to server.", Alert.AlertType.ERROR);
                return;
            }

            showAlert("Purchase Successful!", Alert.AlertType.INFORMATION);
            if (onSuccess != null) onSuccess.run();
            closeWindow();
        });

        cancelButton.setOnAction(e -> closeWindow());
    }

    private void setupCardNumberField() {
        cardNumberField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();

            // Strip all non-digits
            String digits = newText.replaceAll("\\D", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);

            // Format: add space every 4 digits
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) formatted.append(" ");
                formatted.append(digits.charAt(i));
            }

            // If the formatted result is same as current, keep caret/selection
            if (formatted.toString().equals(change.getControlText())) return change;

            int anchor = change.getAnchor();
            int caret = change.getCaretPosition();

            // Set full replacement
            change.setText(formatted.toString());
            change.setRange(0, change.getControlText().length());

            // Adjust caret and anchor (based on digits before them)
            int rawCaret = 0;
            for (int i = 0, count = 0; i < formatted.length(); i++) {
                if (Character.isDigit(formatted.charAt(i))) {
                    count++;
                }
                if (count == caret) {
                    rawCaret = i + 1;
                    break;
                }
            }

            final int finalCaret = Math.min(rawCaret, formatted.length());
            Platform.runLater(() -> cardNumberField.positionCaret(finalCaret));
            return change;
        }));

        // Handle BACK_SPACE + space deletion
        cardNumberField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode().toString().equals("BACK_SPACE")) {
                int caret = cardNumberField.getCaretPosition();
                if (caret > 0 && cardNumberField.getText().charAt(caret - 1) == ' ') {
                    event.consume();
                    Platform.runLater(() -> {
                        String digits = cardNumberField.getText().replaceAll("\\D", "");
                        if (digits.length() > 0) {
                            StringBuilder updated = new StringBuilder(digits.substring(0, digits.length() - 1));
                            cardNumberField.setText(formatCard(updated.toString()));
                            cardNumberField.positionCaret(Math.max(0, cardNumberField.getText().length()));
                        }
                    });
                }
            }
        });
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
            } else if (newVal.length() == 3 && newVal.charAt(2) != '/') {
                expDateField.setText(newVal.substring(0, 2) + "/" + newVal.charAt(2));
            } else if (newVal.length() < 3 && oldVal.contains("/")) {
                expDateField.setText(newVal.replace("/", ""));
            }
        });
    }

    private String formatCard(String digits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append(" ");
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }


    private boolean validateFields() {
        if (idNumberField.getText().length() != 9 || !idNumberField.getText().matches("\\d+")) {
            showAlert("Invalid identification number.", Alert.AlertType.ERROR);
            return false;
        }
        if (!cardNumberField.getText().replaceAll(" ", "").matches("\\d{16}")) {
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

    public String getIdNumber() {
        return idNumberField.getText();
    }

    public String getCardNumber() {
        return cardNumberField.getText().replaceAll(" ", "");
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
            cardNumberField.setText(formatCard(response.getCardNumber()));
            expDateField.setText(response.getExpDate());
            cvvField.setText(response.getCvv());
        });
    }

    public void onClose() {
        EventBus.getDefault().unregister(this);
    }
}
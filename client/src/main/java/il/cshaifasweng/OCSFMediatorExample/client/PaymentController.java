package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.function.UnaryOperator;

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
        setupCVVField();
        try {
            SimpleClient.getClient().sendToServer(new Msg("PAYMENT_PREFILL", SceneController.loggedUsername));
        } catch (IOException e) {
            e.printStackTrace();
        }
        confirmButton.setOnAction(e -> {
            if (!validateFields()) return;

            try {
                String[] paymentData = new String[]{
                        SceneController.loggedUsername,
                        getIdNumber(),
                        getCardNumber(),
                        getExpDate(),
                        getCVV()
                };
                SimpleClient.getClient().sendToServer(new Msg("PAYMENT_INFO", paymentData));

                if (onSuccess != null) onSuccess.run();
                closeWindow();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        cancelButton.setOnAction(e -> closeWindow());
    }

    @Subscribe
    public void handlePrefillData(Msg msg) {
        if (!"PAYMENT_PREFILL".equals(msg.getAction())) return;

        String[] data = (String[]) msg.getData();

        Platform.runLater(() -> {
            idNumberField.setText(data[0] != null ? data[0] : "");
            cardNumberField.setText(formatCard(data[1] != null ? data[1] : ""));
            expDateField.setText(data[2] != null ? data[2] : "");
            cvvField.setText(data[3] != null ? data[3] : "");
        });
    }

    private void setupCardNumberField() {
        cardNumberField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            String digits = newText.replaceAll("\\D", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);

            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) formatted.append(" ");
                formatted.append(digits.charAt(i));
            }

            if (formatted.toString().equals(change.getControlText())) return change;

            change.setText(formatted.toString());
            change.setRange(0, change.getControlText().length());
            return change;
        }));

        cardNumberField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if ("BACK_SPACE".equals(event.getCode().toString())) {
                int caret = cardNumberField.getCaretPosition();
                if (caret > 0 && cardNumberField.getText().charAt(caret - 1) == ' ') {
                    event.consume();
                    Platform.runLater(() -> {
                        String digits = cardNumberField.getText().replaceAll("\\D", "");
                        if (!digits.isEmpty()) {
                            StringBuilder updated = new StringBuilder(digits.substring(0, digits.length() - 1));
                            cardNumberField.setText(formatCard(updated.toString()));
                            cardNumberField.positionCaret(cardNumberField.getText().length());
                        }
                    });
                }
            }
        });
    }

    private void setupCVVField() {
        cvvField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d{0,3}")) {
                return change;
            }
            return null;
        }));
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
        if (idNumberField.getText().length() != 9 || !idNumberField.getText().matches("\\d+")) return false;
        if (!cardNumberField.getText().replaceAll(" ", "").matches("\\d{16}")) return false;
        if (!expDateField.getText().matches("\\d{2}/\\d{2}")) return false;
        if (!cvvField.getText().matches("\\d{3,4}")) return false;
        return true;
    }

    private void closeWindow() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        EventBus.getDefault().unregister(this);
        stage.close();
    }

    public String getIdNumber() { return idNumberField.getText(); }
    public String getCardNumber() { return cardNumberField.getText().replaceAll(" ", ""); }
    public String getExpDate() { return expDateField.getText(); }
    public String getCVV() { return cvvField.getText(); }
}

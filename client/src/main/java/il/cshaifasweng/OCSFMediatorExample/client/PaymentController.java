package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.time.YearMonth;

/*
 * payment controller
 * - formats and validates card number, expiration, and cvv
 * - pre-fills fields from the server when available
 * - posts updated card details back to the server
 */
public class PaymentController {

    @FXML private TextField cardNumberField; // 16 digits, grouped in 4s
    @FXML private TextField expDateField;    // mm/yy
    @FXML private TextField cvvField;        // 3 digits
    @FXML private Button confirmButton;
    @FXML private Button cancelButton;

    private Runnable onSuccess;
    public void setOnSuccess(Runnable onSuccess) { this.onSuccess = onSuccess; }

    @FXML
    public void initialize() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        setupCardNumberField();
        setupExpirationField();
        setupCVVField();

        try {
            SimpleClient.getClient().sendToServer(new Msg("PAYMENT_PREFILL", SceneController.loggedUsername));
        } catch (IOException e) {
            e.printStackTrace();
        }

        confirmButton.setOnAction(e -> {
            if (!validateAndAlert()) return;
            try {
                String[] payload = new String[]{
                        SceneController.loggedUsername,
                        getCardNumber(),
                        getExpDate(),
                        getCVV()
                };
                SimpleClient.getClient().sendToServer(new Msg("PAYMENT_INFO", payload));
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
        String action = msg.getAction();
        if (!( "PAYMENT_PREFILL".equals(action) || "PAYMENT_PREFILL_OK".equals(action) )) return;

        Object dataObj = msg.getData();
        if (!(dataObj instanceof String[] arr)) return;

        Platform.runLater(() -> {
            String cardDigits = stripNonDigits(safe(arr, 0));
            String expDigits  = stripNonDigits(safe(arr, 1));
            String cvvDigits  = stripNonDigits(safe(arr, 2));

            cardNumberField.setText(groupEvery4(limitDigits(cardDigits, 16)));

            if (!expDigits.isEmpty()) {
                expDigits = limitDigits(expDigits, 4);
                expDateField.setText(expDigits.length() <= 2
                        ? expDigits
                        : expDigits.substring(0, 2) + "/" + expDigits.substring(2));
            } else {
                expDateField.clear();
            }

            cvvField.setText(limitDigits(cvvDigits, 3));
        });
    }

    private void setupCardNumberField() {
        cardNumberField.setTextFormatter(new TextFormatter<>(change -> {
            String controlText = change.getControlText();
            int rangeStart = change.getRangeStart();
            int rangeEnd = change.getRangeEnd();
            String insertedRaw = change.getText();

            String insertedDigits = insertedRaw.replaceAll("\\D", "");
            int startDigit = countDigitsBefore(controlText, rangeStart);
            int endDigit   = countDigitsBefore(controlText, rangeEnd);
            String oldDigits = controlText.replaceAll("\\D", "");

            int newLenIfAll = oldDigits.length() - (endDigit - startDigit) + insertedDigits.length();
            if (newLenIfAll > 16) {
                int allow = 16 - (oldDigits.length() - (endDigit - startDigit));
                if (allow < 0) allow = 0;
                if (insertedDigits.length() > allow) insertedDigits = insertedDigits.substring(0, allow);
            }

            StringBuilder newDigits = new StringBuilder();
            newDigits.append(oldDigits, 0, Math.min(startDigit, oldDigits.length()));
            newDigits.append(insertedDigits);
            if (endDigit < oldDigits.length()) newDigits.append(oldDigits.substring(endDigit));

            String formatted = groupEvery4(newDigits.toString());

            int caretDigitIndex = Math.min(startDigit + insertedDigits.length(), newDigits.length());
            int newCaret = posFromDigitIndex(caretDigitIndex);
            if (caretDigitIndex == newDigits.length()) newCaret = formatted.length();
            newCaret = clamp(newCaret, 0, formatted.length());

            change.setRange(0, controlText.length());
            change.setText(formatted);
            change.setCaretPosition(newCaret);
            change.setAnchor(newCaret);
            return change;
        }));
    }

    private void setupExpirationField() {
        expDateField.setTextFormatter(new TextFormatter<>(change -> {
            String controlText = change.getControlText();
            int rs = change.getRangeStart();
            int re = change.getRangeEnd();
            String insertRaw = change.getText();

            String digitsInsert = insertRaw.replaceAll("\\D", "");
            int startDigit = countDigitsBeforeSlash(controlText, rs);
            int endDigit   = countDigitsBeforeSlash(controlText, re);

            String oldDigits = controlText.replaceAll("\\D", "");
            int newLenIfAll = oldDigits.length() - (endDigit - startDigit) + digitsInsert.length();
            if (newLenIfAll > 4) {
                int allow = 4 - (oldDigits.length() - (endDigit - startDigit));
                if (allow < 0) allow = 0;
                if (digitsInsert.length() > allow) digitsInsert = digitsInsert.substring(0, allow);
            }

            StringBuilder newDigits = new StringBuilder();
            newDigits.append(oldDigits, 0, Math.min(startDigit, oldDigits.length()));
            newDigits.append(digitsInsert);
            if (endDigit < oldDigits.length()) newDigits.append(oldDigits.substring(endDigit));

            String mm = newDigits.length() >= 2 ? newDigits.substring(0, 2) : newDigits.toString();
            String yy = newDigits.length() > 2 ? newDigits.substring(2) : "";
            String formatted = yy.isEmpty() ? mm : mm + "/" + yy;

            int caretDigitIndex = Math.min(startDigit + digitsInsert.length(), newDigits.length());
            int newCaret = caretPosInMMYY(caretDigitIndex);
            if (caretDigitIndex == newDigits.length()) newCaret = formatted.length();
            newCaret = clamp(newCaret, 0, formatted.length());

            change.setRange(0, controlText.length());
            change.setText(formatted);
            change.setCaretPosition(newCaret);
            change.setAnchor(newCaret);
            return change;
        }));
    }

    private void setupCVVField() {
        cvvField.setTextFormatter(new TextFormatter<>(c -> {
            String t = c.getControlNewText().replaceAll("\\D", "");
            if (t.length() > 3) t = t.substring(0, 3);
            if (t.equals(c.getControlNewText())) return c;
            c.setRange(0, c.getControlText().length());
            c.setText(t);
            int caret = Math.min(t.length(), t.length());
            c.setCaretPosition(caret);
            c.setAnchor(caret);
            return c;
        }));
    }

    private boolean validateAndAlert() {
        String num = cardNumberField.getText().replaceAll("\\D", "");
        if (!num.matches("\\d{16}")) {
            showAlert("Card number must be exactly 16 digits.");
            return false;
        }

        String cvv = cvvField.getText();
        if (!cvv.matches("\\d{3}")) {
            showAlert("Security code (CVV) must be exactly 3 digits.");
            return false;
        }

        String exp = expDateField.getText();
        if (!exp.matches("\\d{2}/\\d{2}")) {
            showAlert("Expiration must be in MM/YY format.");
            return false;
        }
        int mm = Integer.parseInt(exp.substring(0, 2));
        int yy = Integer.parseInt(exp.substring(3, 5));
        if (mm < 1 || mm > 12) {
            showAlert("Expiration month must be between 01 and 12.");
            return false;
        }
        YearMonth expiry = YearMonth.of(2000 + yy, mm);
        YearMonth now = YearMonth.now();
        if (expiry.isBefore(now)) {
            showAlert("This card is expired.");
            return false;
        }
        return true;
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Payment Details");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void closeWindow() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        stage.close();
    }

    public String getCardNumber() { return cardNumberField.getText().replaceAll("\\D", ""); }
    public String getExpDate() { return expDateField.getText(); }
    public String getCVV() { return cvvField.getText(); }

    // helpers
    private static String safe(String[] arr, int i) { return (arr != null && i >= 0 && i < arr.length && arr[i] != null) ? arr[i] : ""; }
    private static String stripNonDigits(String s) { return s.replaceAll("\\D", ""); }
    private static String limitDigits(String s, int n) { return s.length() > n ? s.substring(0, n) : s; }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static int countDigitsBefore(String s, int pos) {
        int c = 0, lim = Math.min(pos, s.length());
        for (int i = 0; i < lim; i++) if (Character.isDigit(s.charAt(i))) c++;
        return c;
    }
    private static String groupEvery4(String digits) {
        StringBuilder sb = new StringBuilder(digits.length() + digits.length() / 4);
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append(' ');
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }
    private static int posFromDigitIndex(int digitIndex) { return digitIndex + (digitIndex / 4); }
    private static int countDigitsBeforeSlash(String s, int pos) {
        int c = 0, lim = Math.min(pos, s.length());
        for (int i = 0; i < lim; i++) if (Character.isDigit(s.charAt(i))) c++;
        return c;
    }
    private static int caretPosInMMYY(int digitIndex) { return (digitIndex <= 2) ? digitIndex : digitIndex + 1; }
}

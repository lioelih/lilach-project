package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

public class PaymentController {

    @FXML private TextField idNumberField;
    @FXML private TextField cardNumberField;
    @FXML private TextField expDateField;
    @FXML private TextField cvvField;
    @FXML private Button confirmButton;
    @FXML private Button cancelButton;

    private Runnable onSuccess;

    public void setOnSuccess(Runnable onSuccess) { this.onSuccess = onSuccess; }

    @FXML
    public void initialize() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        setupIdField();
        setupCardNumberField();
        setupExpirationField();
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
        String action = msg.getAction();
        if (!( "PAYMENT_PREFILL".equals(action) || "PAYMENT_PREFILL_OK".equals(action) )) return;

        Object dataObj = msg.getData();
        if (!(dataObj instanceof String[] arr)) return;

        Platform.runLater(() -> {
            String id  = safe(arr, 0);
            String cardDigits = stripNonDigits(safe(arr, 1));
            String expDigits  = stripNonDigits(safe(arr, 2));
            String cvvDigits  = stripNonDigits(safe(arr, 3));

            idNumberField.setText(limitDigits(id, 9));
            cardNumberField.setText(groupEvery4(limitDigits(cardDigits, 16)));

            if (!expDigits.isEmpty()) {
                expDigits = limitDigits(expDigits, 4);
                expDateField.setText(expDigits.length() <= 2
                        ? expDigits
                        : expDigits.substring(0, 2) + "/" + expDigits.substring(2));
            } else {
                expDateField.clear();
            }

            cvvField.setText(limitDigits(cvvDigits, 4));
        });
    }

    private void setupIdField() {
        idNumberField.setTextFormatter(new TextFormatter<>(c -> {
            String t = c.getControlNewText().replaceAll("\\D", "");
            if (t.length() > 9) t = t.substring(0, 9);
            if (t.equals(c.getControlNewText())) return c;
            c.setRange(0, c.getControlText().length());
            c.setText(t);
            int caret = Math.min(t.length(), t.length());
            c.setCaretPosition(caret);
            c.setAnchor(caret);
            return c;
        }));
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
            if (caretDigitIndex == newDigits.length()) {
                newCaret = formatted.length(); // allow caret after the last char
            }
            newCaret = clamp(newCaret, 0, formatted.length());


            change.setRange(0, controlText.length());
            change.setText(formatted);
            change.setCaretPosition(newCaret);           // clamp avoids IndexOutOfBounds
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
            if (caretDigitIndex == newDigits.length()) {
                newCaret = formatted.length(); // allow caret after the last char
            }
            newCaret = clamp(newCaret, 0, formatted.length());

            change.setRange(0, controlText.length());
            change.setText(formatted);
            change.setCaretPosition(newCaret);           // clamp avoids IndexOutOfBounds
            change.setAnchor(newCaret);
            return change;
        }));
    }

    private void setupCVVField() {
        cvvField.setTextFormatter(new TextFormatter<>(c -> {
            String t = c.getControlNewText().replaceAll("\\D", "");
            if (t.length() > 4) t = t.substring(0, 4);
            if (t.equals(c.getControlNewText())) return c;
            c.setRange(0, c.getControlText().length());
            c.setText(t);
            int caret = Math.min(t.length(), t.length());
            c.setCaretPosition(caret);
            c.setAnchor(caret);
            return c;
        }));
    }

    private boolean validateFields() {
        if (!idNumberField.getText().matches("\\d{9}")) return false;
        if (!cardNumberField.getText().replace(" ", "").matches("\\d{16}")) return false;
        if (!expDateField.getText().matches("\\d{2}/\\d{2}")) return false;
        if (!cvvField.getText().matches("\\d{3,4}")) return false;
        return true;
    }

    private void closeWindow() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        stage.close();
    }

    public String getIdNumber() { return idNumberField.getText(); }
    public String getCardNumber() { return cardNumberField.getText().replaceAll("\\D", ""); }
    public String getExpDate() { return expDateField.getText(); }
    public String getCVV() { return cvvField.getText(); }

    // --- helpers ---
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

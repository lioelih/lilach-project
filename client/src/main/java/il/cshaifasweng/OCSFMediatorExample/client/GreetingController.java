package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.function.BiConsumer;

/*
 * greeting controller:
 * - lets the user type a greeting and choose a background color
 * - enforces a 200-character limit with a live counter
 * - calls back with the text and hex color when saved
 */
public class GreetingController {

    @FXML private TextArea greetingTextArea;
    @FXML private ColorPicker colorPicker;
    @FXML private Label charCountLabel;
    @FXML private Button saveGreetingBtn;
    @FXML private Button cancelGreetingBtn;
    @FXML private Region rootPane;

    private BiConsumer<String, String> onSave;

    public void init(String initialText, String initialHex, BiConsumer<String, String> onSave) {
        this.onSave = onSave;

        // populate text
        if (initialText != null) {
            greetingTextArea.setText(initialText);
        }

        // populate color picker
        if (initialHex != null) {
            try {
                colorPicker.setValue(Color.web(initialHex));
            } catch (IllegalArgumentException e) {
                colorPicker.setValue(Color.WHITE);
            }
        } else {
            colorPicker.setValue(Color.WHITE);
        }

        // update preview and character count
        updateBackground();
        updateCharCount();

        // listeners
        greetingTextArea.textProperty().addListener((obs, old, nw) -> updateCharCount());
        colorPicker.valueProperty().addListener((obs, old, nw) -> updateBackground());

        // button handlers
        cancelGreetingBtn.setOnAction(e -> closeWindow());
        saveGreetingBtn.setOnAction(e -> {
            String text = greetingTextArea.getText();
            if (text.length() > 200) {
                // shouldn't happen—save button disabled—but just in case
                return;
            }
            String hex = toHex(colorPicker.getValue());
            if (onSave != null) onSave.accept(text, hex);
            closeWindow();
        });
    }

    @FXML
    private void initialize() {
        // disable save if over 200 chars
        greetingTextArea.textProperty().addListener((obs, old, nw) -> {
            saveGreetingBtn.setDisable(nw.length() > 200);
        });
    }

    // update the greyed-out remaining-chars label
    private void updateCharCount() {
        int remaining = 200 - greetingTextArea.getText().length();
        if (remaining < 150) {
            charCountLabel.setText(remaining + " chars remaining");
            charCountLabel.setVisible(true);
        } else {
            charCountLabel.setVisible(false);
        }
    }

    // paint the popup background to preview the chosen color
    private void updateBackground() {
        Color c = colorPicker.getValue();
        rootPane.setStyle(String.format("-fx-background-color: %s; -fx-border-color: #ccc;", toHex(c)));
    }

    // close this window
    private void closeWindow() {
        Stage st = (Stage) greetingTextArea.getScene().getWindow();
        st.close();
    }

    // helper: convert color to "#rrggbb"
    private String toHex(Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}

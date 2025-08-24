package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.time.LocalTime;
import java.util.ResourceBundle;

/*
 * contact controller:
 * - shows store status (open/closed) based on local time (08:00–20:00)
 * - loads the app logo
 * - provides quick navigation to home and catalog screens
 * - opens the default mail client with a prefilled support address, with a fallback alert
 */
public class ContactController implements Initializable {

    // ui elements
    @FXML private ImageView logoImage;
    @FXML private Label statusLabel;
    @FXML private Button emailButton;

    // navigation: go to home screen
    @FXML
    private void goHome() {
        SceneController.switchScene("home");
    }

    // navigation: go to catalog screen
    @FXML
    private void goCatalog() {
        SceneController.switchScene("catalog");
    }

    // initialize ui: logo, status, and email action
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));

        boolean open = isOpenNow();
        applyStatus(open);

        if (emailButton != null) {
            emailButton.setOnAction(e -> {
                try {
                    String uri = "mailto:lilachstoresupp@gmail.com?subject=Support%20Request";
                    java.awt.Desktop.getDesktop().mail(new java.net.URI(uri));
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.INFORMATION, "Email: lilachstoresupp@gmail.com").showAndWait();
                }
            });
        }
    }

    // business hours check: open 08:00–20:00 local time
    private boolean isOpenNow() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(20, 0));
    }

    // update status label text and styling
    private void applyStatus(boolean open) {
        if (statusLabel == null) return;
        statusLabel.setText(open ? "OPEN NOW" : "CLOSED");
        statusLabel.setStyle(open
                ? "-fx-background-color:#e6ffed;-fx-text-fill:#137333;-fx-padding:4 10;-fx-background-radius:999;-fx-font-weight:bold;"
                : "-fx-background-color:#fdecea;-fx-text-fill:#a61b1b;-fx-padding:4 10;-fx-background-radius:999;-fx-font-weight:bold;");
    }
}

package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

public class ContactController implements Initializable {

    @FXML
    private ImageView logoImage;

    @FXML
    private void goHome() {
        SceneController.switchScene("home");
    }

    @FXML
    private void goCatalog() {
        SceneController.switchScene("catalog");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Adjust the path according to your actual structure
        Image logo = new Image(getClass().getResourceAsStream("/image/logo.png"));
        logoImage.setImage(logo);
    }
}

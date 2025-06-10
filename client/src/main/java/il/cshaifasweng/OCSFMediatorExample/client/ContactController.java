package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;

public class ContactController {

    @FXML
    private void goHome() {
        SceneController.switchScene("home");
    }

    @FXML
    private void goCatalog() {
        SceneController.switchScene("catalog");
    }
}

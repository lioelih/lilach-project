package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.io.IOException;

public class HomeController {

    @FXML public Label welcomeLabel;
    @FXML private Button logoutButton;
    @FXML private Button catalogButton;
    @FXML private Button contactButton;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button vipButton;

    @FXML
    public void initialize() {
        boolean loggedIn = SceneController.loggedUsername != null;

        catalogButton.setOnAction(e -> SceneController.switchScene("catalog"));
        contactButton.setOnAction(e -> SceneController.switchScene("contact"));
        loginButton.setOnAction(e -> SceneController.switchScene("login"));
        registerButton.setOnAction(e -> SceneController.switchScene("register"));
        vipButton.setOnAction(e -> SceneController.switchScene("vip"));

        if (loggedIn) {
            loginButton.setVisible(false);
            registerButton.setVisible(false);
            logoutButton.setVisible(true);
            welcomeLabel.setText("Welcome, " + SceneController.loggedUsername);
        } else {
            logoutButton.setVisible(false);
            welcomeLabel.setText("");
        }

        logoutButton.setOnAction(e -> {
            try {
                SimpleClient.getClient().sendToServer(new Msg("LOGOUT", SceneController.loggedUsername));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            SceneController.loggedUsername = null;
            SceneController.switchScene("home");
        });
    }
}

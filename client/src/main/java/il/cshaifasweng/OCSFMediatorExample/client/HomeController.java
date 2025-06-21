package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.LogoutRequest;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class HomeController {

    public Label welcomeLabel;
    public Button logoutButton;
    @FXML
    private Button catalogButton;

    @FXML
    private Button contactButton;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Button vipButton;


    @FXML
    public void initialize()
    { // Just front page stuff :D
        welcomeLabel.setText("");
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
                SimpleClient.getClient().sendToServer(new LogoutRequest(SceneController.loggedUsername));
            } catch (IOException ex) {
                ex.printStackTrace();
            }


            SceneController.loggedUsername = null;
            SceneController.switchScene("home");

        });
    }
}

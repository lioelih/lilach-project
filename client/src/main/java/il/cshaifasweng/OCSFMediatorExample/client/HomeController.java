package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class HomeController {

    @FXML public Label welcomeLabel;
    @FXML private Button usersButton;
    @FXML private Button logoutButton;
    @FXML private Button catalogButton;
    @FXML private Button contactButton;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button vipButton;
    @FXML private Button ordersButton;
    @FXML private ImageView logoImage;
    @FXML
    public void initialize() {
        boolean loggedIn  = SceneController.loggedUsername != null;
        boolean canWorker = SceneController.hasPermission(SceneController.Role.WORKER);

        // Home nav
        catalogButton .setOnAction(e -> SceneController.switchScene("catalog"));
        contactButton .setOnAction(e -> SceneController.switchScene("contact"));
        vipButton     .setOnAction(e -> SceneController.switchScene("vip"));

        // Login / Register
        loginButton   .setVisible(!loggedIn);
        loginButton   .setManaged(!loggedIn);
        registerButton.setVisible(!loggedIn);
        registerButton.setManaged(!loggedIn);
        loginButton   .setOnAction(e -> SceneController.switchScene("login"));
        registerButton.setOnAction(e -> SceneController.switchScene("register"));

        // Logout / Orders
        logoutButton  .setVisible(loggedIn);
        logoutButton  .setManaged(loggedIn);
        ordersButton  .setVisible(loggedIn);
        ordersButton  .setManaged(loggedIn);
        ordersButton  .setOnAction(e -> SceneController.switchScene("orders"));
        logoutButton .setOnAction(e -> {
            try {
                SimpleClient.getClient().sendToServer(new Msg("LOGOUT", SceneController.loggedUsername));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            SceneController.loggedUsername = null;
            SceneController.setCurrentUserRole(SceneController.Role.USER);
            SceneController.switchScene("home");
        });

        // Users (WORKER+)
        usersButton   .setVisible(canWorker);
        usersButton   .setManaged(canWorker);
        usersButton   .setOnAction(e -> SceneController.switchScene("users"));

        // welcome
        if (loggedIn) {
            welcomeLabel.setText("Welcome, " + SceneController.loggedUsername);
        } else {
            welcomeLabel.setText("");
        }

        // logo
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));
    }
}

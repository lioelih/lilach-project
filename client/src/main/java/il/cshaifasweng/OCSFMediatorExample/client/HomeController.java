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
    @FXML public Button ordersButton;
    @FXML private Button logoutButton;
    @FXML private Button catalogButton;
    @FXML private Button contactButton;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button vipButton;
    @FXML private ImageView logoImage;
    @FXML
    public void initialize() {
        boolean loggedIn = SceneController.loggedUsername != null;

        catalogButton.setOnAction(e -> SceneController.switchScene("catalog"));
        contactButton.setOnAction(e -> SceneController.switchScene("contact"));
        loginButton.setOnAction(e -> SceneController.switchScene("login"));
        registerButton.setOnAction(e -> SceneController.switchScene("register"));
        vipButton.setOnAction(e -> SceneController.switchScene("vip"));
        ordersButton.setOnAction(e -> openOrdersWindow());


        if (loggedIn) {
            loginButton.setVisible(false);
            registerButton.setVisible(false);
            logoutButton.setVisible(true);
            welcomeLabel.setText("Welcome, " + SceneController.loggedUsername);
            ordersButton.setVisible(true);
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
        Image logo = new Image(getClass().getResourceAsStream("/image/logo.png"));
        logoImage.setImage(logo);
    }

    private void openOrdersWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("orders.fxml"));
            Scene scene = new Scene(loader.load());

            // if the OrdersController needs the username:
            OrdersController oc = loader.getController();

            Stage st = new Stage();
            st.initModality(Modality.NONE);            // separate window
            st.setTitle("Orders");
            st.setScene(scene);
            st.show();                                 // non-modal, so Home stays open
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

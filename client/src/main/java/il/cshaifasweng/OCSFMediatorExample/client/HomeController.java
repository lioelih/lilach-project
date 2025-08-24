package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

/*
 * home controller:
 * - wires up navigation buttons and toggles visibility based on login/role
 * - shows a welcome message when logged in
 * - listens for session-related events and refreshes the ui accordingly
 */
public class HomeController {

    @FXML public Label welcomeLabel;
    @FXML public Button catalogButton2Button;
    @FXML private Button usersButton;
    @FXML private Button logoutButton;
    @FXML private Button catalogButton;
    @FXML private Button contactButton;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button vipButton;
    @FXML private Button ordersButton;
    @FXML public Button vip2Button;
    @FXML private ImageView logoImage;
    @FXML private ImageView storeImage;

    @FXML
    public void initialize() {
        try { EventBus.getDefault().unregister(this); } catch (Exception ignored) {}
        EventBus.getDefault().register(this);

        boolean loggedIn = SceneController.loggedUsername != null;
        boolean canWorker = SceneController.hasPermission(User.Role.WORKER);

        // nav
        catalogButton.setOnAction(e -> SceneController.switchScene("catalog"));
        catalogButton2Button.setOnAction(e -> SceneController.switchScene("catalog"));
        contactButton.setOnAction(e -> SceneController.switchScene("contact"));
        vipButton.setOnAction(e -> SceneController.switchScene("vip"));
        vip2Button.setOnAction(e -> SceneController.switchScene("vip"));
        vip2Button.setVisible(!SceneController.isVIP);
        vip2Button.setManaged(!SceneController.isVIP);

        // auth buttons
        loginButton.setVisible(!loggedIn);
        loginButton.setManaged(!loggedIn);
        registerButton.setVisible(!loggedIn);
        registerButton.setManaged(!loggedIn);
        loginButton.setOnAction(e -> SceneController.switchScene("login"));
        registerButton.setOnAction(e -> SceneController.switchScene("register"));

        // logout / orders
        logoutButton.setVisible(loggedIn);
        logoutButton.setManaged(loggedIn);
        ordersButton.setVisible(loggedIn);
        ordersButton.setManaged(loggedIn);
        ordersButton.setOnAction(e -> SceneController.switchScene("orders"));
        logoutButton.setOnAction(e -> {
            try {
                SimpleClient.getClient().sendToServer(new Msg("LOGOUT", SceneController.loggedUsername));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            SceneController.loggedUsername = null;
            SceneController.isVIP = false;
            SceneController.setCurrentUserRole(User.Role.USER);
            SceneController.switchScene("home");
        });

        // users (worker+)
        usersButton.setVisible(canWorker);
        usersButton.setManaged(canWorker);
        usersButton.setOnAction(e -> SceneController.switchScene("users"));

        // welcome label
        if (loggedIn) {
            welcomeLabel.setText("Welcome, " + SceneController.loggedUsername);
            welcomeLabel.setVisible(true);
        } else {
            welcomeLabel.setText("");
            welcomeLabel.setVisible(false);
        }

        // images
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));
        storeImage.setImage(new javafx.scene.image.Image(
                getClass().getResourceAsStream("/image/rose.png"),
                1100, 500, /* preserveRatio */ false, /* smooth */ true));
    }

    // react to session-related events and account freeze
    @Subscribe
    public void onMsg(Msg msg) {
        switch (msg.getAction()) {
            case "LOCAL_ROLE_VIP_CHANGED":
            case "USER_UPDATED":
            case "VIP_ACTIVATED":
            case "VIP_CANCELLED":
                Platform.runLater(this::refreshUIFromSession);
                break;
            case "ACCOUNT_FROZEN":
                Platform.runLater(() ->
                        SceneController.forceLogoutWithAlert((String) msg.getData())
                );
                break;
        }
    }

    // re-apply visibility and labels from the current session state
    private void refreshUIFromSession() {
        boolean loggedIn = SceneController.loggedUsername != null;
        boolean canWorker = SceneController.hasPermission(User.Role.WORKER);

        loginButton.setVisible(!loggedIn);    loginButton.setManaged(!loggedIn);
        registerButton.setVisible(!loggedIn); registerButton.setManaged(!loggedIn);

        logoutButton.setVisible(loggedIn);    logoutButton.setManaged(loggedIn);
        ordersButton.setVisible(loggedIn);    ordersButton.setManaged(loggedIn);

        usersButton.setVisible(canWorker);    usersButton.setManaged(canWorker);

        boolean showVipCta = !SceneController.isVIP;
        vip2Button.setVisible(showVipCta);    vip2Button.setManaged(showVipCta);

        if (loggedIn) {
            welcomeLabel.setText("Welcome, " + SceneController.loggedUsername);
            welcomeLabel.setVisible(true);
        } else {
            welcomeLabel.setText("");
            welcomeLabel.setVisible(false);
        }
    }
}

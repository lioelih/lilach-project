package il.cshaifasweng.OCSFMediatorExample.client;

import Events.LoginEvent;
import Events.RegisterEvent;
import Events.WarningEvent;
import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import il.cshaifasweng.LoginUserDTO;
import java.io.IOException;

/*
 * app entry point:
 * - asks for server address, opens the socket, and registers to the event bus
 * - sets up the main stage via scenecontroller and shows the home scene
 * - handles login/register/warning events from the server
 * - ensures clean logout and socket close on exit
 */
public class App extends Application {

    private static Scene scene;
    private SimpleClient client;

    @Override
    public void start(Stage stage) throws IOException {
        EventBus.getDefault().register(this); // register to event bus at startup

        String[] connectionInfo = showConnectionDialog(); // prompt for ip:port
        if (connectionInfo == null) { // no valid input -> exit
            System.exit(1);
            return;
        }

        client = SimpleClient.getClient();
        client.setPort(Integer.parseInt(connectionInfo[1]));
        client.setHost(connectionInfo[0]);
        client.openConnection();
        SimpleClient.setClient(client);
        client.sendToServer("add client"); // subscribe this client on the server

        SceneController.setMainStage(stage);
        SceneController.switchScene("home"); // connected successfully -> go home
        stage.setMaximized(true);

        // on window close, log out (if needed) and unregister the client
        stage.setOnCloseRequest(e -> {
            try {
                String u = SceneController.loggedUsername;
                if (u != null && !u.isBlank()) {
                    SimpleClient.logoutAndClose(u);
                    SceneController.loggedUsername = null;
                }
                client.sendToServer("remove client");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    public void stop() throws Exception {
        EventBus.getDefault().unregister(this); // stop receiving events
        try {
            String u = SceneController.loggedUsername;
            if (u != null && !u.isBlank()) {
                SimpleClient.logoutAndClose(u); // graceful logout if still logged in
                SceneController.loggedUsername = null;
            }
        } catch (Exception ignored) {}
        try { client.sendToServer("remove client"); } catch (IOException ignored) {}
        try { client.closeConnection(); } catch (IOException ignored) {}
        super.stop();
    }

    public static void main(String[] args) {
        launch(); // launch javafx app
    }

    // simple input dialog for "ip:port"; returns split pair or null
    private String[] showConnectionDialog() {
        TextInputDialog dialog = new TextInputDialog("localhost:3000");
        dialog.setTitle("Connect to Server");
        dialog.setHeaderText("Enter Server IP and Port (e.g. 127.0.0.1:3000)");
        dialog.setContentText("Format: ip:port");

        return dialog.showAndWait()
                .map(input -> input.split(":"))
                .filter(arr -> arr.length == 2)
                .orElse(null);
    }

    // reacts to login response: on success, store session info and go home; on failure, show error
    @Subscribe
    public void onLoginResponse(LoginEvent event) {
        Msg msg = event.getMsg();
        Platform.runLater(() -> {
            if ("LOGIN_SUCCESS".equals(msg.getAction())) {
                LoginUserDTO u = (LoginUserDTO) msg.getData();
                SceneController.loggedUsername = u.getUsername();
                SceneController.setCurrentUserRole(User.Role.valueOf(u.getRole()));
                SceneController.isVIP = u.isVIP();
                SceneController.switchScene("home");
            } else {
                String message = String.valueOf(msg.getData());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Error");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            }
        });
    }

    // reacts to register response: on success, navigate to login screen
    @Subscribe
    public void onRegisterResponse(RegisterEvent event) {
        Msg msg = event.getMsg();
        Platform.runLater(() -> {
            if (msg.getAction().equals("REGISTER_SUCCESS")) {
                SceneController.switchScene("login");
            }
            // on failure, do nothing here (the form handles validation)
        });
    }

    // shows server warnings as alerts
    @Subscribe
    public void onWarning(WarningEvent event) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText(event.getWarning().getMessage());
            alert.showAndWait();
        });
    }
}

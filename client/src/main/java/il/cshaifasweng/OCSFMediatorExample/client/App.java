package il.cshaifasweng.OCSFMediatorExample.client;

import Events.CatalogEvent;
import Events.LoginEvent;
import Events.RegisterEvent;
import Events.WarningEvent;
import il.cshaifasweng.Msg;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;

public class App extends Application {

    private static Scene scene;
    private SimpleClient client;

    @Override
    public void start(Stage stage) throws IOException {
        EventBus.getDefault().register(this); // register first event bus on start

        String[] connectionInfo = showConnectionDialog();
        if (connectionInfo == null) { // if no info was put into connection
            System.exit(1);
            return;
        }

        client = SimpleClient.getClient();
        client.setPort(Integer.parseInt(connectionInfo[1]));
        client.setHost(connectionInfo[0]);
        client.openConnection();
        SimpleClient.setClient(client);
        client.sendToServer("add client");

        SceneController.setMainStage(stage);
        SceneController.switchScene("home"); // if we connected, then we switch to home
        stage.setMaximized(true);


        stage.setOnCloseRequest(e -> {
            try {
                if (SceneController.loggedUsername != null) {
                    client.sendToServer(new Msg("LOGOUT", SceneController.loggedUsername));
                } // must remember to logout once user finishes his session
                client.sendToServer("remove client");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    public void stop() throws Exception {
        EventBus.getDefault().unregister(this);
        client.sendToServer("remove client"); // close / stop
        client.closeConnection();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    } // calls launch function

    private String[] showConnectionDialog() {
        TextInputDialog dialog = new TextInputDialog("localhost:3000");
        dialog.setTitle("Connect to Server");
        dialog.setHeaderText("Enter Server IP and Port (e.g. 127.0.0.1:3000)");
        dialog.setContentText("Format: ip:port"); // we use a textinputdialog to get the ip and start up the home page

        return dialog.showAndWait()
                .map(input -> input.split(":"))
                .filter(arr -> arr.length == 2)
                .orElse(null);
    }

    @Subscribe
    public void onLoginResponse(LoginEvent event) {
        Msg msg = event.getMsg();
        Platform.runLater(() -> {
            if ("LOGIN_SUCCESS".equals(msg.getAction())) { // if login is successful, we keep the important info such as role and name

                String[] payload = (String[]) msg.getData();
                String username = payload[0];
                String roleName = payload[1];

                // save both into SceneController
                SceneController.loggedUsername     = username;
                SceneController.setCurrentUserRole(
                        SceneController.Role.valueOf(roleName)
                );

                SceneController.switchScene("home");
            } else { // otherwise we send out an error
                String message = msg.getData().toString();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Error");
                alert.setHeaderText(null);
                alert.setContentText(
                        message.equals("Account is inactive")
                                ? "This account is inactive. Please contact support."
                                : "Invalid credentials or user does not exist."
                );
                alert.showAndWait();
            }
        });
    }


    @Subscribe
    public void onRegisterResponse(RegisterEvent event) {
        Msg msg = event.getMsg();
        Platform.runLater(() -> {
            if (msg.getAction().equals("REGISTER_SUCCESS")) {
                SceneController.switchScene("login"); // switches to login once user was created
            }
            // Do nothing if failed
        });
    }
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



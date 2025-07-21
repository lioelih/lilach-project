package il.cshaifasweng.OCSFMediatorExample.client;

import Events.LoginEvent;
import Events.RegisterEvent;
import Events.WarningEvent;
import il.cshaifasweng.Msg;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;
    private SimpleClient client;

    @Override
    public void start(Stage stage) throws IOException {
        EventBus.getDefault().register(this);

        String[] connectionInfo = showConnectionDialog();
        if (connectionInfo == null) {
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
        SceneController.switchScene("home");
        stage.setMaximized(true);


        stage.setOnCloseRequest(e -> {
            try {
                if (SceneController.loggedUsername != null) {
                    client.sendToServer(new Msg("LOGOUT", SceneController.loggedUsername));
                }
                client.sendToServer("remove client");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    public void stop() throws Exception {
        EventBus.getDefault().unregister(this);
        client.sendToServer("remove client");
        client.closeConnection();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }

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

    @Subscribe
    public void onLoginResponse(LoginEvent event) {
        Msg msg = event.getMsg();
        Platform.runLater(() -> {
            if (msg.getAction().equals("LOGIN_SUCCESS")) {
                SceneController.loggedUsername = (String) msg.getData();
                SceneController.switchScene("home");
            } else {
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
                SceneController.switchScene("login");
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



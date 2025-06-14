package il.cshaifasweng.OCSFMediatorExample.client;
import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import java.util.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private SimpleClient client;

    @Override
    public void start(Stage stage) throws IOException {
        EventBus.getDefault().register(this);
        String[] connectionInfo = showConnectionDialog(); // Opens a window for us from showConnectionDialog to input the server IP
        client = SimpleClient.getClient();
        client.setPort(Integer.parseInt(connectionInfo[1]));
        client.setHost(connectionInfo[0]);
        client.openConnection();

        SceneController.setMainStage(stage);
        SceneController.switchScene("home"); // Sets our scene to Home after opening the connection
        stage.setOnCloseRequest(e -> {
            try (Connection conn = DBUtil.getConnection()) {
                if (SceneController.loggedUsername != null) {
                    PreparedStatement stmt = conn.prepareStatement("UPDATE users SET is_logged_in = FALSE WHERE username = ?");
                    stmt.setString(1, SceneController.loggedUsername);
                    stmt.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    @Subscribe // Opens Catalog upon request
    public void onCatalogReceived(List<Product> catalog) {
        Platform.runLater(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("catalog.fxml")); // Opens catalog
            try {
                Parent root = loader.load();
                CatalogController controller = loader.getController();
                controller.updateCatalog(catalog);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }



    @Override
    public void stop() throws Exception {
        // TODO Auto-generated method stub
        EventBus.getDefault().unregister(this);
        client.sendToServer("remove client");
        client.closeConnection();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }

    private String[] showConnectionDialog() { // Our connection input window
        TextInputDialog dialog = new TextInputDialog("localhost:3000");
        dialog.setTitle("Connect to Server");
        dialog.setHeaderText("Enter Server IP and Port (e.g. 3000)");
        dialog.setContentText("Format: ip:port");
        // This recognizes the port as well as ip in a unique format : ip:port, that way it keeps things easier

        return dialog.showAndWait()
                .map(input -> input.split(":"))
                .filter(arr -> arr.length == 2)
                .orElse(null);
    }
}




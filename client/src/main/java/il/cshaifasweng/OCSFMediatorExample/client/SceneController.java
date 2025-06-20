package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneController {
    public static Stage mainStage;
    public static String loggedUsername = null;

    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    public static void switchScene(String fxmlName) {
        try { // Will allow the user to switch scenes between different panels
            FXMLLoader loader = new FXMLLoader(SceneController.class.getResource(fxmlName + ".fxml"));
            Parent root = loader.load();
            mainStage.setScene(new Scene(root)); // <- this crashes if mainStage == null
            mainStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

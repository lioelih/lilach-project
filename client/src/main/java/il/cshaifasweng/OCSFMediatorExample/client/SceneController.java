package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import java.io.IOException;

public class SceneController {
    public static Stage mainStage;
    public static String loggedUsername = null;

    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    public static void switchScene(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneController.class.getResource(fxmlName + ".fxml"));
            Parent root = loader.load();

            // Get usable screen bounds (excludes task-bar)
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();

            Scene scene = new Scene(root, visualBounds.getWidth(), visualBounds.getHeight());
            mainStage.setScene(scene);
            mainStage.setTitle("Lilach Store");
            mainStage.setX(visualBounds.getMinX());
            mainStage.setY(visualBounds.getMinY());
            mainStage.setWidth(visualBounds.getWidth());
            mainStage.setHeight(visualBounds.getHeight());
            mainStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

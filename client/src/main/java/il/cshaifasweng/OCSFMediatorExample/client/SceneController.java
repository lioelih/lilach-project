package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.User;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Window;
import java.io.IOException;

public class SceneController {
    public static Stage mainStage;
    public static String loggedUsername = null;
    public static boolean isVIP = false;

    private static User.Role currentUserRole;

    public static void setCurrentUserRole(User.Role role) {
        currentUserRole = role;
    }

    public static User.Role getCurrentUserRole() {
        return currentUserRole;
    }

    public static boolean hasPermission(User.Role required) {
        if (currentUserRole == null) return false;
        return currentUserRole.ordinal() >= required.ordinal();
    }

    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    public static void clearPermissions() {
        currentUserRole = null;
        isVIP = false;
    }

    public static void forceLogoutWithAlert(String text) {
        Platform.runLater(() -> {
            try {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Account Frozen");
                a.setHeaderText(null);
                a.setContentText((text != null && !text.isBlank())
                        ? text
                        : "Your account has been frozen. Contact support for more information.");
                a.showAndWait();
            } catch (Exception ignored) {}

            loggedUsername = null;
            clearPermissions();

            for (Window w : Window.getWindows()) {
                if (w instanceof Stage s && s.isShowing()) s.close();
            }
            switchScene("home");
        });
    }

    public static void switchScene(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneController.class.getResource(fxmlName + ".fxml"));
            Parent root = loader.load();

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

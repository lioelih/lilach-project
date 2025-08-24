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

/*
 * scene controller
 * - centralizes navigation and session state (username, role, vip)
 * - provides helpers for permission checks
 * - provides a single place to force-logout and show the freeze alert
 * - switches scenes by loading fxml and sizing to the screen's visual bounds
 */
public class SceneController {
    public static Stage mainStage;
    public static String loggedUsername = null;
    public static boolean isVIP = false;

    private static User.Role currentUserRole;

    /* set the current user's role for permission checks */
    public static void setCurrentUserRole(User.Role role) {
        currentUserRole = role;
    }

    /* get the current user's role */
    public static User.Role getCurrentUserRole() {
        return currentUserRole;
    }

    /*
     * permission helper:
     * returns true if the current role's ordinal is at least the required role's ordinal
     */
    public static boolean hasPermission(User.Role required) {
        if (currentUserRole == null) return false;
        return currentUserRole.ordinal() >= required.ordinal();
    }

    /* set the primary stage used by the app (called once at startup) */
    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    /* clear session flags (role + vip); does not touch username */
    public static void clearPermissions() {
        currentUserRole = null;
        isVIP = false;
    }

    /*
     * force logout flow used when the server freezes the account:
     * - show a single error alert with an optional server message
     * - clear all session state
     * - close all open windows
     * - return to the home screen
     */
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

    /*
     * scene switcher:
     * loads the requested fxml and sizes the main stage to the primary screen's visual bounds
     */
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

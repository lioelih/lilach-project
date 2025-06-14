package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class HomeController {

    public Label welcomeLabel;
    public Button logoutButton;
    @FXML
    private Button catalogButton;

    @FXML
    private Button contactButton;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    public void initialize()
    { // Just front page stuff :D
        boolean loggedIn = SceneController.loggedUsername != null;
        catalogButton.setOnAction(e -> SceneController.switchScene("catalog"));
        contactButton.setOnAction(e -> SceneController.switchScene("contact"));
        loginButton.setOnAction(e -> SceneController.switchScene("login"));
        registerButton.setOnAction(e -> SceneController.switchScene("register"));

        if (loggedIn) {
            loginButton.setVisible(false);
            registerButton.setVisible(false);
            logoutButton.setVisible(true);
            welcomeLabel.setText("Welcome, " + SceneController.loggedUsername);
        } else {
            logoutButton.setVisible(false);
            welcomeLabel.setText("");
        }

        logoutButton.setOnAction(e -> {
            try (Connection conn = DBUtil.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("UPDATE users SET is_logged_in = FALSE WHERE username = ?");
                stmt.setString(1, SceneController.loggedUsername);
                stmt.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            SceneController.loggedUsername = null;
            SceneController.switchScene("home");

        });
    }
}

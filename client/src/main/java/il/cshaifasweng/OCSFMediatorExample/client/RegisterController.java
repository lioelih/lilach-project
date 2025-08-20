package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import Events.RegisterEvent;

import java.io.IOException;
import java.util.List;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneNumberField;
    @FXML private TextField nameField;
    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Branch> branchComboBox;
    @FXML private Button submitButton;
    @FXML private Button backButton;
    @FXML private ImageView logoImage;

    @FXML
    public void initialize() throws IOException {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        EventBus.getDefault().register(this);

        backButton.setOnAction(e -> {
            EventBus.getDefault().unregister(this);
            SceneController.switchScene("home");
        });

        submitButton.setOnAction(event -> {
            Branch selected = branchComboBox.getValue();
            if (selected == null) {
                showAlert("You must select a branch.");
                return;
            }

            String[] fields = new String[]{
                    usernameField.getText().trim(),      // f[0] username
                    emailField.getText().trim(),         // f[1] email
                    nameField.getText().trim(),          // f[2] full name
                    phoneNumberField.getText().trim(),   // f[3] phone
                    idField.getText().trim(),            // f[4] 9-digit ID
                    passwordField.getText(),             // f[5] password
                    String.valueOf(selected.getBranchId()) // f[6] branchId
            };

            if (!isValidInput(fields)) return;

            try {
                SimpleClient.getClient().sendToServer(new Msg("REGISTER", fields));
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Failed to send registration data to server.");
            }
        });

        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));
        SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null));
    }

    private boolean isValidInput(String[] f) {
        // f[0]=username, f[1]=email, f[2]=fullName, f[3]=phone, f[4]=id, f[5]=password, f[6]=branchId

        // 1) all required
        for (int i = 0; i <= 5; i++) {
            if (f[i].isBlank()) {
                showAlert("All fields are required.");
                return false;
            }
        }

        // 2) username & password: no spaces
        if (f[0].contains(" ") || f[5].contains(" ")) {
            showAlert("Username or password cannot contain spaces.");
            return false;
        }

        // 3) valid e‑mail
        if (!f[1].matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,}$")) {
            showAlert("Invalid email format.");
            return false;
        }

        // 4) full name: at least two parts separated by a space, each at least 2 letters
        if (!f[2].matches("^[A-Za-z]{2,}\\s+[A-Za-z]{2,}$")) {
            showAlert("Full Name must include first and last name, each at least 2 letters long.");
            return false;
        }

        // 5) phone: exactly 10 digits, must start with 05
        if (!f[3].matches("^05\\d{8}$")) {
            showAlert("Phone number must be 10 digits starting with 05.");
            return false;
        }

        // 6) id: exactly 9 digits
        if (!f[4].matches("^\\d{9}$")) {
            showAlert("ID must be exactly 9 digits.");
            return false;
        }

        // 7) password length
        if (f[5].length() < 4 || f[5].length() > 16) {
            showAlert("Password must be 4–16 characters.");
            return false;
        }

        return true;
    }

    @Subscribe
    public void onBranchesReceived(Msg message) {
        if ("BRANCHES_OK".equals(message.getAction())) {
            List<Branch> branches = (List<Branch>) message.getData();
            Platform.runLater(() -> {
                branchComboBox.getItems().clear();
                branchComboBox.getItems().addAll(branches);
            });
        }
    }

    @Subscribe
    public void onRegisterEvent(RegisterEvent event) {
        Msg msg = event.getMsg();
        if ("REGISTER_SUCCESS".equals(msg.getAction())) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Registration Successful");
                alert.setHeaderText(null);
                alert.setContentText("Registration successful! You can now log in.");
                alert.showAndWait();
                SceneController.switchScene("login");
            });
        } else if ("REGISTER_FAILED".equals(msg.getAction())) {
            String errorMsg = (String) msg.getData();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Registration Failed");
                alert.setHeaderText(null);
                alert.setContentText("Registration failed: " + errorMsg);
                alert.showAndWait();
            });
        }
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void onClose() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }
}

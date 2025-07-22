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
import Events.RegisterEvent; // make sure this is imported

import java.io.IOException;
import java.util.List;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneNumberField;
    @FXML private TextField nameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Branch> branchComboBox;
    @FXML private Button submitButton;
    @FXML private Button backButton;
    @FXML private ImageView logoImage;

    @FXML
    public void initialize() throws IOException {
        EventBus.getDefault().unregister(this);
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

            System.out.println("[Client] Register button clicked.");
            System.out.println("[Client] Selected branch: " + selected.getName() + ", ID: " + selected.getBranchId());

            String[] fields = new String[]{
                    usernameField.getText().trim(),
                    emailField.getText().trim(),
                    nameField.getText().trim(),        // fullName first
                    phoneNumberField.getText().trim(), // phone second
                    passwordField.getText(),
                    String.valueOf(selected.getBranchId())
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

        System.out.println("[Client] Requesting branch list from server...");
        SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null));
    }

    private boolean isValidInput(String[] f) {
        // f[0]=username, f[1]=email, f[2]=fullName, f[3]=phone, f[4]=password, f[5]=branchId

        // 1) all required
        for (int i = 0; i < 5; i++) {
            if (f[i].isBlank()) {
                showAlert("All fields are required.");
                return false;
            }
        }

        // 2) username & password: no spaces
        if (f[0].contains(" ") || f[4].contains(" ")) {
            showAlert("Username or password cannot contain spaces.");
            return false;
        }

        // 3) valid e‑mail
        if (!f[1].matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showAlert("Invalid email format.");
            return false;
        }

        // 4) full name: at least two parts separated by a space
        if (!f[2].matches("^[^\\s]+\\s+[^\\s]+$")) {
            showAlert("Full Name must include first and last name separated by a space.");
            return false;
        }

        // 5) phone: exactly 10 digits, must start with 05
        if (!f[3].matches("^05\\d{8}$")) {
            showAlert("Phone number must be 10 digits starting with 05.");
            return false;
        }

        // 6) password length
        if (f[4].length() < 4 || f[4].length() > 16) {
            showAlert("Password must be 4–16 characters.");
            return false;
        }

        return true;
    }


    @Subscribe
    public void onBranchesReceived(Msg message) {
        if ("BRANCHES_OK".equals(message.getAction())) {
            List<Branch> branches = (List<Branch>) message.getData();

            System.out.println("[Client] Received branches from server:");
            for (Branch b : branches) {
                System.out.println("[Client] Branch: " + b.getName() + ", ID: " + b.getBranchId());
            }

            Platform.runLater(() -> {
                branchComboBox.getItems().clear();
                branchComboBox.getItems().addAll(branches);
            });
        }
    }

    // THIS IS THE NEW ADDITION: Listen for server register response!
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
                // Optionally, redirect to login or home scene after success
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

    // Optional: Call this on controller close or scene change to unregister EventBus
    public void onClose() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }
}

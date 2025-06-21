package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

public class VIPController {

    @FXML private Button subscribeButton;
    @FXML private Button cancelVipButton;
    @FXML private Button backButton;

    private User user;

    @FXML
    public void initialize() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        backButton.setOnAction(e -> SceneController.switchScene("home"));
        subscribeButton.setOnAction(e -> openPaymentWindow());
        if (SceneController.loggedUsername == null) {
            subscribeButton.setDisable(true);
            cancelVipButton.setDisable(true);
            return;
        }
        try {
            SimpleClient.getClient().sendToServer(new FetchUserRequest(SceneController.loggedUsername));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void handleUserResponse(FetchUserResponse response) {
        System.out.println("Received user response: " + response);
        Platform.runLater(() -> {
            this.user = response.getUser();

            if (user != null && user.isVIP() && !user.getVipCanceled()) {
                subscribeButton.setDisable(true);
                cancelVipButton.setVisible(true);
            } else {
                subscribeButton.setDisable(false);
                cancelVipButton.setVisible(false);
            }

            subscribeButton.setOnAction(e -> openPaymentWindow());

            cancelVipButton.setOnAction(e -> {
                try {
                    SimpleClient.getClient().sendToServer(new CancelVIPRequest(user.getUsername()));
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("VIP Cancelled");
                    alert.setHeaderText(null);
                    alert.setContentText("VIP will be deactivated after your current period ends.");
                    alert.showAndWait();
                    cancelVipButton.setDisable(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            backButton.setOnAction(e -> SceneController.switchScene("home"));
        });
    }

    private void openPaymentWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/Payment.fxml"));
            Parent root = loader.load();

            PaymentController controller = loader.getController();

            controller.setOnSuccess(() -> {
                String idNumber = controller.getIdNumber();
                String cardNumber = controller.getCardNumber();
                String expDate = controller.getExpDate();
                String cvv = controller.getCVV();

                try {
                    // First: update payment info
                    PaymentInfoRequest infoRequest = new PaymentInfoRequest(
                            SceneController.loggedUsername,
                            idNumber,
                            cardNumber,
                            expDate,
                            cvv,
                            false // false: not marking this as VIP activation
                    );
                    SimpleClient.getClient().sendToServer(infoRequest);

                    // Then: send VIP activation request
                    VIPPaymentRequest vipRequest = new VIPPaymentRequest(
                            SceneController.loggedUsername,
                            idNumber,
                            cardNumber,
                            expDate,
                            cvv
                    );
                    SimpleClient.getClient().sendToServer(vipRequest);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("VIP Status Added!");
                    alert.setHeaderText(null);
                    alert.setContentText("Thank you for subscribing to VIP!");
                    alert.showAndWait();
                    SceneController.switchScene("home");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Stage stage = new Stage();
            stage.setTitle("VIP Payment");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Optional: unregister from EventBus if this controller gets destroyed
    public void onClose() {
        EventBus.getDefault().unregister(this);
    }
}
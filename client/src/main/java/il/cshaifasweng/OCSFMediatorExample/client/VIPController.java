package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

public class VIPController  {

    @FXML private Button subscribeButton;
    @FXML private Button cancelVipButton;
    @FXML private Button backButton;
    @FXML private ImageView logoImage;
    private User user;

    @FXML
    public void initialize() {
        Image logo = new Image(getClass().getResourceAsStream("/image/logo.png"));
        logoImage.setImage(logo);
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        backButton.setOnAction(e -> {
            EventBus.getDefault().unregister(this);
            SceneController.switchScene("home");
            });
        subscribeButton.setOnAction(e -> openPaymentWindow());

        if (SceneController.loggedUsername == null) {
            subscribeButton.setDisable(true);
            cancelVipButton.setDisable(true);
            return;
        }

        try {
            SimpleClient.getClient().sendToServer(new Msg("FETCH_USER", SceneController.loggedUsername));
        } catch (IOException e) {
            e.printStackTrace();
        }
        cancelVipButton.setOnAction(e -> {
            try {
                SimpleClient.getClient().sendToServer(new Msg("CANCEL_VIP", SceneController.loggedUsername));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    @Subscribe
    public void handleUserFetched(Msg msg) {
        if (!"FETCH_USER".equals(msg.getAction())) return;

        this.user = (User) msg.getData();

        Platform.runLater(() -> {
            boolean isVipAndActive = user != null && user.isVIP() && !user.getVipCanceled();
            subscribeButton.setDisable(isVipAndActive);
            cancelVipButton.setVisible(isVipAndActive);
        });
    }

    @Subscribe
    public void handleVipActivated(Msg msg) {
        if (!"VIP_ACTIVATED".equals(msg.getAction())) return;

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("VIP Activated");
            alert.setHeaderText(null);
            alert.setContentText("You are now a VIP Member!");
            alert.showAndWait();
            EventBus.getDefault().unregister(this);
            SceneController.switchScene("home");
        });
    }

    @Subscribe
    public void handleVipCancelled(Msg msg) {
        if (!"VIP_CANCELLED".equals(msg.getAction())) return;

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("VIP Cancelled");
            alert.setHeaderText(null);
            alert.setContentText("VIP will be deactivated after current period ends.");
            alert.showAndWait();
            cancelVipButton.setDisable(true);
        });
    }

    private void openPaymentWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/Payment.fxml"));
            Parent root = loader.load();

            PaymentController controller = loader.getController();
            controller.setOnSuccess(() -> {
                try {
                    SimpleClient.getClient().sendToServer(new Msg("ACTIVATE_VIP", SceneController.loggedUsername));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Stage stage = new Stage();
            stage.setTitle("VIP Payment");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

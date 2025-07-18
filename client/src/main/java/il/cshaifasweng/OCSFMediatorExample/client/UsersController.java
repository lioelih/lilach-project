package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.*;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import javafx.collections.ListChangeListener;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UsersController {

    @FXML private TableView<UserDisplayDTO> usersTable;
    @FXML private TableColumn<UserDisplayDTO, Integer> colId;
    @FXML private TableColumn<UserDisplayDTO, String> colUsername;
    @FXML private TableColumn<UserDisplayDTO, String> colPassword;
    @FXML private TableColumn<UserDisplayDTO, String> colEmail;
    @FXML private TableColumn<UserDisplayDTO, String> colPhone;
    @FXML private TableColumn<UserDisplayDTO, String> colRole;
    @FXML private TableColumn<UserDisplayDTO, String> colBranch;
    @FXML private TableColumn<UserDisplayDTO, Double> colTotalSpent;
    @FXML private TableColumn<UserDisplayDTO, Boolean> colIsVIP;
    @FXML private TableColumn<UserDisplayDTO, Void> colActions;
    @FXML private TextField searchField;

    @FXML private Button goHomeButton;
    @FXML private ImageView logoImage;

    private final ObservableList<UserDisplayDTO> usersList = FXCollections.observableArrayList();

    // Dynamic branch names list - initially empty
    private final ObservableList<String> branchNames = FXCollections.observableArrayList();

    @FXML
    public void initialize() throws IOException {
        EventBus.getDefault().register(this);

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUsername.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsername.setOnEditCommit(event -> {
            UserDisplayDTO user = event.getRowValue();
            user.setUsername(event.getNewValue());
            sendUpdateUser(user);
        });

        colPassword.setCellValueFactory(new PropertyValueFactory<>("password"));
        colPassword.setCellFactory(TextFieldTableCell.forTableColumn());
        colPassword.setOnEditCommit(event -> {
            UserDisplayDTO user = event.getRowValue();
            user.setPassword(event.getNewValue());
            sendUpdateUser(user);
        });


        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setCellFactory(TextFieldTableCell.forTableColumn());
        colEmail.setOnEditCommit(event -> {
            UserDisplayDTO user = event.getRowValue();
            user.setEmail(event.getNewValue());
            sendUpdateUser(user);
        });

        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colPhone.setCellFactory(TextFieldTableCell.forTableColumn());
        colPhone.setOnEditCommit(event -> {
            UserDisplayDTO user = event.getRowValue();
            user.setPhone(event.getNewValue());
            sendUpdateUser(user);
        });

        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(ComboBoxTableCell.forTableColumn("USER", "WORKER", "MANAGER", "ADMIN"));
        colRole.setOnEditCommit(event -> {
            UserDisplayDTO user = event.getRowValue();
            user.setRole(event.getNewValue());
            sendUpdateUser(user);
        });

        colBranch.setCellValueFactory(new PropertyValueFactory<>("branchName"));

        // Use dynamic branchNames ObservableList for ComboBoxTableCell
        colBranch.setCellFactory(ComboBoxTableCell.forTableColumn(branchNames));
        colBranch.setOnEditCommit(event -> {
            UserDisplayDTO user = event.getRowValue();
            user.setBranchName(event.getNewValue());
            sendUpdateUser(user);
        });

        colIsVIP.setCellValueFactory(cellData -> cellData.getValue().vipProperty());
        colIsVIP.setCellFactory(CheckBoxTableCell.forTableColumn(colIsVIP));
        colIsVIP.setEditable(true);
        // I added the listener here because when checking the isVIP status, it wouldn't register unless a different attribute was changed, so this makes sure its constantly being monitored for a change
        usersList.addListener((ListChangeListener<UserDisplayDTO>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (UserDisplayDTO addedUser : change.getAddedSubList()) {
                        addedUser.vipProperty().addListener((obs, oldVal, newVal) -> {

                            addedUser.setVip(newVal);
                            sendUpdateUser(addedUser);
                        });
                    }
                }
            }
        });
        for (UserDisplayDTO u : usersList) {
            u.vipProperty().addListener((obs, oldVal, newVal) -> {
                u.setVip(newVal);
                sendUpdateUser(u);
            });
        }


        colTotalSpent.setCellValueFactory(new PropertyValueFactory<>("totalSpent"));

        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button toggleButton = new Button();

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    UserDisplayDTO user = getTableView().getItems().get(getIndex());
                    toggleButton.setText(user.isActive() ? "Freeze" : "Unfreeze");
                    toggleButton.setOnAction(e -> {
                        String action = user.isActive() ? "FREEZE_USER" : "UNFREEZE_USER";
                        try {
                            SimpleClient.getClient().sendToServer(new Msg(action, user.getId()));
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        user.setActive(!user.isActive());
                        toggleButton.setText(user.isActive() ? "Freeze" : "Unfreeze");
                    });
                    setGraphic(toggleButton);
                }
            }
        });

        FilteredList<UserDisplayDTO> filteredUsers = new FilteredList<>(usersList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String lower = newVal.toLowerCase();
            filteredUsers.setPredicate(user -> user.getUsername().toLowerCase().contains(lower));
        });

        usersTable.setItems(filteredUsers);
        usersTable.setEditable(true);

        goHomeButton.setOnAction(e -> {
            EventBus.getDefault().unregister(this);
            SceneController.switchScene("home");
        });

        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));

        // Request branches list from server
        System.out.println("[Client] Requesting branch list from server...");
        SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null));

        // Request users list from server
        SimpleClient.getClient().sendToServer(new Msg("FETCH_ALL_USERS", null));
    }

    private void sendUpdateUser(UserDisplayDTO user) {
        try {
            SimpleClient.getClient().sendToServer(new Msg("UPDATE_USER", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "phone", user.getPhone(),
                    "role", user.getRole(),
                    "active", user.isActive(),
                    "branchName", user.getBranchName(),
                    "password", user.getPassword(),
                    "isVIP", user.isVip()
            )));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onMsgReceived(Msg msg) {
        Platform.runLater(() -> {
            switch (msg.getAction()) {
                case "BRANCHES_OK" -> {
                    // Receive branch list, extract names, update ComboBox list
                    List<Branch> branches = (List<Branch>) msg.getData();
                    branchNames.clear();
                    branchNames.addAll(branches.stream()
                            .map(Branch::getBranchName)
                            .collect(Collectors.toList()));
                    System.out.println("[Client] Received branches from server: " + branchNames);
                    // Refresh branch column so UI updates with new combo box items
                    usersTable.refresh();
                }
                case "FETCH_ALL_USERS_OK" -> {
                    List<Map<String, Object>> raw = (List<Map<String, Object>>) msg.getData();
                    usersList.clear();
                    for (Map<String, Object> map : raw) {
                        usersList.add(new UserDisplayDTO(
                                (int) map.get("id"),
                                (String) map.get("username"),
                                (String) map.get("email"),
                                (String) map.get("phone"),
                                (String) map.get("role"),
                                (boolean) map.get("active"),
                                (double) map.get("totalSpent"),
                                (String) map.get("branchName"),
                                (String) map.get("password"),
                                (boolean) map.get("isVIP")
                        ));
                    }
                }
                case "UPDATE_USER_OK" -> {
                    System.out.println("[Client] User update successful");
                }
                case "UPDATE_USER_FAILED" -> {
                    String error = (String) msg.getData();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Update Failed");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to update user: " + error);
                    alert.showAndWait();
                }
            }
        });
    }
}

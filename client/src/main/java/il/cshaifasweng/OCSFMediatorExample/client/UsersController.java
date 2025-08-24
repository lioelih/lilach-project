package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.Msg;
import il.cshaifasweng.*;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
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
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * userscontroller
 * - shows a table of users with inline editing
 * - enforces view/edit permissions based on current role
 * - pushes edits to server on commit (username, password, email, phone, role, branch, vip)
 * - supports freezing/unfreezing by admins
 * - keeps branch choices dynamic and filters by search text
 * - listens for server events to keep ui in sync
 */
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
    private final ObservableList<String> branchNames = FXCollections.observableArrayList(); // dynamic branch list

    @FXML
    public void initialize() throws IOException {
        // gate access to workers/managers/admins
        boolean canView =
                SceneController.hasPermission(User.Role.WORKER) ||
                        SceneController.hasPermission(User.Role.MANAGER) ||
                        SceneController.hasPermission(User.Role.ADMIN);
        if (!canView) {
            SceneController.switchScene("home");
            return;
        }

        final boolean canEdit =
                SceneController.hasPermission(User.Role.MANAGER) ||
                        SceneController.hasPermission(User.Role.ADMIN);

        EventBus.getDefault().unregister(this);
        EventBus.getDefault().register(this);

        // id (read-only)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        // username (inline edit)
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUsername.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsername.setOnEditCommit(event -> {
            if (!canEdit) return;
            UserDisplayDTO user = event.getRowValue();
            user.setUsername(event.getNewValue());
            sendUpdateUser(user);
        });

        // password (inline edit)
        colPassword.setCellValueFactory(new PropertyValueFactory<>("password"));
        colPassword.setCellFactory(TextFieldTableCell.forTableColumn());
        colPassword.setOnEditCommit(event -> {
            if (!canEdit) return;
            UserDisplayDTO user = event.getRowValue();
            user.setPassword(event.getNewValue());
            sendUpdateUser(user);
        });

        // email (inline edit)
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setCellFactory(TextFieldTableCell.forTableColumn());
        colEmail.setOnEditCommit(event -> {
            if (!canEdit) return;
            UserDisplayDTO user = event.getRowValue();
            user.setEmail(event.getNewValue());
            sendUpdateUser(user);
        });

        // phone (inline edit)
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colPhone.setCellFactory(TextFieldTableCell.forTableColumn());
        colPhone.setOnEditCommit(event -> {
            if (!canEdit) return;
            UserDisplayDTO user = event.getRowValue();
            user.setPhone(event.getNewValue());
            sendUpdateUser(user);
        });

        // role (inline edit via combo)
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        if (canEdit) {
            colRole.setCellFactory(ComboBoxTableCell.forTableColumn("USER", "WORKER", "MANAGER", "ADMIN"));
        }
        colRole.setOnEditCommit(event -> {
            if (!canEdit) return;
            UserDisplayDTO user = event.getRowValue();
            user.setRole(event.getNewValue());
            sendUpdateUser(user);
        });

        // branch (inline edit via dynamic combo)
        colBranch.setCellValueFactory(new PropertyValueFactory<>("branchName"));
        if (canEdit) {
            colBranch.setCellFactory(ComboBoxTableCell.forTableColumn(branchNames));
        }
        colBranch.setOnEditCommit(event -> {
            if (!canEdit) return;
            UserDisplayDTO user = event.getRowValue();
            user.setBranchName(event.getNewValue());
            sendUpdateUser(user);
        });

        // vip (checkbox cell; push on change)
        colIsVIP.setCellValueFactory(cellData -> cellData.getValue().vipProperty());
        colIsVIP.setCellFactory(CheckBoxTableCell.forTableColumn(colIsVIP));
        colIsVIP.setEditable(true);

        // ensure vip changes are observed and sent immediately
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

        // totals (read-only)
        colTotalSpent.setCellValueFactory(new PropertyValueFactory<>("totalSpent"));

        // actions (freeze/unfreeze for admins)
        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button toggleButton = new Button();
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                boolean isAdmin = SceneController.hasPermission(User.Role.ADMIN);
                if (empty || !isAdmin) {
                    setGraphic(null);
                    setManaged(false);
                    return;
                }
                setManaged(true);
                UserDisplayDTO row = getTableView().getItems().get(getIndex());
                toggleButton.setText(row.isActive() ? "Freeze" : "Unfreeze");
                toggleButton.setOnAction(e -> {
                    String action = row.isActive() ? "FREEZE_USER" : "UNFREEZE_USER";
                    try { SimpleClient.getClient().sendToServer(new Msg(action, row.getId())); }
                    catch (IOException ex) { throw new RuntimeException(ex); }
                });
                setGraphic(toggleButton);
            }
        });

        // table editability by role
        usersTable.setEditable(canEdit);
        colUsername.setEditable(canEdit);
        colPassword.setEditable(canEdit);
        colEmail.setEditable(canEdit);
        colPhone.setEditable(canEdit);
        colRole.setEditable(canEdit);
        colBranch.setEditable(canEdit);
        colIsVIP.setEditable(canEdit);

        // live search by username
        FilteredList<UserDisplayDTO> filteredUsers = new FilteredList<>(usersList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String lower = newVal.toLowerCase();
            filteredUsers.setPredicate(user -> user.getUsername().toLowerCase().contains(lower));
        });
        usersTable.setItems(filteredUsers);
        usersTable.setEditable(true);

        // nav: go home
        goHomeButton.setOnAction(e -> {
            EventBus.getDefault().unregister(this);
            SceneController.switchScene("home");
        });

        // branding
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));

        // request initial data
        System.out.println("[Client] Requesting branch list from server...");
        SimpleClient.getClient().sendToServer(new Msg("LIST_BRANCHES", null));
        SimpleClient.getClient().sendToServer(new Msg("FETCH_ALL_USERS", null));
    }

    // push an update for a single user to the server
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

    // handle server events that affect the users view
    @Subscribe
    public void onMsgReceived(Msg msg) {
        Platform.runLater(() -> {
            switch (msg.getAction()) {
                case "BRANCHES_OK" -> {
                    // refresh branch list choices
                    List<Branch> branches = (List<Branch>) msg.getData();
                    branchNames.clear();
                    branchNames.addAll(branches.stream().map(Branch::getBranchName).collect(Collectors.toList()));
                    System.out.println("[Client] Received branches from server: " + branchNames);
                    usersTable.refresh();
                }
                case "FETCH_ALL_USERS_OK" -> {
                    // hydrate table from server payload
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
                case "USER_CREATED" -> {
                    // append new user if not present
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) msg.getData();
                    int id = (int) map.get("id");
                    boolean exists = usersList.stream().anyMatch(u -> u.getId() == id);
                    if (!exists) {
                        usersList.add(new UserDisplayDTO(
                                id,
                                (String) map.get("username"),
                                (String) map.get("email"),
                                (String) map.get("phone"),
                                (String) map.get("role"),
                                (boolean) map.get("active"),
                                ((Number) map.getOrDefault("totalSpent", 0.0)).doubleValue(),
                                (String) map.get("branchName"),
                                (String) map.get("password"),
                                (boolean) map.get("isVIP")
                        ));
                    }
                }
                case "USER_FREEZE_OK" -> {
                    // reflect freeze in table
                    int id = (int) msg.getData();
                    usersList.stream().filter(u -> u.getId() == id).findFirst()
                            .ifPresent(u -> { u.setActive(false); usersTable.refresh(); });
                }
                case "USER_UNFREEZE_OK" -> {
                    // reflect unfreeze in table
                    int id = (int) msg.getData();
                    usersList.stream().filter(u -> u.getId() == id).findFirst()
                            .ifPresent(u -> { u.setActive(true); usersTable.refresh(); });
                }
                case "UPDATE_USER_OK" -> {
                    System.out.println("[Client] User update successful");
                }
                case "USER_UPDATED" -> {
                    // merge remote user changes into the table
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) msg.getData();
                    int id = (int) map.get("id");
                    usersList.stream().filter(r -> r.getId() == id).findFirst().ifPresent(r -> {
                        r.setUsername((String) map.get("username"));
                        r.setEmail((String) map.get("email"));
                        r.setPhone((String) map.get("phone"));
                        r.setRole((String) map.get("role"));
                        r.setBranchName((String) map.get("branchName"));
                        r.setActive((boolean) map.get("active"));
                        r.setVip((boolean) map.get("isVIP"));
                        usersTable.refresh();
                    });
                }
                case "ACCOUNT_FROZEN" -> {
                    // if current client is frozen while in this screen
                    SceneController.forceLogoutWithAlert((String) msg.getData());
                }
                case "UPDATE_USER_FAILED" -> {
                    // show error from server
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

    // react to local role/vip changes (e.g., permissions downgraded)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLocalRoleVipChanged(Msg msg) {
        if (!"LOCAL_ROLE_VIP_CHANGED".equals(msg.getAction())) return;
        boolean canView =
                SceneController.hasPermission(User.Role.WORKER) ||
                        SceneController.hasPermission(User.Role.MANAGER) ||
                        SceneController.hasPermission(User.Role.ADMIN);
        if (!canView) {
            try { SimpleClient.getClient().sendToServer("remove client"); } catch (IOException ignored) {}
            EventBus.getDefault().unregister(this);
            SceneController.switchScene("home");
            return;
        }
        usersTable.refresh();
    }
}

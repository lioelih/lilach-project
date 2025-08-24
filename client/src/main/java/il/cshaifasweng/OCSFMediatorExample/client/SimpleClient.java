package il.cshaifasweng.OCSFMediatorExample.client;

import Events.*;
import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import java.io.IOException;
import java.io.ObjectStreamClass;

/*
 * simpleclient
 * - wraps ocsf abstractclient
 * - routes server messages onto the fx thread via eventbus
 * - deduplicates 'account_frozen' alerts using an atomic guard
 * - exposes helpers for connection lifecycle (getclient, ensureconnected, logoutandclose)
 */
public class SimpleClient extends AbstractClient {
	private static final java.util.concurrent.atomic.AtomicBoolean FREEZE_HANDLED = new java.util.concurrent.atomic.AtomicBoolean(false);
	private static SimpleClient client = null;

	public SimpleClient(String host, int port) {
		super(host, port);
		System.out.println("User.class from:   " + User.class.getResource("User.class"));
		System.out.println("User SUID:         " + ObjectStreamClass.lookup(User.class).getSerialVersionUID());
		System.out.println("Branch.class from: " + Branch.class.getResource("Branch.class"));
		System.out.println("Branch SUID:       " + ObjectStreamClass.lookup(Branch.class).getSerialVersionUID());
	}

	public static void setClient(SimpleClient c) {
		client = c;
	}

	@Override
	protected void handleMessageFromServer(Object msg) {
		if (msg instanceof Warning warning) {
			postFx(new WarningEvent(warning));
			return;
		}
		if (!(msg instanceof Msg massage)) return;

		switch (massage.getAction()) {
			case "SENT_CATALOG" -> {
				System.out.println("Meow");
				postFx(new CatalogEvent("SENT_CATALOG", (java.util.List<il.cshaifasweng.OCSFMediatorExample.entities.Product>) massage.getData()));
			}
			case "PRODUCT_UPDATED" -> {
				postFx(new CatalogEvent("PRODUCT_UPDATED", (java.util.List<il.cshaifasweng.OCSFMediatorExample.entities.Product>) massage.getData()));
			}
			case "PRODUCT_ADDED" -> {
				postFx(new CatalogEvent("PRODUCT_ADDED", (java.util.List<il.cshaifasweng.OCSFMediatorExample.entities.Product>) massage.getData()));
			}
			case "PRODUCT_DELETED" -> {
				postFx(new CatalogEvent("PRODUCT_DELETED", (java.util.List<il.cshaifasweng.OCSFMediatorExample.entities.Product>) massage.getData()));
			}
			case "LOGIN_SUCCESS", "LOGIN_FAILED" -> {
				if ("LOGIN_SUCCESS".equals(massage.getAction())) {
					// re-arm freeze guard on successful login
					FREEZE_HANDLED.set(false);
				}
				postFx(new LoginEvent(massage));
			}
			case "REGISTER_SUCCESS", "REGISTER_FAILED" -> postFx(new RegisterEvent(massage));
			case "FETCH_USER", "PAYMENT_PREFILL",
				 "PAYMENT_INFO", "VIP_ACTIVATED", "VIP_CANCELLED",
				 "BASKET_FETCHED", "BASKET_UPDATED",
				 "HAS_CARD", "ORDER_OK", "ORDER_FAIL", "BRANCHES_OK",
				 "STOCK_OK", "ADD_STOCK_OK", "STOCK_SINGLE_OK", "FETCH_ORDERS_OK", "FETCH_ORDER_PRODUCTS_OK",
				 "MARK_ORDER_RECEIVED_OK", "FETCH_ALL_USERS_OK", "FREEZE_USER_OK", "UNFREEZE_USER_OK",
				 "CHANGE_ROLE_OK","USER_UNFREEZE_OK","USER_FREEZE_OK", "UPDATE_USER_OK","USER_CREATED",
				 "UPDATE_USER_FAILED", "CANCEL_OK", "CREATE_CUSTOM_BOUQUET_OK","UPDATE_CUSTOM_BOUQUET_OK",
				 "LIST_CUSTOM_BOUQUETS_OK","ORDERS_DIRTY" -> {
				postFx(massage);
			}
			case "ACCOUNT_FROZEN" -> {
				// show a single logout alert for account freeze
				if (FREEZE_HANDLED.compareAndSet(false, true)) {
					javafx.application.Platform.runLater(() ->
							SceneController.forceLogoutWithAlert((String) massage.getData())
					);
				}
			}
			case "USER_UPDATED" -> {
				// still broadcast to event bus
				postFx(massage);
				try {
					@SuppressWarnings("unchecked")
					java.util.Map<String, Object> row = (java.util.Map<String, Object>) massage.getData();
					String username = (String) row.get("username");
					if (SceneController.loggedUsername != null && SceneController.loggedUsername.equals(username)) {
						String roleStr = (String) row.get("role");
						boolean newVip = java.lang.Boolean.TRUE.equals(row.get("isVIP"));
						il.cshaifasweng.OCSFMediatorExample.entities.User.Role newRole =
								il.cshaifasweng.OCSFMediatorExample.entities.User.Role.valueOf(roleStr);
						SceneController.setCurrentUserRole(newRole);
						SceneController.isVIP = newVip;
						// local role/vip change event posted via fx thread
						postFx(new Msg("LOCAL_ROLE_VIP_CHANGED", null));
					}
				} catch (ClassCastException ignored) { }
			}
			case "SENT_SALES"     -> postFx(new Events.SalesEvent("SENT_SALES", (java.util.List<il.cshaifasweng.OCSFMediatorExample.entities.Sale>) massage.getData()));
			case "SALE_ADDED"     -> postFx(new Events.SalesEvent("SALE_ADDED", (java.util.List<il.cshaifasweng.OCSFMediatorExample.entities.Sale>) massage.getData()));
			case "SALE_UPDATED"   -> postFx(new Events.SalesEvent("SALE_UPDATED", (java.util.List<il.cshaifasweng.OCSFMediatorExample.entities.Sale>) massage.getData()));
			case "SALE_DELETED"   -> postFx(new Events.SalesEvent("SALE_DELETED", (java.util.List<il.cshaifasweng.OCSFMediatorExample.entities.Sale>) massage.getData()));
			default -> System.out.println("Unhandled message: " + massage.getAction());
		}
	}

	private void postFx(Object event) {
		javafx.application.Platform.runLater(() -> org.greenrobot.eventbus.EventBus.getDefault().post(event));
	}

	public static SimpleClient getClient() {
		if (client == null) {
			client = new SimpleClient("localhost", 3000); // default host/port
			System.out.println("New client created");
		}
		return client;
	}

	public static boolean ensureConnected() {
		if (client == null) return false;
		if (!client.isConnected()) {
			try {
				client.openConnection();
				System.out.println("Client reconnected!");
				return true;
			} catch (IOException e) {
				System.err.println("Reconnection failed: " + e.getMessage());
				return false;
			}
		}
		return true;
	}

	public static void logoutAndClose(String username) {
		if (client == null) return;
		try {
			if (!client.isConnected()) client.openConnection();
			client.sendToServer(new Msg("LOGOUT", username));
		} catch (IOException ignored) {
		} finally {
			try { client.closeConnection(); } catch (IOException ignored) {}
		}
	}
}

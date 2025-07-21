package il.cshaifasweng.OCSFMediatorExample.client;

import Events.*;
import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.List;

public class SimpleClient extends AbstractClient {

	private static SimpleClient client = null;

	public SimpleClient(String host, int port) {
		super(host, port);
	}

	public static void setClient(SimpleClient c) {
		client = c;
	}

	@Override
	protected void handleMessageFromServer(Object msg) {
		if (msg instanceof Warning warning) {
			EventBus.getDefault().post(new WarningEvent(warning));
		}
		else if (msg instanceof Msg massage) {
			switch (massage.getAction()) {
				case "SENT_CATALOG"      -> EventBus.getDefault()
						.post(new CatalogEvent("SENT_CATALOG",
								(List<Product>) massage.getData()));
				case "PRODUCT_UPDATED"   -> EventBus.getDefault()
						.post(new CatalogEvent("PRODUCT_UPDATED",
								(List<Product>) massage.getData()));
				case "PRODUCT_ADDED"     -> EventBus.getDefault()
						.post(new CatalogEvent("PRODUCT_ADDED",
								(List<Product>) massage.getData()));
				case "PRODUCT_DELETED"   -> EventBus.getDefault()
						.post(new CatalogEvent("PRODUCT_DELETED",
								(List<Product>) massage.getData()));

				case "LOGIN_SUCCESS", "LOGIN_FAILED"   ->
						EventBus.getDefault().post(new LoginEvent(massage));
				case "REGISTER_SUCCESS", "REGISTER_FAILED" ->
						EventBus.getDefault().post(new RegisterEvent(massage));

				case "FETCH_USER", "PAYMENT_PREFILL",
					 "PAYMENT_INFO", "VIP_ACTIVATED", "VIP_CANCELLED",
					 "BASKET_FETCHED", "BASKET_UPDATED",
					 "HAS_CARD", "ORDER_OK", "ORDER_FAIL", "BRANCHES_OK",
					 "STOCK_OK", "ADD_STOCK_OK", "STOCK_SINGLE_OK", "FETCH_ORDERS_OK", "FETCH_ORDER_PRODUCTS_OK",
					 "MARK_ORDER_RECEIVED_OK", "FETCH_ALL_USERS_OK", "FREEZE_USER_OK", "UNFREEZE_USER_OK",
					 "CHANGE_ROLE_OK", "UPDATE_USER_OK", "UPDATE_USER_FAILED", "CANCEL_OK", "CREATE_CUSTOM_BOUQUET_OK","UPDATE_CUSTOM_BOUQUET_OK", "LIST_CUSTOM_BOUQUETS_OK"->   // ← new line
						EventBus.getDefault().post(massage);

				case "SENT_SALES" ->
						EventBus.getDefault().post(new SalesEvent("SENT_SALES",
								(List<Sale>) massage.getData()));

				default ->
						System.out.println("Unhandled message: " + massage.getAction());
			}

		}
	}

	public static SimpleClient getClient() {
		if (client == null) {
			client = new SimpleClient("localhost", 3000); // Default host/port
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
}

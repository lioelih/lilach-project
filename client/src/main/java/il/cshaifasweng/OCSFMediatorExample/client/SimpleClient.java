package il.cshaifasweng.OCSFMediatorExample.client;

import Events.*;
import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class SimpleClient extends AbstractClient {

	private static SimpleClient client = null;

	public SimpleClient(String host, int port) {
		super(host, port);
	}

	@Override
	protected void handleMessageFromServer(Object msg) {
		if (msg instanceof Warning warning) {
			EventBus.getDefault().post(new WarningEvent(warning));
		}
		else if (msg instanceof Msg massage) {
			switch (massage.getAction()) {
				case "SENT_CATALOG" -> {
					EventBus.getDefault().post(new CatalogEvent("SENT_CATALOG", (List<Product>) massage.getData()));
				}
				case "PRODUCT_UPDATED" -> {
					EventBus.getDefault().post(new CatalogEvent("PRODUCT_UPDATED", (List<Product>) massage.getData()));
				}
				case "PRODUCT_ADDED" -> {
					EventBus.getDefault().post(new CatalogEvent("PRODUCT_ADDED", (List<Product>) massage.getData()));
				}
				case "PRODUCT_DELETED" -> {
					EventBus.getDefault().post(new CatalogEvent("PRODUCT_DELETED", (List<Product>) massage.getData()));
				}
				case "LOGIN_SUCCESS", "LOGIN_FAILED" ->
						EventBus.getDefault().post(new LoginEvent(massage));
				case "REGISTER_SUCCESS", "REGISTER_FAILED" ->
						EventBus.getDefault().post(new RegisterEvent(massage));
				case "FETCH_USER" ->
						EventBus.getDefault().post(massage);
				case "PAYMENT_PREFILL" ->
						EventBus.getDefault().post(massage);
				case "PAYMENT_INFO" ->
						EventBus.getDefault().post(massage);
				case "VIP_ACTIVATED", "VIP_CANCELLED" ->
						EventBus.getDefault().post(massage);
				default ->
						System.out.println("Unhandled message: " + massage.getAction());

			}
		}
	}

	public static SimpleClient getClient() {
		if (client == null) {
			client = new SimpleClient("localhost", 3000); // Default host/port
		}
		return client;
	}
}

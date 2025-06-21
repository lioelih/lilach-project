package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import org.greenrobot.eventbus.EventBus;

import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;

import java.util.List;

public class SimpleClient extends AbstractClient {

	private static SimpleClient client = null;
	public SimpleClient(String host, int port) {
		super(host, port);
	}


	@Override
	protected void handleMessageFromServer(Object msg) {
		if (msg instanceof Warning) {
			EventBus.getDefault().post(new WarningEvent((Warning) msg));
		} else if (msg instanceof List<?>) { // When receiving a list from server, send an event bus of a CatalogEvent class
			List<?> list = (List<?>) msg;
			if (!list.isEmpty() && list.get(0) instanceof Product) {
				@SuppressWarnings("unchecked")
				List<Product> products = (List<Product>) list;
				EventBus.getDefault().post(new CatalogEvent(products));
			}
		}
		else if (msg instanceof LoginResponse) {
			System.out.println("CLIENT: Got LoginResponse: " + ((LoginResponse) msg).message);
			EventBus.getDefault().post((LoginResponse) msg);
		}
		else if (msg instanceof RegisterResponse) {
			EventBus.getDefault().post((RegisterResponse) msg);
		}
		else if (msg instanceof FetchUserResponse) {
			FetchUserResponse response = (FetchUserResponse) msg;
			System.out.println("CLIENT: Received FetchUserResponse for " + response.getUser().getUsername());
			EventBus.getDefault().post(response);
		}
		else if (msg instanceof PaymentPrefillResponse) {
			System.out.println("CLIENT: Received PaymentPrefillResponse, posting to EventBus.");
			EventBus.getDefault().post((PaymentPrefillResponse) msg);
		}

	}


	public static SimpleClient getClient() {
		if (client == null) {
			client = new SimpleClient("localhost", 3000); // Default Constructor
		}
		return client;
	}

}

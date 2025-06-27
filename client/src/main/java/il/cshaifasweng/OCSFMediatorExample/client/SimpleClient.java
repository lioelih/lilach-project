package il.cshaifasweng.OCSFMediatorExample.client;

import Events.CatalogEvent;
import Events.WarningEvent;
import il.cshaifasweng.Msg;
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
		}
		else if (msg instanceof Msg massage) {
			switch (massage.getAction()) {
				case "SENT_CATALOG": {
					CatalogEvent event = new CatalogEvent("SENT_CATALOG", (List<Product>) massage.getData());
					EventBus.getDefault().post(event);
				}
				case "PRODUCT_UPDATED": {
					CatalogEvent event = new CatalogEvent("PRODUCT_UPDATED", (List<Product>) massage.getData());
					EventBus.getDefault().post(event);
				}
				case "PRODUCT_ADDED": {
					CatalogEvent event = new CatalogEvent("PRODUCT_ADDED", (List<Product>) massage.getData());
					EventBus.getDefault().post(event);
				}
				case "PRODUCT_DELETED": {
					CatalogEvent event = new CatalogEvent("PRODUCT_DELETED", (List<Product>) massage.getData());
					EventBus.getDefault().post(event);
				}
				default: {
					WarningEvent event = new WarningEvent(new Warning("Catalog wont load"));
					EventBus.getDefault().post(event);
				}
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

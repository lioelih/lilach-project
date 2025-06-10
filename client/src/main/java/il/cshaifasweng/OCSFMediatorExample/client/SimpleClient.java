package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import org.greenrobot.eventbus.EventBus;

import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;

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
	}


	public static SimpleClient getClient() {
		if (client == null) {
			client = new SimpleClient("localhost", 3000); // Default Constructor
		}
		return client;
	}

}

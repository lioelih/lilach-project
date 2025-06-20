package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;
import org.hibernate.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimpleServer extends AbstractServer {
	private static final ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();

	public SimpleServer(int port) {
		super(port);
	}

	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) throws IOException {
		if (msg instanceof String msgString) {

			if (msgString.startsWith("#warning")) {
				Warning warning = new Warning("Warning from server!");
				client.sendToClient(warning);
				System.out.format("Sent warning to client %s\n", client.getInetAddress().getHostAddress());

			} else if (msgString.equals("GET_CATALOG")) {
				List<Product> catalog = fetchCatalog();
				client.sendToClient(catalog);
				System.out.printf("Sent %d products to client %s%n", catalog.size(), client.getInetAddress().getHostAddress());

			} else if (msgString.startsWith("add client")) {
				SubscribedClient connection = new SubscribedClient(client);
				SubscribersList.add(connection);
				client.sendToClient("client added successfully");

			} else if (msgString.startsWith("remove client")) {
				SubscribersList.removeIf(subscribedClient -> subscribedClient.getClient().equals(client));
			}
		}
		else if (msg instanceof Product product) {
			if (product.getId() <= 0) {
				saveNewProduct(product);
				client.sendToClient("Product added successfully");
				client.sendToClient(fetchCatalog());

			} else {
				updateFullProduct(product);
				client.sendToClient("Product updated successfully");
				client.sendToClient(fetchCatalog());
			}
		}
	}

	private void saveNewProduct(Product product) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			session.beginTransaction();
			session.save(product); // image is saved as BLOB via JPA automatically
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateFullProduct(Product updatedProduct) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			session.beginTransaction();
			Product dbProduct = session.get(Product.class, updatedProduct.getId());
			if (dbProduct != null) {
				dbProduct.setName(updatedProduct.getName());
				dbProduct.setType(updatedProduct.getType());
				dbProduct.setPrice(updatedProduct.getPrice());
				dbProduct.setImage(updatedProduct.getImage()); // also updates BLOB
				session.update(dbProduct);
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<Product> fetchCatalog() {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			return session.createQuery("from Product", Product.class).list();
		}
	}

	public void sendToAllClients(String message) {
		for (SubscribedClient subscribedClient : SubscribersList) {
			try {
				subscribedClient.getClient().sendToClient(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

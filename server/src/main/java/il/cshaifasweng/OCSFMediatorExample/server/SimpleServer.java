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
			}
			else if (msgString.equals("GET_CATALOG")) {
				List<Product> catalog = fetchCatalog();
				client.sendToClient(catalog);
				System.out.printf("Sent %d products to client %s%n", catalog.size(), client.getInetAddress().getHostAddress());
			}

			else if (msgString.startsWith("add client")) {
				SubscribedClient connection = new SubscribedClient(client);
				SubscribersList.add(connection);
				client.sendToClient("client added successfully");
			}
			else if (msgString.startsWith("remove client")) {
				SubscribersList.removeIf(subscribedClient -> subscribedClient.getClient().equals(client));
			}
		}
		else if (msg instanceof Product product) {
			if (product.getId() <= 0) {
				saveNewProduct(product);
				client.sendToClient("Product added successfully");
				client.sendToClient(fetchCatalog());
			}
			else {
				updateFullProduct(product);
				client.sendToClient("Product updated successfully");
				List<Product> updatedCatalog = fetchCatalog();
				client.sendToClient(updatedCatalog);
			}
		}
	}

	private void updateFullProduct(Product updatedProduct) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			session.beginTransaction();
			Product dbProduct = session.get(Product.class, updatedProduct.getId());
			if (dbProduct != null) {
				dbProduct.setPrice(updatedProduct.getPrice());
				dbProduct.setName(updatedProduct.getName());
				dbProduct.setType(updatedProduct.getType());
				dbProduct.setImage(updatedProduct.getImage());
				session.update(dbProduct);
			}
			session.getTransaction().commit();
		}
	}

	private void saveNewProduct(Product product) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			session.beginTransaction();
			session.save(product); // Hibernate will auto-generate the ID
			session.getTransaction().commit();
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

	private List<Product> fetchCatalog() {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			return session.createQuery("from Product", Product.class).list();
		}
	}

	private void updateProductPrice(int id, double newPrice) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			session.beginTransaction();
			Product product = session.get(Product.class, id);
			if (product != null) {
				product.setPrice(newPrice);
				session.update(product);
			}
			session.getTransaction().commit();
		}
	}
}

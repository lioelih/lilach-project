package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.entities.LogoutRequest;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;
import org.hibernate.Session;
import il.cshaifasweng.OCSFMediatorExample.entities.LoginResponse;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.io.IOException;

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
		else if (msg instanceof LoginRequest request) {
			System.out.println("SERVER: Received LoginRequest from user: " + request.username);
			try (Session session = HibernateUtil.getSessionFactory().openSession()) {
				Query<User> query = session.createQuery(
						"FROM User WHERE username = :username AND password = :password", User.class);
				query.setParameter("username", request.username);
				query.setParameter("password", request.password);
				List<User> users = query.list();
				boolean exists = !users.isEmpty();
				String msgText = exists ? "Login successful" : "Invalid credentials";
				LoginResponse response = new LoginResponse(exists, msgText);

				System.out.println("SERVER: Sending LoginResponse with success=" + exists + ", message=" + msgText);
				client.sendToClient(response); // Make sure this line is reached!
			}
			catch (Exception e) {
				System.out.println("SERVER ERROR WHILE HANDLING LOGIN:");
				e.printStackTrace();
			}
		}


		else if (msg instanceof RegisterRequest request) {
			try (Session session = HibernateUtil.getSessionFactory().openSession()) {
				Query<?> check = session.createQuery("FROM User WHERE username = :username OR email = :email");
				check.setParameter("username", request.username);
				check.setParameter("email", request.email);
				if (!check.list().isEmpty()) {
					client.sendToClient(new RegisterResponse(false, "User already exists"));
					return;
				}

				session.beginTransaction();
				User newUser = new User();
				newUser.setUsername(request.username);
				newUser.setEmail(request.email);
				newUser.setPhoneNumber(request.phoneNumber);
				newUser.setFullName(request.fullName);
				newUser.setPassword(request.password);
				newUser.setRole(User.Role.USER);
				session.persist(newUser);
				session.getTransaction().commit();

				client.sendToClient(new RegisterResponse(true, "Registration successful"));
			}
		}
		else if (msg instanceof LogoutRequest logout) {
			try (Session session = HibernateUtil.getSessionFactory().openSession()) {
				session.beginTransaction();
				Query query = session.createQuery("UPDATE User SET loggedIn = false WHERE username = :username");
				query.setParameter("username", logout.username);
				query.executeUpdate();
				session.getTransaction().commit();
			}
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

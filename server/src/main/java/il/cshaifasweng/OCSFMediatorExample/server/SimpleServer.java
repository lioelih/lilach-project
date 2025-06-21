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
import java.time.LocalDate;
import java.io.IOException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimpleServer extends AbstractServer {
	private static final ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();
	private ScheduledExecutorService scheduler;
	public SimpleServer(int port) {
		super(port);
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> {
			System.out.println("Running daily VIP expiration check...");
			expireVIPAccountsIfNeeded();
		}, 0, 24, TimeUnit.HOURS);
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
			else if (msgString.startsWith("UPDATE_PRICE")) {
				String[] parts = msgString.split(":");
				int id = Integer.parseInt(parts[1]);
				double newPrice = Double.parseDouble(parts[2]);

				updateProductPrice(id, newPrice);

				List<Product> updatedCatalog = fetchCatalog();
				client.sendToClient(updatedCatalog);
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
		else if (msg instanceof FetchUserRequest request) {
			System.out.println("Sending FetchUserRequest");
			try (Session session = HibernateUtil.getSessionFactory().openSession()) {
				User user = session.createQuery("FROM User WHERE username = :username", User.class)
						.setParameter("username", request.getUsername())
						.uniqueResult();
				client.sendToClient(new FetchUserResponse(user));
			}
		}
		else if (msg instanceof Product product) {
			updateFullProduct(product);
			client.sendToClient("Product updated successfully");
			List<Product> updatedCatalog = fetchCatalog();
			client.sendToClient(updatedCatalog);
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
				newUser.setBranch(request.branch);
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
		else if (msg instanceof PaymentInfoRequest request) {
			System.out.println("SERVER: Received PaymentInfoRequest from " + request.getUsername());
			try (Session session = HibernateUtil.getSessionFactory().openSession()) {
				User user = session.createQuery("FROM User WHERE username = :username", User.class)
						.setParameter("username", request.getUsername())
						.uniqueResult();

				if (user != null) {
					session.beginTransaction();
					user.setIdentificationNumber(request.getIdNumber());
					user.setCreditCardNumber(request.getCardNumber());
					user.setCreditCardExpiration(request.getExpDate());
					user.setCreditCardSecurityCode(request.getCvv());
					session.merge(user);
					session.getTransaction().commit();

					client.sendToClient(new PaymentInfoResponse(true, "Payment details updated."));
					System.out.println("SERVER: Payment info updated for " + user.getUsername());
				} else {
					client.sendToClient(new PaymentInfoResponse(false, "User not found."));
				}
			} catch (Exception e) {
				e.printStackTrace();
				client.sendToClient(new PaymentInfoResponse(false, "An error occurred."));
			}
		}
		else if (msg instanceof VIPPaymentRequest request) {
			System.out.println("SERVER: Received VIPPaymentRequest from " + request.getUsername());
			try (Session session = HibernateUtil.getSessionFactory().openSession()) {
				Query<User> query = session.createQuery("FROM User WHERE username = :username", User.class);
				query.setParameter("username", request.getUsername());
				List<User> users = query.list();

				if (!users.isEmpty()) {
					User user = users.get(0);

					session.beginTransaction();

					if (user.isVIP() && user.getVipCanceled()) {
						// User had canceled but is reactivating before expiration
						user.setVipCanceled(false);
						System.out.println("SERVER: VIP reactivation without new charge.");
					} else if (!user.isVIP()) {
						// First-time subscription
						user.setVIP(true);
						user.setVipExpirationDate(LocalDate.now().plusMonths(12));
						user.setVipCanceled(false);
						user.setActive(true);
					}
					user.setIdentificationNumber(request.getIdNumber());
					user.setCreditCardNumber(request.getCardNumber());
					user.setCreditCardExpiration(request.getExpDate());
					user.setCreditCardSecurityCode(request.getCvv());
					session.merge(user);
					session.getTransaction().commit();
					client.sendToClient(new PaymentInfoResponse(true, "VIP subscription processed."));
				} else {
					client.sendToClient(new PaymentInfoResponse(false, "User not found."));
				}
			}
		}
		else if (msg instanceof CancelVIPRequest request) {
			System.out.println("SERVER: Received CancelVIPRequest from " + request.getUsername());

			try (Session session = HibernateUtil.getSessionFactory().openSession()) {
				User user = session.createQuery("FROM User WHERE username = :username", User.class)
						.setParameter("username", request.getUsername())
						.uniqueResult();

				if (user != null) {
					user.setVipCanceled(true); // flag it for expiration handling
					session.beginTransaction();
					session.merge(user);
					session.getTransaction().commit();

					System.out.println("SERVER: VIP successfully cancelled for " + user.getUsername());
				} else {
					System.out.println("SERVER: User not found for cancellation: " + request.getUsername());
				}
			}
		}
		else if (msg instanceof PaymentPrefillRequest request) {
			System.out.println("SERVER: Received PaymentPrefillRequest from " + request.getUsername());
			try (Session session = HibernateUtil.getSessionFactory().openSession()) {
				User user = session.createQuery("FROM User WHERE username = :username", User.class)
						.setParameter("username", request.getUsername())
						.uniqueResult();

				if (user != null) {
					client.sendToClient(new PaymentPrefillResponse(
							user.getIdentificationNumber(),
							user.getCreditCardNumber(),
							user.getCreditCardExpiration(),
							user.getCreditCardSecurityCode()
					));
				} else {
					client.sendToClient(new PaymentPrefillResponse("", "", "", ""));
				}
			} catch (Exception e) {
				e.printStackTrace();
				client.sendToClient(new PaymentPrefillResponse("", "", "", ""));
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

	public void expireVIPAccountsIfNeeded() {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			session.beginTransaction();

			List<User> users = session.createQuery(
					"FROM User WHERE isVIP = true AND vipCanceled = true", User.class
			).list();

			LocalDate today = LocalDate.now();

			for (User user : users) {
				if (user.getVipExpirationDate() != null && !user.getVipExpirationDate().isAfter(today)) {
					user.setVIP(false);
					user.setActive(false);
					session.merge(user);
					System.out.println("Deactivated expired VIP user: " + user.getUsername());
				}
			}

			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
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

	@Override
	protected void serverStopped() {
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdownNow();
		}
		System.out.println("Server stopped. Scheduler shut down.");
	}

}

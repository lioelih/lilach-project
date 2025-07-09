package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.io.IOException;
import java.time.LocalDate;
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
                client.sendToClient(new Warning("Warning from server!"));
            } else if (msgString.equals("add client")) {
                SubscribersList.add(new SubscribedClient(client));
                client.sendToClient("client added successfully");
            } else if (msgString.equals("remove client")) {
                SubscribersList.removeIf(sc -> sc.getClient().equals(client));
            }
        } else if (msg instanceof Msg massage) {
            String action = massage.getAction();
            Object data = massage.getData();

            switch (action) {
                case "LOGIN" -> {
                    String[] credentials = (String[]) data;
                    String username = credentials[0];
                    String password = credentials[1];

                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        Query<User> query = session.createQuery("FROM User WHERE username = :u AND password = :p", User.class);
                        query.setParameter("u", username);
                        query.setParameter("p", password);
                        List<User> users = query.list();

                        if (users.isEmpty()) {
                            client.sendToClient(new Msg("LOGIN_FAILED", "Invalid credentials"));
                        } else {
                            User user = users.get(0);
                            if (!user.isActive()) {
                                client.sendToClient(new Msg("LOGIN_FAILED", "Account is inactive"));
                            } else {
                                client.sendToClient(new Msg("LOGIN_SUCCESS", user.getUsername()));
                            }
                        }
                    }
                }

                case "REGISTER" -> {
                    String[] regData = (String[]) data;
                    String username = regData[0], email = regData[1], fullName = regData[2],
                            phoneNumber = regData[3], password = regData[4], branch = regData[5];

                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        Query<User> check = session.createQuery("FROM User WHERE username = :u OR email = :e");
                        check.setParameter("u", username);
                        check.setParameter("e", email);

                        if (!check.list().isEmpty()) {
                            client.sendToClient(new Msg("REGISTER_FAILED", "User already exists"));
                            return;
                        }

                        User newUser = new User();
                        newUser.setUsername(username);
                        newUser.setEmail(email);
                        newUser.setPhoneNumber(phoneNumber);
                        newUser.setFullName(fullName);
                        newUser.setPassword(password);
                        newUser.setBranch(branch);
                        newUser.setRole(User.Role.USER);
                        newUser.setActive(true);

                        session.beginTransaction();
                        session.persist(newUser);
                        session.getTransaction().commit();

                        client.sendToClient(new Msg("REGISTER_SUCCESS", null));
                    }
                }

                case "LOGOUT" -> {
                    String username = (String) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        session.beginTransaction();
                        Query query = session.createQuery("UPDATE User SET loggedIn = false WHERE username = :u");
                        query.setParameter("u", username);
                        query.executeUpdate();
                        session.getTransaction().commit();
                    }
                }

                case "FETCH_USER" -> {
                    String username = (String) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username)
                                .uniqueResult();
                        client.sendToClient(new Msg("FETCH_USER", user));
                    }
                }

                case "PAYMENT_INFO" -> {
                    String[] pData = (String[]) data;
                    String username = pData[0], id = pData[1], card = pData[2],
                            exp = pData[3], cvv = pData[4];

                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username).uniqueResult();

                        if (user != null) {
                            session.beginTransaction();
                            user.setIdentificationNumber(id);
                            user.setCreditCardNumber(card);
                            user.setCreditCardExpiration(exp);
                            user.setCreditCardSecurityCode(cvv);
                            session.merge(user);
                            session.getTransaction().commit();
                            client.sendToClient(new Msg("PAYMENT_INFO", "Saved"));
                        } else {
                            client.sendToClient(new Msg("PAYMENT_INFO", "User not found"));
                        }
                    }
                }

                case "PAYMENT_PREFILL" -> {
                    String username = (String) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username).uniqueResult();
                        if (user != null) {
                            String[] cardData = new String[]{
                                    user.getIdentificationNumber(),
                                    user.getCreditCardNumber(),
                                    user.getCreditCardExpiration(),
                                    user.getCreditCardSecurityCode()
                            };
                            client.sendToClient(new Msg("PAYMENT_PREFILL", cardData));
                        } else {
                            client.sendToClient(new Msg("PAYMENT_PREFILL", new String[]{"", "", "", ""}));
                        }
                    }
                }

                case "ACTIVATE_VIP" -> {
                    String username = (String) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username).uniqueResult();
                        if (user != null) {
                            session.beginTransaction();
                            if (!user.isVIP()) {
                                user.setVIP(true);
                                user.setVipExpirationDate(LocalDate.now().plusMonths(12));
                            }
                            user.setVipCanceled(false);
                            user.setActive(true);
                            session.merge(user);
                            session.getTransaction().commit();
                            client.sendToClient(new Msg("VIP_ACTIVATED", null));
                        }
                    }
                }

                case "CANCEL_VIP" -> {
                    String username = (String) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username).uniqueResult();
                        if (user != null) {
                            session.beginTransaction();
                            user.setVipCanceled(true);
                            session.merge(user);
                            session.getTransaction().commit();
                            client.sendToClient(new Msg("VIP_CANCELLED", null));
                        }
                    }
                }

                case "GET_CATALOG" -> {
                    List<Product> catalog = fetchCatalog();
                    client.sendToClient(new Msg("SENT_CATALOG", catalog));
                }

                case "ADD_PRODUCT" -> {
                    Product product = (Product) data;
                    saveNewProduct(product);
                    client.sendToClient(new Msg("PRODUCT_ADDED", fetchCatalog()));
                }

                case "UPDATE_PRODUCT" -> {
                    Product product = (Product) data;
                    updateFullProduct(product);
                    client.sendToClient(new Msg("PRODUCT_UPDATED", fetchCatalog()));
                }

                case "DELETE_PRODUCT" -> {
                    Product product = (Product) data;
                    deleteProduct(product.getId());
                    client.sendToClient(new Msg("PRODUCT_DELETED", fetchCatalog()));
                }

                case "GET_SALES" -> {
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        List<Sale> sales = session.createQuery("FROM Sale", Sale.class).list();

                        for (Sale sale : sales) {
                            sale.setProductIds(session.createNativeQuery(
                                            "SELECT product_id FROM sale_products WHERE sale_id = :saleId", Integer.class)
                                    .setParameter("saleId", sale.getId())
                                    .getResultList());
                        }
                        client.sendToClient(new Msg("SENT_SALES", sales));
                    }
                }

                default -> client.sendToClient(new Msg("ERROR", "Unknown action: " + action));
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
                dbProduct.setImage(updatedProduct.getImage());
                session.update(dbProduct);
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveNewProduct(Product product) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.save(product);
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteProduct(int productId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            Product dbProduct = session.get(Product.class, productId);
            if (dbProduct != null) {
                session.delete(dbProduct);
                System.out.println("Product deleted successfully.");
            } else {
                System.out.println("Product not found.");
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
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
            return session.createQuery("FROM Product", Product.class).list();
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

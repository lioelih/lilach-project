package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.*;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.Transaction;
import il.cshaifasweng.LoginUserDTO;

import javax.mail.MessagingException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.time.LocalDateTime;
import javax.mail.MessagingException;
import il.cshaifasweng.OCSFMediatorExample.server.EmailService;
import java.util.stream.Collectors;
import java.util.List;
import static java.util.stream.Collectors.toList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class SimpleServer extends AbstractServer {
    // central broadcast list and periodic scheduler
    private static final ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();
    private ScheduledExecutorService scheduler;
    private final ConcurrentMap<String, ConnectionToClient> onlineUsers = new ConcurrentHashMap<>(); // For online users
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
        // simple string channel for test/control; main flow uses Msg
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
                // authentication
                case "LOGIN" -> {
                    String[] credentials = (String[]) data;
                    String username = credentials[0];
                    String password = credentials[1];

                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        Query<User> query = session.createQuery("FROM User WHERE username = :u AND password = :p", User.class);
                        query.setParameter("u", username);
                        query.setParameter("p", password);
                        List<User> users = query.list();

                        User user = users.get(0);
                        if (users.isEmpty()) {
                            client.sendToClient(new Msg("LOGIN_FAILED", "Invalid credentials"));
                            System.out.println("User is not found" + user.getUsername());
                            return;
                        }

                        if (!user.isActive()) {
                            client.sendToClient(new Msg("LOGIN_FAILED", "Account is inactive"));
                            System.out.println("User is inative" + user.getUsername());
                            return;
                        }

                        if (onlineUsers.containsKey(username)) {
                            client.sendToClient(new Msg("LOGIN_FAILED", "User is currently logged in"));
                            System.out.println("User is already online" + user.getUsername());
                            return;
                        }

                        client.setInfo("username", username);
                        onlineUsers.put(username, client);
                        client.sendToClient(new Msg("LOGIN_SUCCESS", toLoginDTO(user)));
                        System.out.println("Sent to client:" + user.getUsername());
                    } catch (Exception e) {
                        e.printStackTrace();
                        client.sendToClient(new Msg("LOGIN_FAILED", "Server error"));
                    }
                }




                // registration
                case "REGISTER" -> {
                    String[] regData = (String[]) data;
                    String username   = regData[0];
                    String email      = regData[1];
                    String fullName   = regData[2];
                    String phone      = regData[3];
                    String id9        = regData[4];
                    String password   = regData[5];
                    String branchStr  = regData[6];

                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        Query<User> check = session.createQuery("FROM User WHERE username = :u OR email = :e");
                        check.setParameter("u", username);
                        check.setParameter("e", email);
                        if (!check.list().isEmpty()) {
                            client.sendToClient(new Msg("REGISTER_FAILED", "User already exists"));
                            return;
                        }

                        int branchId;
                        try {
                            branchId = Integer.parseInt(branchStr);
                        } catch (NumberFormatException e) {
                            client.sendToClient(new Msg("REGISTER_FAILED", "Invalid branch ID"));
                            return;
                        }

                        Branch selectedBranch = session.createQuery(
                                        "FROM Branch WHERE branch_id = :bid", Branch.class)
                                .setParameter("bid", branchId)
                                .uniqueResult();
                        if (selectedBranch == null) {
                            client.sendToClient(new Msg("REGISTER_FAILED", "Selected branch does not exist"));
                            return;
                        }

                        User u = new User();
                        u.setUsername(username);
                        u.setEmail(email);
                        u.setPhoneNumber(phone);
                        u.setFullName(fullName);
                        u.setPassword(password);
                        u.setIdentificationNumber(id9);   // <-- save 9-digit ID here
                        u.setBranch(selectedBranch);
                        u.setRole(User.Role.USER);
                        u.setActive(true);

                        session.beginTransaction();
                        try {
                            session.persist(u);
                            session.getTransaction().commit();
                            client.sendToClient(new Msg("REGISTER_SUCCESS", null));
                        } catch (Exception ex) {
                            session.getTransaction().rollback();
                            ex.printStackTrace();
                            client.sendToClient(new Msg("REGISTER_FAILED", "Server error during registration."));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        client.sendToClient(new Msg("REGISTER_FAILED", "Server error during registration."));
                    }
                }


                // logout bookkeeping
                case "LOGOUT" -> {
                    String username = (String) data;
                    onlineUsers.remove(username);
                }

                // user fetch with branch prefetch
                case "FETCH_USER" -> {
                    String username = (String) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery(
                                        "FROM User u JOIN FETCH u.branch WHERE u.username = :u", User.class)
                                .setParameter("u", username)
                                .uniqueResult();
                        client.sendToClient(new Msg("FETCH_USER", user));
                    }
                }

                // store/update payment info
                case "PAYMENT_INFO" -> {
                    String[] pData = (String[]) data;
                    String username = pData[0];
                    String card     = pData[1];
                    String exp      = pData[2];
                    String cvv      = pData[3];

                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username).uniqueResult();

                        if (user != null) {
                            session.beginTransaction();
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

                // read payment info to prefill form
                case "PAYMENT_PREFILL" -> {
                    String username = (String) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username).uniqueResult();
                        if (user != null) {
                            String[] cardData = new String[]{
                                    user.getCreditCardNumber(),
                                    user.getCreditCardExpiration(),
                                    user.getCreditCardSecurityCode()
                            };
                            client.sendToClient(new Msg("PAYMENT_PREFILL", cardData));
                        } else {
                            client.sendToClient(new Msg("PAYMENT_PREFILL", new String[]{"", "", ""}));
                        }
                    }
                }

                // vip activation/cancel
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
                            sendToAllClients(new Msg("USER_UPDATED", user));
                            sendToUser(username, new Msg("USER_UPDATED", user));
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
                            sendToAllClients(new Msg("USER_UPDATED", user));
                            sendToUser(username, new Msg("USER_UPDATED", user));
                        }
                    }
                }


                // catalog crud
                case "GET_CATALOG" -> {
                    List<Product> catalog = fetchCatalog();
                    client.sendToClient(new Msg("SENT_CATALOG", catalog));
                }

                case "ADD_PRODUCT" -> {
                    Product product = (Product) data;
                    saveNewProduct(product);
                    sendToAllClients(new Msg("PRODUCT_ADDED", fetchCatalog()));
                }

                case "UPDATE_PRODUCT" -> {
                    Product product = (Product) data;
                    updateFullProduct(product);
                    sendToAllClients(new Msg("PRODUCT_UPDATED", fetchCatalog()));
                }

                case "DELETE_PRODUCT" -> {
                    Product product = (Product) data;
                    deleteProduct(product.getId());
                    sendToAllClients(new Msg("PRODUCT_DELETED", fetchCatalog()));
                }

                // basket fetch respects non-privileged branch visibility
                case "FETCH_BASKET" -> {
                    String username = (String) data;
                    try ( Session session = HibernateUtil.getSessionFactory().openSession() ) {
                        User user = session.createQuery(
                                        "FROM User u JOIN FETCH u.branch WHERE u.username = :u",
                                        User.class)
                                .setParameter("u", username)
                                .uniqueResult();
                        if (user == null) {
                            client.sendToClient(new Msg("BASKET_FETCHED", List.of()));
                            return;
                        }

                        boolean privileged = user.isVIP()
                                || user.getRole().ordinal() >= User.Role.WORKER.ordinal();

                        List<Basket> basketItems;
                        if (privileged) {
                            basketItems = session.createQuery(
                                            """
                                            SELECT b
                                              FROM Basket b
                                         LEFT JOIN FETCH b.product
                                         LEFT JOIN FETCH b.customBouquet cb
                                         LEFT JOIN FETCH cb.items
                                             WHERE b.user.id = :uid
                                               AND b.order    IS NULL
                                            """, Basket.class)
                                    .setParameter("uid", user.getId())
                                    .list();
                        } else {
                            basketItems = session.createQuery(
                                            """
                                            SELECT DISTINCT b
                                              FROM Basket b
                                         LEFT JOIN FETCH b.customBouquet cb
                                         LEFT JOIN FETCH cb.items
                                         LEFT JOIN FETCH b.product p
                                         LEFT JOIN Storage st
                                                ON st.product.product_id = p.id
                                               AND st.branch.branch_id   = :bid
                                               AND st.quantity > 0
                                             WHERE b.user.id = :uid
                                               AND b.order IS NULL
                                               AND (b.customBouquet IS NOT NULL OR st.storage_id IS NOT NULL)
                                            """, Basket.class)
                                    .setParameter("uid", user.getId())
                                    .setParameter("bid", user.getBranch().getBranchId())
                                    .list();
                        }

                        client.sendToClient(new Msg("BASKET_FETCHED", basketItems));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        client.sendToClient(new Msg("BASKET_FETCHED", List.of()));
                    }
                }

                // add catalog product to basket (merges same product)
                case "ADD_TO_BASKET" -> {
                    Object[] arr = (Object[]) data;
                    String username = (String) arr[0];
                    Product product = (Product) arr[1];
                    System.out.println("Received ADD_TO_BASKET for " + username + " product ID: " + product.getId());
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username).uniqueResult();

                        Product dbProduct = session.get(Product.class, product.getId());

                        if (user != null && dbProduct != null) {
                            session.beginTransaction();

                            Basket existing = session.createQuery(
                                            "FROM Basket WHERE user.id = :userId AND product.id = :productId AND order IS NULL", Basket.class)
                                    .setParameter("userId", user.getId())
                                    .setParameter("productId", dbProduct.getId())
                                    .uniqueResult();

                            if (existing != null) {
                                existing.setAmount(existing.getAmount() + 1);
                                existing.setPrice(existing.getAmount() * dbProduct.getPrice());
                                session.merge(existing);
                            } else {
                                Basket basket = new Basket();
                                basket.setUser(user);
                                basket.setProduct(dbProduct);
                                basket.setAmount(1);
                                basket.setPrice(dbProduct.getPrice());
                                session.persist(basket);
                            }

                            session.getTransaction().commit();
                            client.sendToClient(new Msg("BASKET_UPDATED", null));
                        } else {
                            System.err.println("User or Product not found");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                // add x amount (bundle etc.)
                case "ADD_TO_BASKET_X_AMOUNT" -> {
                    Object[] arr = (Object[]) data;
                    String username = (String) arr[0];
                    Product product = (Product) arr[1];
                    int amount = (int) arr[2];
                    System.out.println("Received ADD_TO_BASKET for " + username + " product ID: " + product.getId());
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username).uniqueResult();

                        Product dbProduct = session.get(Product.class, product.getId());

                        if (user != null && dbProduct != null) {
                            session.beginTransaction();

                            Basket existing = session.createQuery(
                                            "FROM Basket WHERE user.id = :userId AND product.id = :productId AND order IS NULL", Basket.class)
                                    .setParameter("userId", user.getId())
                                    .setParameter("productId", dbProduct.getId())
                                    .uniqueResult();

                            if (existing != null) {
                                existing.setAmount(existing.getAmount() + amount);
                                existing.setPrice(existing.getAmount() * dbProduct.getPrice());
                                session.merge(existing);
                            } else {
                                Basket basket = new Basket();
                                basket.setUser(user);
                                basket.setProduct(dbProduct);
                                basket.setAmount(amount);
                                basket.setPrice(dbProduct.getPrice());
                                session.persist(basket);
                            }

                            session.getTransaction().commit();
                            client.sendToClient(new Msg("BASKET_UPDATED", null));
                        } else {
                            System.err.println("User or Product not found");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                // remove item from basket
                case "REMOVE_BASKET_ITEM" -> {
                    Basket basketItem = (Basket) data;
                    System.out.println("[Server] REMOVE_BASKET_ITEM received with ID: " + basketItem.getId());

                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        session.beginTransaction();

                        Basket b = session.get(Basket.class, basketItem.getId());
                        if (b != null) {
                            session.remove(b);
                            System.out.println("[Server] Removed basket item from DB: " + b);
                        } else {
                            System.out.println("[Server] No basket item found for ID: " + basketItem.getId());
                        }

                        session.getTransaction().commit();
                        client.sendToClient(new Msg("BASKET_UPDATED", null));
                    } catch (Exception e) {
                        System.err.println("[Server] Error removing basket item: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // update basket quantity/price
                case "UPDATE_BASKET_AMOUNT" -> {
                    Basket updatedItem = (Basket) data;
                    System.out.println("[Server] Updating amount for basket ID: " + updatedItem.getId());
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        session.beginTransaction();
                        Basket existing = session.get(Basket.class, updatedItem.getId());
                        if (existing != null) {
                            existing.setAmount(updatedItem.getAmount());
                            existing.setPrice(updatedItem.getPrice());
                            session.update(existing);
                        }
                        session.getTransaction().commit();
                        client.sendToClient(new Msg("BASKET_UPDATED", null));
                    }
                }

                // sales crud/load
                case "GET_SALES" -> {
                    List<Sale> sales = fetchSales();
                    System.out.println("Sending sales to client");
                    client.sendToClient(new Msg("SENT_SALES", sales));
                }

                case "ADD_SALE" -> {
                    Sale newSale = (Sale) data;
                    saveNewSale(newSale);
                    sendToAllClients(new Msg("SALE_ADDED", fetchSales()));
                }

                case "UPDATE_SALE" -> {
                    Sale updatedSale = (Sale) data;
                    updateSale(updatedSale);
                    sendToAllClients(new Msg("SALE_UPDATED", fetchSales()));
                }

                case "DELETE_SALE" -> {
                    Sale targetSale = (Sale) data;
                    deleteSale(targetSale.getId());
                    sendToAllClients(new Msg("SALE_DELETED", fetchSales()));
                }

                // order creation: validate stock, compute discounts, persist
                case "NEW_ORDER" -> {
                    OrderDTO dto = (OrderDTO) data;
                    boolean ok = false;

                    double totalPrice       = 0;
                    double vipDiscount      = 0;
                    double deliveryFee      = 0;
                    double compensationUsed = 0;
                    int    newOrderId       = 0;

                    try ( Session s = HibernateUtil.getSessionFactory().openSession() ) {
                        Transaction tx = s.beginTransaction();

                        // 1. Fetch user
                        User user = s.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", dto.getUsername())
                                .uniqueResult();
                        if (user == null) {
                            client.sendToClient(new Msg("ORDER_FAIL", "Unknown user"));
                            return;
                        }

                        // 2. Fetch *all* basket items (real products + custom bouquets)
                        List<Basket> basketItems = s.createQuery("""
            SELECT b
              FROM Basket b
              LEFT JOIN FETCH b.product
              LEFT JOIN FETCH b.customBouquet
             WHERE b.id    IN (:ids)
               AND b.user  = :u
               AND b.order IS NULL
        """, Basket.class)
                                .setParameter("ids", dto.getBasketIds())
                                .setParameter("u",   user)
                                .list();
                        if (basketItems.size() != dto.getBasketIds().size()) {
                            client.sendToClient(new Msg("ORDER_FAIL", "Basket mismatch"));
                            return;
                        }

                        // 3. If PICKUP, validate & deduct stock (only real products)
                        int branchId = -1;
                        if ("PICKUP".equals(dto.getFulfilType())) {
                            if (dto.getFulfilInfo() == null || dto.getFulfilInfo().isBlank()) {
                                client.sendToClient(new Msg("ORDER_FAIL", "No pick‑up branch selected"));
                                return;
                            }
                            try {
                                branchId = Integer.parseInt(dto.getFulfilInfo());
                            } catch (NumberFormatException ex) {
                                client.sendToClient(new Msg("ORDER_FAIL", "Malformed branch id"));
                                return;
                            }

                            // build per‑product request map (skip custom bouquets)
                            Map<Integer,Integer> requested = new HashMap<>();
                            for (Basket b : basketItems) {
                                if (b.getProduct() != null) {
                                    requested.merge(
                                            b.getProduct().getId(),
                                            b.getAmount(),
                                            Integer::sum
                                    );
                                }
                            }

                            // availability check
                            List<Object[]> chk = checkBranchStock(s, branchId, requested);
                            for (Object[] row : chk) {
                                Integer have = (Integer) row[2];
                                int     want = (Integer) row[1];
                                if (have == null || have < want) {
                                    client.sendToClient(new Msg("ORDER_FAIL",
                                            "Not enough items in selected pick‑up branch!"));
                                    tx.rollback();
                                    return;
                                }
                            }

                            // deduct across all Storage rows
                            for (Object[] row : chk) {
                                int pid  = (Integer) row[0];
                                int want = (Integer) row[1];

                                List<Storage> rows = s.createQuery("""
                    FROM Storage
                    WHERE product.product_id = :pid
                      AND branch.branch_id   = :bid
                """, Storage.class)
                                        .setParameter("pid", pid)
                                        .setParameter("bid", branchId)
                                        .list();

                                int remaining = want;
                                for (Storage st : rows) {
                                    int avail    = st.getQuantity();
                                    int toRemove = Math.min(avail, remaining);
                                    st.setQuantity(avail - toRemove);
                                    remaining   -= toRemove;
                                    s.merge(st);
                                    if (remaining <= 0) break;
                                }
                            }

                        } else if ("DELIVERY".equals(dto.getFulfilType())) {
                            // delivery must have an address
                            if (dto.getFulfilInfo() == null || dto.getFulfilInfo().isBlank()) {
                                client.sendToClient(new Msg("ORDER_FAIL", "No delivery address provided"));
                                tx.rollback();
                                return;
                            }
                        }

                        // 4. Compute discounts & fees

                        // 4a) Sale discount (only on real products)
                        double saleDiscount = 0;
                        try ( Session saleSession = HibernateUtil.getSessionFactory().openSession() ) {
                            List<Sale> sales = saleSession.createQuery("FROM Sale", Sale.class).list();
                            for (Sale sale : sales) {
                                sale.setProductIds(
                                        saleSession.createNativeQuery(
                                                        "SELECT product_id FROM sale_products WHERE sale_id = :saleId",
                                                        Integer.class
                                                )
                                                .setParameter("saleId", sale.getId())
                                                .getResultList()
                                );
                            }
                            // filter out any basket row with a null product
                            List<Basket> productLines = basketItems.stream()
                                    .filter(b -> b.getProduct() != null)
                                    .collect(Collectors.toList());

                            saleDiscount = Sale.calculateTotalDiscount(productLines, sales);
                        }

                        // 4b) Subtotal (all lines, including custom bouquets)
                        double subtotal = basketItems.stream()
                                .mapToDouble(Basket::getPrice)
                                .sum();

                        // 4c) VIP discount *only* if post‑sale ≥ 50 NIS
                        double afterSale = subtotal - saleDiscount;
                        if (user.isVIP() && afterSale >= 50.0) {
                            vipDiscount = afterSale * 0.10;
                        }

                        // 4d) Flat delivery fee last
                        if ("DELIVERY".equals(dto.getFulfilType())) {
                            deliveryFee = 10.0;
                        }

                        // 4e) Grand total before compensation
                        totalPrice = subtotal - saleDiscount - vipDiscount + deliveryFee;

                        // 5. Apply compensation credit if requested
                        if (dto.isUseCompensation()) {
                            compensationUsed = Math.min(user.getCompensationTab(), totalPrice);
                            totalPrice      -= compensationUsed;
                            user.setCompensationTab(user.getCompensationTab() - compensationUsed);
                            s.merge(user);
                        }

                        // 6. Create Order entity, link baskets
                        Order order = new Order();
                        order.setUser(user);
                        order.setStatus(Order.STATUS_PENDING);
                        order.setTotalPrice(totalPrice);
                        order.setCompensationUsed(compensationUsed);
                        order.setDeadline(dto.getDeadline());
                        order.setRecipient(dto.getRecipient());
                        order.setGreeting(dto.getGreeting());
                        if ("PICKUP".equals(dto.getFulfilType())) {
                            Branch b = s.get(Branch.class, branchId);
                            order.setBranch(b);
                            order.setDelivery(null);
                        } else {
                            order.setBranch(null);
                            order.setDelivery(dto.getFulfilInfo());
                        }
                        s.persist(order);

                        for (Basket b : basketItems) {
                            b.setOrder(order);
                            s.merge(b);
                        }

                        newOrderId = order.getOrderId();
                        tx.commit();
                        ok = true;

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    // 7. Reply to client
                    Map<String,Object> result = Map.of(
                            "orderId",          newOrderId,
                            "totalPrice",       totalPrice,
                            "vipDiscount",      vipDiscount,
                            "deliveryFee",      deliveryFee,
                            "compensationUsed", compensationUsed
                    );
                    client.sendToClient(new Msg(ok ? "ORDER_OK" : "ORDER_FAIL", result));
                }

                // card presence check
                case "HAS_CARD" -> {
                    String username = (String) data;

                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery(
                                        "FROM User WHERE username = :u", User.class)
                                .setParameter("u", username)
                                .uniqueResult();

                        boolean hasCard =
                                user != null
                                        && user.getCreditCardNumber() != null
                                        && !user.getCreditCardNumber().isBlank();

                        client.sendToClient(new Msg("HAS_CARD", hasCard));
                    }
                }

                // branches list (trim heavy relations)
                case "LIST_BRANCHES" -> {
                    System.out.println("[Server] Listing BRANCHES");
                    try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                        List<Branch> list = s.createQuery("FROM Branch", Branch.class).list();

                        System.out.println("[Server] Branches found in DB:");
                        for (Branch b : list) {
                            System.out.println("[Server] Branch: " + b.getName() + ", ID: " + b.getBranchId());
                        }

                        list.forEach(b -> {
                            b.getStockLines().size();
                            b.setManager(null);
                            b.getStockLines().clear();
                        });

                        System.out.println("[Server] Sending BRANCHES_OK with " + list.size() + " branches");
                        client.sendToClient(new Msg("BRANCHES_OK", list));
                    } catch (Exception ex) {
                        System.err.println("[Server] Failed to send branches: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }

                // stock view per branch (null means all)
                case "STOCK_BY_BRANCH" -> {
                    Integer branchId = (Integer) data;

                    String hql = """
                    SELECT new il.cshaifasweng.StockLineDTO(
                        st.storage_id,
                        p.product_id,
                        p.product_name,
                        p.product_type,
                        p.product_price,
                        p.product_image,
                        b.branch_id,
                        b.branch_name,
                        st.quantity)
                    FROM Storage st
                    JOIN st.product p
                    JOIN st.branch b
                    WHERE (:bid IS NULL OR b.branch_id = :bid)
                      AND st.quantity > 0
    """;


                    try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                        List<StockLineDTO> rows = s
                                .createQuery(hql, StockLineDTO.class)
                                .setParameter("bid", branchId)
                                .list();

                        client.sendToClient(new Msg("STOCK_OK", rows));
                    }
                }

                // add or increase stock (payload: [productId, branchId, qty])
                case "ADD_STOCK" -> {
                    System.out.println("[Server] Adding stock line");
                    int[] arr  = (int[]) data;
                    int pid    = arr[0];
                    int bid    = arr[1];
                    int qtyAdd = arr[2];

                    try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                        s.beginTransaction();

                        Storage st = s.createQuery("""
            FROM Storage
            WHERE product.product_id = :pid
              AND branch.branch_id   = :bid
            """, Storage.class)
                                .setParameter("pid", pid)
                                .setParameter("bid", bid)
                                .uniqueResult();

                        if (st == null) {
                            st = new Storage();
                            st.setProduct(s.get(Product.class, pid));
                            st.setBranch (s.get(Branch.class,  bid));
                            st.setQuantity(qtyAdd);
                            s.persist(st);
                        } else {
                            st.setQuantity(st.getQuantity() + qtyAdd);
                            s.merge(st);
                        }

                        s.getTransaction().commit();
                        client.sendToClient(new Msg("ADD_STOCK_OK", null));
                    }
                }

                // fetch qty for a product in a branch (payload: { productId, branchId })
                case "FETCH_STOCK_SINGLE" -> {
                    Object[] arr   = (Object[]) data;
                    int pid        = (Integer) arr[0];
                    int bid        = (Integer) arr[1];

                    try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                        Integer qty = (Integer) s.createQuery("""
            SELECT st.quantity
            FROM Storage st
            WHERE st.product.product_id = :pid
              AND st.branch.branch_id   = :bid
            """, Integer.class)
                                .setParameter("pid", pid)
                                .setParameter("bid", bid)
                                .setMaxResults(1)
                                .uniqueResult();

                        client.sendToClient(new Msg("STOCK_SINGLE_OK",
                                qty == null ? 0 : qty));
                    }
                }

                // order queries (scope-based)
                case "FETCH_ORDERS" -> {
                    Object[] payload = (Object[]) data;
                    String username = (String) payload[0];
                    String scope = (String) payload[1];

                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username)
                                .uniqueResult();

                        if (user == null) {
                            client.sendToClient(new Msg("FETCH_ORDERS_OK", List.of()));
                            return;
                        }

                        List<Order> orders;
                        String hql;

                        if (scope.equals("MINE")) {
                            hql = "FROM Order o WHERE o.user.id = :uid ORDER BY o.id DESC";
                            orders = session.createQuery(hql, Order.class)
                                    .setParameter("uid", user.getId())
                                    .list();
                        } else if (scope.equals("ALL_USERS")) {
                            hql = "FROM Order o WHERE o.user.branch = :branch ORDER BY o.id DESC";
                            orders = session.createQuery(hql, Order.class)
                                    .setParameter("branch", user.getBranch())
                                    .list();
                        } else if (scope.equals("ALL")) {
                            hql = "FROM Order o ORDER BY o.id DESC";
                            orders = session.createQuery(hql, Order.class).list();
                        } else {
                            client.sendToClient(new Msg("FETCH_ORDERS_OK", List.of()));
                            return;
                        }

                        List<OrderDisplayDTO> displayList = new ArrayList<>();
                        for (Order o : orders) {
                            String fulfilment;
                            if (o.getDelivery() != null && !o.getDelivery().isBlank()) {
                                fulfilment = "Delivery to: " + o.getDelivery();
                            } else if (o.getBranch() != null) {
                                fulfilment = "Pickup from: " + o.getBranch().getName();
                            } else {
                                fulfilment = "Unknown";
                            }

                            double totalPrice = o.getTotalPrice();

                            displayList.add(new OrderDisplayDTO(
                                    o.getOrderId(),
                                    o.getUser().getUsername(),
                                    fulfilment,
                                    totalPrice,
                                    o.getDeadline(),
                                    o.getRecipient(),
                                    o.getGreeting(),
                                    o.getStatus(),
                                    o.getCompensationUsed()
                            ));
                        }

                        client.sendToClient(new Msg("FETCH_ORDERS_OK", displayList));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        client.sendToClient(new Msg("FETCH_ORDERS_OK", List.of()));
                    }
                }

                // mark order received + email notification
                case "MARK_ORDER_RECEIVED" -> {
                    int orderId = (int) data;
                    try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                        Transaction tx = s.beginTransaction();

                        Order order = s.get(Order.class, orderId);
                        if (order != null && order.getStatus() != Order.STATUS_RECEIVED) {
                            order.setStatus(Order.STATUS_RECEIVED);
                            s.merge(order);
                        }
                        tx.commit();

                        User user = order.getUser();
                        String toAddress     = user.getEmail();
                        String username      = user.getUsername();
                        String recipientRaw  = order.getRecipient();
                        String recipientName = recipientRaw != null
                                ? recipientRaw.split("\\s*\\(")[0].trim()
                                : "";
                        boolean toSelf       = recipientName.equalsIgnoreCase(user.getFullName());

                        String fulfilType, fulfilInfo;
                        if (order.getBranch() != null) {
                            fulfilType = "PICKUP";
                            fulfilInfo = order.getBranch().getName();
                        } else {
                            fulfilType = "DELIVERY";
                            fulfilInfo = order.getDelivery();
                        }

                        @SuppressWarnings("unchecked")
                        List<Basket> lines = s.createQuery("""
    SELECT b
      FROM Basket b
 LEFT JOIN FETCH b.product
 LEFT JOIN FETCH b.customBouquet cb
 LEFT JOIN FETCH cb.items i
 LEFT JOIN FETCH i.product
     WHERE b.order.orderId = :oid
""", Basket.class)
                                .setParameter("oid", orderId)
                                .list();

                        List<String> productNames = lines.stream().map(b -> {
                            if (b.getProduct() != null) {
                                return b.getAmount() + " x " + b.getProduct().getName();
                            }
                            CustomBouquet cb = b.getCustomBouquet();
                            StringBuilder sb = new StringBuilder();
                            sb.append(b.getAmount())
                                    .append(" x Custom: ")
                                    .append(cb.getName())
                                    .append(" (Style: ").append(cb.getStyle());
                            if (cb.getPot() != null) {
                                sb.append(", Pot: ").append(cb.getPot());
                            }
                            List<String> parts = cb.getItems().stream()
                                    .filter(it -> it.getQuantity() > 0)
                                    .map(it -> it.getProduct().getName() + " x " + it.getQuantity())
                                    .toList();
                            if (!parts.isEmpty()) {
                                sb.append(", Flowers: ").append(String.join(", ", parts));
                            }
                            sb.append(")");
                            return sb.toString();
                        }).toList();
                        double totalPaid = order.getTotalPrice();

                        try {
                            EmailService.sendOrderReceivedEmail(
                                    toAddress,
                                    username,
                                    orderId,
                                    toSelf,
                                    recipientName,
                                    fulfilType,
                                    fulfilInfo,
                                    productNames,
                                    totalPaid
                            );
                            System.out.println("[Server] Sent receive-notification email to " + toAddress);
                        } catch (MessagingException mex) {
                            mex.printStackTrace();
                            System.err.println("[Server] Failed to send e-mail for order " + orderId);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    client.sendToClient(new Msg("MARK_ORDER_RECEIVED_OK", List.of()));
                }

                // full order details for receipt view
                case "FETCH_ORDER_PRODUCTS" -> {
                    int orderId = (int) massage.getData();
                    try ( Session s = HibernateUtil.getSessionFactory().openSession() ) {
                        Order o = s.get(Order.class, orderId);

                        @SuppressWarnings("unchecked")
                        List<Basket> items = s.createQuery("""
            SELECT b
              FROM Basket b
         LEFT JOIN FETCH b.product
         LEFT JOIN FETCH b.customBouquet cb
         LEFT JOIN FETCH cb.items i
         LEFT JOIN FETCH i.product
             WHERE b.order.orderId = :oid
        """, Basket.class)
                                .setParameter("oid", orderId)
                                .list();

                        List<OrderDetailsDTO.Line> lines   = new ArrayList<>();
                        double                       subtotal = 0;
                        for (Basket b : items) {
                            String name;
                            if (b.getProduct() != null) {
                                name = b.getProduct().getName();
                            } else {
                                CustomBouquet cb = b.getCustomBouquet();
                                StringBuilder   sb = new StringBuilder();
                                sb.append(cb.getName()).append(" (");
                                sb.append("Style: ").append(cb.getStyle());
                                if (cb.getPot() != null) {
                                    sb.append(", Pot: ").append(cb.getPot());
                                }
                                if (!cb.getItems().isEmpty()) {
                                    sb.append(", Flowers: ");
                                    List<String> parts = cb.getItems().stream()
                                            .filter(it -> it.getQuantity() > 0)
                                            .map(it -> it.getProduct().getName() + " x " + it.getQuantity())
                                            .collect(Collectors.toList());
                                    sb.append(String.join(", ", parts));
                                }
                                sb.append(")");
                                name = sb.toString();
                            }

                            int    qty   = b.getAmount();
                            double price = b.getPrice();
                            lines.add(new OrderDetailsDTO.Line(name, qty, price));
                            subtotal += price;
                        }

                        double saleDiscount = 0;
                        try ( Session saleSession = HibernateUtil.getSessionFactory().openSession() ) {
                            List<Sale> sales = saleSession.createQuery("FROM Sale", Sale.class).list();
                            for (Sale sale : sales) {
                                sale.setProductIds(
                                        saleSession.createNativeQuery(
                                                        "SELECT product_id FROM sale_products WHERE sale_id = :saleId",


































                                                        Integer.class
                                                )
                                                .setParameter("saleId", sale.getId())
                                                .getResultList()
                                );
                            }
                            List<Basket> productLines = items.stream()
                                    .filter(x -> x.getProduct() != null)
                                    .collect(Collectors.toList());
                            saleDiscount = Sale.calculateTotalDiscount(productLines, sales);
                        }

                        double afterSale   = subtotal - saleDiscount;
                        boolean isVip      = o.getUser().isVIP();
                        double vipDiscount = (isVip && afterSale >= 50.0)
                                ? afterSale * 0.10
                                : 0.0;

                        double deliveryFee = (o.getDelivery() != null && !o.getDelivery().isBlank())
                                ? 10.0
                                : 0.0;

                        double total    = o.getTotalPrice();
                        double compUsed = o.getCompensationUsed();

                        OrderDetailsDTO dto = new OrderDetailsDTO(
                                lines,
                                subtotal,
                                saleDiscount,
                                vipDiscount,
                                deliveryFee,
                                total,
                                compUsed
                        );
                        client.sendToClient(new Msg("FETCH_ORDER_PRODUCTS_OK", dto));

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        client.sendToClient(new Msg("FETCH_ORDER_PRODUCTS_FAIL", "Could not load order details"));
                    }
                }

                // users listing for admin views
                case "FETCH_ALL_USERS" -> {
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        List<User> users = session.createQuery("FROM User", User.class).list();
                        List<Map<String, Object>> results = new ArrayList<>();

                        for (User user : users) {
                            Double total = session.createQuery(
                                            "SELECT SUM(o.totalPrice) FROM Order o WHERE o.user.id = :uid", Double.class)
                                    .setParameter("uid", user.getId())
                                    .uniqueResultOptional()
                                    .orElse(0.0);

                            Map<String, Object> map = new HashMap<>();
                            map.put("id", user.getId());
                            map.put("username", user.getUsername());
                            map.put("email", user.getEmail());
                            map.put("phone", user.getPhoneNumber());
                            map.put("role", user.getRole().toString());
                            map.put("active", user.isActive());
                            map.put("totalSpent", total);
                            map.put("branchName", user.getBranch().getBranchName());
                            map.put("password", user.getPassword());
                            map.put("isVIP", user.isVIP());
                            results.add(map);
                        }

                        client.sendToClient(new Msg("FETCH_ALL_USERS_OK", results));
                    }
                }

                // account state changes
                case "FREEZE_USER" -> {
                    int targetId = (int) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        session.beginTransaction();
                        User user = session.get(User.class, targetId);
                        if (user != null) {
                            user.setActive(false);
                            session.merge(user);
                        }
                        session.getTransaction().commit();

                        client.sendToClient(new Msg("USER_FREEZE_OK", targetId));
                        if (user != null) {
                            sendToAllClients(new Msg("USER_UPDATED", user));
                            sendToUser(user.getUsername(),
                                    new Msg("ACCOUNT_FROZEN", "Your account has been frozen. Contact support for more information."));
                            ConnectionToClient c = onlineUsers.remove(user.getUsername());
                            if (c != null) try { c.close(); } catch (IOException ignored) {}
                        }
                    }
                }


                case "UNFREEZE_USER" -> {
                    int targetId = (int) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        session.beginTransaction();
                        User user = session.get(User.class, targetId);
                        if (user != null) {
                            user.setActive(true);
                            session.merge(user);
                        }
                        session.getTransaction().commit();

                        client.sendToClient(new Msg("USER_UNFREEZE_OK", targetId));
                        if (user != null) {
                            sendToAllClients(new Msg("USER_UPDATED", user));
                            sendToUser(user.getUsername(), new Msg("USER_UPDATED", user));
                        }
                    }
                }


                // change role
                case "CHANGE_ROLE" -> {
                    Map<String, Object> map = (Map<String, Object>) data;
                    int userId = (int) map.get("id");
                    String newRole = (String) map.get("role");

                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        session.beginTransaction();
                        User user = session.get(User.class, userId);
                        if (user != null) {
                            user.setRole(User.Role.valueOf(newRole));
                            session.merge(user);
                        }
                        session.getTransaction().commit();

                        client.sendToClient(new Msg("CHANGE_ROLE_OK", userId));
                        if (user != null) {
                            sendToAllClients(new Msg("USER_UPDATED", user));
                            sendToUser(user.getUsername(), new Msg("USER_UPDATED", user));
                        }
                    }
                }


                // update user (validates conflicts, updates branch/role/etc.)
                case "UPDATE_USER" -> {
                    Map<String, Object> updateData = (Map<String, Object>) data;
                    int userId = (int) updateData.get("id");
                    String newUsername = (String) updateData.get("username");
                    String newEmail = (String) updateData.get("email");
                    String newPhone = (String) updateData.get("phone");
                    String newRole = (String) updateData.get("role");
                    Boolean newActive = (Boolean) updateData.get("active");
                    String newBranchName = (String) updateData.get("branchName");
                    String newPassword = (String) updateData.get("password");
                    Boolean newVip = (Boolean) updateData.get("isVIP");

                    try ( Session session = HibernateUtil.getSessionFactory().openSession() ) {
                        session.beginTransaction();
                        User user = session.get(User.class, userId);
                        if (user == null) {
                            client.sendToClient(new Msg("UPDATE_USER_FAILED", "User not found"));
                            return;
                        }

                        String oldUsername = user.getUsername();

                        List<User> conflicts = session.createQuery(
                                        "FROM User WHERE (username = :username OR email = :email) AND id != :id",
                                        User.class)
                                .setParameter("username", newUsername)
                                .setParameter("email", newEmail)
                                .setParameter("id", userId)
                                .list();
                        if (!conflicts.isEmpty()) {
                            client.sendToClient(new Msg("UPDATE_USER_FAILED", "Username or email already taken by another user"));
                            session.getTransaction().rollback();
                            return;
                        }

                        user.setUsername(newUsername);
                        user.setEmail(newEmail);
                        user.setPhoneNumber(newPhone);
                        user.setRole(User.Role.valueOf(newRole));
                        if (newActive != null) user.setActive(newActive);
                        if (newVip != null)    user.setVIP(newVip);
                        if (newPassword != null && !newPassword.isBlank()) user.setPassword(newPassword);
                        if (newBranchName != null && !newBranchName.isBlank()) {
                            Branch branch = session.createQuery(
                                            "FROM Branch WHERE branch_name = :name", Branch.class)
                                    .setParameter("name", newBranchName)
                                    .uniqueResult();
                            if (branch == null) {
                                client.sendToClient(new Msg("UPDATE_USER_FAILED", "Branch not found"));
                                session.getTransaction().rollback();
                                return;
                            }
                            user.setBranch(branch);
                        }

                        session.merge(user);
                        session.getTransaction().commit();

                        client.sendToClient(new Msg("UPDATE_USER_OK", null));

                        // move connection key if username changed
                        if (!oldUsername.equals(newUsername)) {
                            ConnectionToClient c = onlineUsers.remove(oldUsername);
                            if (c != null) {
                                c.setInfo("username", newUsername);
                                onlineUsers.put(newUsername, c);
                            }
                        }

                        sendToAllClients(new Msg("USER_UPDATED", user));
                        sendToUser(newUsername, new Msg("USER_UPDATED", user));

                        if (newActive != null && !newActive) {
                            sendToUser(newUsername,
                                    new Msg("ACCOUNT_FROZEN", "Your account has been frozen. Contact support for more information."));
                            ConnectionToClient c = onlineUsers.remove(newUsername);
                            if (c != null) try { c.close(); } catch (IOException ignored) {}
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        client.sendToClient(new Msg("UPDATE_USER_FAILED", "Error updating user"));
                    }
                }


                // greeting text update on order
                case "UPDATE_GREETING" -> {
                    Map<String, Object> payload = (Map<String, Object>) data;
                    int orderId = (Integer) payload.get("orderId");
                    String greeting = (String) payload.get("greeting");
                    try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                        Transaction tx = s.beginTransaction();
                        Order o = s.get(Order.class, orderId);
                        o.setGreeting(greeting);
                        s.merge(o);
                        tx.commit();
                    }
                }

                // cancellation with time-based refund
                case "CANCEL_ORDER" -> {
                    int orderId = (int) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        Transaction tx = session.beginTransaction();
                        Order o = session.get(Order.class, orderId);

                        if (o.isReceived()) {
                            tx.rollback();
                            client.sendToClient(new Msg("CANCEL_FAIL", "Already received"));
                            return;
                        }

                        LocalDateTime now = LocalDateTime.now();
                        if (now.isAfter(o.getDeadline())) {
                            tx.rollback();
                            client.sendToClient(new Msg("CANCEL_FAIL", "Past deadline"));
                            return;
                        }

                        long minsLeft = Duration.between(now, o.getDeadline()).toMinutes();
                        double pct = minsLeft >= 180 ? 1.0
                                : minsLeft >= 60  ? 0.5
                                :                   0.0;
                        double refundAmt = o.getTotalPrice() * pct;

                        o.setStatus(Order.STATUS_CANCELLED);
                        session.merge(o);
                        User u = o.getUser();
                        u.setCompensationTab(u.getCompensationTab() + refundAmt);
                        session.merge(u);

                        tx.commit();

                        Map<String,Object> payload = Map.of(
                                "orderId",  orderId,
                                "refundPct", pct,
                                "refundAmt", refundAmt
                        );
                        client.sendToClient(new Msg("CANCEL_OK", payload));

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        client.sendToClient(new Msg("CANCEL_FAIL", "Server error"));
                    }
                }

                // custom bouquets: list/create/update/add-to-basket
                case "LIST_CUSTOM_BOUQUETS" -> {
                    String username = (String) data;
                    try ( Session s = HibernateUtil.getSessionFactory().openSession() ) {
                        List<CustomBouquet> list = s.createQuery(
                                        "FROM CustomBouquet cb JOIN FETCH cb.items WHERE cb.user.username = :u",
                                        CustomBouquet.class)
                                .setParameter("u", username)
                                .list();
                        List<CustomBouquetDTO> dtoList = new ArrayList<>();
                        for (CustomBouquet cb : list) {
                            List<CustomBouquetItemDTO> items = cb.getItems().stream()
                                    .map(i -> new CustomBouquetItemDTO(
                                            cb.getId(),
                                            i.getProduct().getId(),
                                            i.getQuantity()))
                                    .toList();
                            dtoList.add(new CustomBouquetDTO(
                                    cb.getId(),
                                    cb.getUser().getUsername(),
                                    cb.getName(),
                                    cb.getStyle(),
                                    cb.getDominantColor(),
                                    cb.getPot(),
                                    cb.getTotalPrice(),
                                    cb.getCreatedAt(),
                                    items
                            ));
                        }
                        client.sendToClient(new Msg("LIST_CUSTOM_BOUQUETS_OK", dtoList));
                    }
                }

                case "CREATE_CUSTOM_BOUQUET" -> {
                    CustomBouquetDTO dto = (CustomBouquetDTO) data;
                    Integer generatedId = null;
                    try ( Session s = HibernateUtil.getSessionFactory().openSession() ) {
                        Transaction tx = s.beginTransaction();

                        CustomBouquet cb = new CustomBouquet();
                        User u = s.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", dto.getUsername())
                                .uniqueResult();
                        cb.setUser(u);
                        cb.setName(dto.getName());
                        cb.setStyle(dto.getStyle());
                        cb.setDominantColor(dto.getDominantColor());
                        cb.setPot(dto.getPot());
                        cb.setTotalPrice(dto.getTotalPrice());
                        for (CustomBouquetItemDTO itemDto : dto.getItems()) {
                            CustomBouquetItem item = new CustomBouquetItem();
                            item.setProduct(s.get(Product.class, itemDto.getProductId()));
                            item.setQuantity(itemDto.getQuantity());
                            cb.addItem(item);
                        }

                        s.persist(cb);
                        tx.commit();
                        generatedId = cb.getId();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    client.sendToClient(new Msg("CREATE_CUSTOM_BOUQUET_OK", generatedId));
                }

                case "UPDATE_CUSTOM_BOUQUET" -> {
                    CustomBouquetDTO dto = (CustomBouquetDTO) data;
                    try ( var sess = HibernateUtil.getSessionFactory().openSession()) {
                        var tx = sess.beginTransaction();
                        CustomBouquet cb = sess.get(CustomBouquet.class, dto.getId());
                        cb.setName(dto.getName());
                        cb.setStyle(dto.getStyle());
                        cb.setPot(dto.getPot());
                        cb.setTotalPrice(dto.getTotalPrice());
                        cb.getItems().clear();
                        for (var itemDto : dto.getItems()) {
                            var item = new CustomBouquetItem();
                            item.setProduct(sess.get(Product.class, itemDto.getProductId()));
                            item.setQuantity(itemDto.getQuantity());
                            cb.addItem(item);
                        }
                        sess.merge(cb);
                        tx.commit();
                    }
                    client.sendToClient(new Msg("UPDATE_CUSTOM_BOUQUET_OK", dto.getId()));
                }

                case "ADD_CUSTOM_TO_BASKET" -> {
                    Object[] arr = (Object[]) data;
                    String username = (String) arr[0];
                    Integer customId = (Integer) arr[1];
                    int amount = (int) arr[2];

                    try ( Session s = HibernateUtil.getSessionFactory().openSession() ) {
                        Transaction tx = s.beginTransaction();
                        User u = s.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username)
                                .uniqueResult();
                        CustomBouquet cb = s.get(CustomBouquet.class, customId);

                        Basket existing = s.createQuery("""
            FROM Basket b
            WHERE b.user = :u
              AND b.customBouquet.id = :cid
              AND b.order IS NULL
        """, Basket.class)
                                .setParameter("u", u)
                                .setParameter("cid", customId)
                                .uniqueResult();

                        if (existing != null) {
                            existing.setAmount(existing.getAmount() + amount);
                            existing.setPrice(existing.getAmount() * cb.getTotalPrice());
                            s.merge(existing);
                        } else {
                            Basket b = new Basket();
                            b.setUser(u);
                            b.setCustomBouquet(cb);
                            b.setAmount(amount);
                            b.setPrice(cb.getTotalPrice());
                            s.persist(b);
                        }
                        tx.commit();
                        client.sendToClient(new Msg("BASKET_UPDATED", null));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                // fallback
                default -> client.sendToClient(new Msg("ERROR", "Unknown action: " + action));
            }
        }
    }

    // basic product crud helpers
    private void saveNewProduct(Product product) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.save(product);
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
                dbProduct.setImage(updatedProduct.getImage());
                session.update(dbProduct);
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteProduct(int productId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            int removed = session.createQuery(
                            "DELETE FROM Basket b WHERE b.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();
            System.out.println("[Server] Removed " + removed + " basket rows");

            Product dbProduct = session.get(Product.class, productId);
            if (dbProduct != null) {
                session.delete(dbProduct);
                System.out.println("[Server] Product deleted successfully.");
            } else {
                System.out.println("[Server] Product not found.");
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("[Server] Failed to delete product " + productId);
            e.printStackTrace();
        }
    }

    // sales crud helpers
    private void saveNewSale(Sale sale) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.save(sale);

            for (int productId : sale.getProductIds()) {
                session.createNativeQuery("INSERT INTO sale_products (sale_id, product_id) VALUES (:saleId, :productId)")
                        .setParameter("saleId", sale.getId())
                        .setParameter("productId", productId)
                        .executeUpdate();
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSale(Sale updatedSale) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            Sale dbSale = session.get(Sale.class, updatedSale.getId());
            if (dbSale != null) {
                dbSale.setDiscountValue(updatedSale.getDiscountValue());
                dbSale.setStartDate(updatedSale.getStartDate());
                dbSale.setEndDate(updatedSale.getEndDate());
                session.update(dbSale);
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteSale(int saleId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Sale dbSale = session.get(Sale.class, saleId);
            if (dbSale != null) {
                session.delete(dbSale);
                System.out.println("[Server] Sale deleted successfully.");
            } else {
                System.out.println("[Server] Sale not found.");
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("[Server] Failed to delete Sale " + saleId);
            e.printStackTrace();
        }
    }

    // daily vip expiry (deactivate expired and cancelled)
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

    // broadcast to all subscribed clients
    public void sendToAllClients(Msg message) {
        for (SubscribedClient subscribedClient : SubscribersList) {
            try {
                subscribedClient.getClient().sendToClient(message);
                System.out.println("sent to: " + subscribedClient.getClient() + message.getAction());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // data loaders
    private List<Product> fetchCatalog() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Product", Product.class).list();
        }
    }

    private  List<Sale> fetchSales() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Sale> sales = session.createQuery("FROM Sale", Sale.class).list();

            for (Sale sale : sales) {
                sale.setProductIds(session.createNativeQuery(
                                "SELECT product_id FROM sale_products WHERE sale_id = :saleId", Integer.class)
                        .setParameter("saleId", sale.getId())
                        .getResultList());
            }
            return sales;
        }
    }

    // branch stock check helper (returns [pid, want, have] triplets)
    private List<Object[]> checkBranchStock(Session s,
                                            int branchId,
                                            Map<Integer,Integer> requested) {
        String hql = """
        SELECT st.product.product_id ,
               SUM(st.quantity)
        FROM Storage st
        WHERE st.branch.branch_id = :bid
          AND st.product.product_id IN (:ids)
        GROUP BY st.product.product_id
        """;

        List<Object[]> stock = s.createQuery(hql,Object[].class)
                .setParameter("bid", branchId)
                .setParameterList("ids", requested.keySet())
                .getResultList();

        Map<Integer,Integer> available = new HashMap<>();
        stock.forEach(row -> available.put( (Integer)row[0] ,
                ((Long)row[1]).intValue() ));

        List<Object[]> result = new ArrayList<>();
        for (var e : requested.entrySet()) {
            Integer pid    = e.getKey();
            int      want  = e.getValue();
            Integer have   = available.get(pid);
            result.add(new Object[]{pid, want, have});
        }
        return result;
    }

    private List<Product> fetchProductsByOrderId(int orderId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("""
            SELECT b.product
            FROM Basket b
            WHERE b.order.id = :oid
        """, Product.class)
                    .setParameter("oid", orderId)
                    .list();
        } catch (Exception e) {
            System.err.println("[Server] Failed to fetch products for order " + orderId);
            e.printStackTrace();
            return List.of();
        }
    }

    @Override
    protected void serverStopped() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        System.out.println("Server stopped. Scheduler shut down.");
    }

    @Override
    protected void clientDisconnected(ConnectionToClient client) {
        cleanupClient(client);
    }

    @Override
    protected void clientException(ConnectionToClient client, Throwable exception) {
        cleanupClient(client);
    }

    private void cleanupClient(ConnectionToClient client) {
        Object u = client.getInfo("username");
        if (u instanceof String username) {
            onlineUsers.remove(username);
            client.setInfo("username", null);
        }
    }
    private static LoginUserDTO toLoginDTO(User u) {
        LoginUserDTO d = new LoginUserDTO();
        d.setId(u.getId());
        d.setUsername(u.getUsername());
        d.setEmail(u.getEmail());
        d.setFullName(u.getFullName());
        d.setPhoneNumber(u.getPhoneNumber());
        d.setRole(u.getRole().name());
        d.setVIP(u.isVIP());
        d.setActive(u.isActive());
        d.setVipExpirationDate(u.getVipExpirationDate());
        d.setVipCanceled(u.getVipCanceled());
        d.setCompensationTab(u.getCompensationTab());
        if (u.getBranch() != null) {
            d.setBranchId(u.getBranch().getBranchId());
            d.setBranchName(u.getBranch().getBranchName());
        }
        return d;
    }
    private void sendToUser(String username, Msg message) {
        ConnectionToClient c = onlineUsers.get(username);
        if (c != null) {
            try { c.sendToClient(message); } catch (IOException ignored) {}
        }
    }

}

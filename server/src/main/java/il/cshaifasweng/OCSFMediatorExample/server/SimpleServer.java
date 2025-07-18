package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.Msg;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;
import il.cshaifasweng.OrderDTO;
import il.cshaifasweng.OrderDisplayDTO;
import il.cshaifasweng.StockLineDTO;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.Transaction;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.time.LocalDateTime;
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
                                client.sendToClient(new Msg("LOGIN_FAILED", "Your account is frozen. Please contact support."));
                            } else {
                                client.sendToClient(new Msg("LOGIN_SUCCESS", user.getUsername()));
                            }
                        }
                    }
                }

                case "REGISTER" -> {
                    String[] regData = (String[]) data;
                    String username = regData[0], email = regData[1], fullName = regData[2],
                            phoneNumber = regData[3], password = regData[4], branchIdString = regData[5];

                    System.out.println("[Server] REGISTER received with username: " + username + ", branchIdString: " + branchIdString);

                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        Query<User> check = session.createQuery("FROM User WHERE username = :u OR email = :e");
                        check.setParameter("u", username);
                        check.setParameter("e", email);

                        if (!check.list().isEmpty()) {
                            client.sendToClient(new Msg("REGISTER_FAILED", "User already exists"));
                            System.out.println("[Server] REGISTER failed: user already exists");
                            return;
                        }

                        int branchId;
                        try {
                            branchId = Integer.parseInt(branchIdString);
                            System.out.println("[Server] Parsed branchId: " + branchId);
                        } catch (NumberFormatException e) {
                            client.sendToClient(new Msg("REGISTER_FAILED", "Invalid branch ID"));
                            System.out.println("[Server] REGISTER failed: invalid branch ID");
                            return;
                        }

                        Query<Branch> branchQuery = session.createQuery("FROM Branch WHERE branch_id = :bid", Branch.class);
                        branchQuery.setParameter("bid", branchId);
                        Branch selectedBranch = branchQuery.uniqueResult();

                        if (selectedBranch == null) {
                            client.sendToClient(new Msg("REGISTER_FAILED", "Selected branch does not exist"));
                            System.out.println("[Server] REGISTER failed: selected branch does not exist for ID: " + branchId);
                            return;
                        } else {
                            System.out.println("[Server] Found branch for registration: " + selectedBranch.getName() + ", ID: " + selectedBranch.getBranchId());
                        }

                        User newUser = new User();
                        newUser.setUsername(username);
                        newUser.setEmail(email);
                        newUser.setPhoneNumber(phoneNumber);
                        newUser.setFullName(fullName);
                        newUser.setPassword(password);
                        newUser.setBranch(selectedBranch);
                        newUser.setRole(User.Role.USER);
                        newUser.setActive(true);

                        session.beginTransaction();

                        try {
                            session.persist(newUser);
                            session.getTransaction().commit();
                            System.out.println("[Server] Registered new user: " + username + " with branch ID: " + branchId);
                            client.sendToClient(new Msg("REGISTER_SUCCESS", null));
                        } catch (Exception ex) {
                            session.getTransaction().rollback();
                            System.out.println("[Server] REGISTER failed: exception during persist");
                            ex.printStackTrace();
                            client.sendToClient(new Msg("REGISTER_FAILED", "Server error during registration."));
                        }
                    } catch (Exception e) {
                        System.out.println("[Server] REGISTER failed: outer exception");
                        e.printStackTrace();
                        client.sendToClient(new Msg("REGISTER_FAILED", "Server error during registration."));
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
                case "FETCH_BASKET" -> {
                    String username = (String) data;
                    System.out.println("[Server] FETCH_BASKET received for: " + username);
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username).uniqueResult();

                        if (user != null) {
                            List<Basket> basketItems = session.createQuery(
                                            "FROM Basket WHERE user.id = :userId AND order IS NULL", Basket.class)
                                    .setParameter("userId", user.getId()).list();

                            System.out.println("[Server] Found " + basketItems.size() + " basket items for user " + username);

                            for (Basket item : basketItems) {
                                Product fullProduct = session.get(Product.class, item.getProduct().getId());
                                item.setProduct(fullProduct);
                                System.out.println(" -> Basket item: " + fullProduct.getName() + " x" + item.getAmount());
                            }

                            client.sendToClient(new Msg("BASKET_FETCHED", basketItems));
                            System.out.println("[Server] BASKET_FETCHED sent to client.");
                        } else {
                            System.out.println("[Server] User not found: " + username);
                        }
                    } catch (Exception e) {
                        System.err.println("[Server] Error fetching basket for " + username);
                        e.printStackTrace();
                    }
                }



                case "ADD_TO_BASKET" -> {
                    Object[] arr = (Object[]) data;
                    String username = (String) arr[0];
                    Product product = (Product) arr[1];
                    System.out.println("Received ADD_TO_BASKET for " + username + " product ID: " + product.getId());
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username).uniqueResult();

                        Product dbProduct = session.get(Product.class, product.getId());  // 🔥 IMPORTANT

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
                                basket.setProduct(dbProduct);  // ✅ use managed object
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
                        ex.printStackTrace(); // ✅ make sure exception shows up
                    }
                }
                case "ADD_TO_BASKET_X_AMOUNT" -> {
                    Object[] arr = (Object[]) data;
                    String username = (String) arr[0];
                    Product product = (Product) arr[1];
                    int amount = (int) arr[2];
                    System.out.println("Received ADD_TO_BASKET for " + username + " product ID: " + product.getId());
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        User user = session.createQuery("FROM User WHERE username = :u", User.class)
                                .setParameter("u", username).uniqueResult();

                        Product dbProduct = session.get(Product.class, product.getId());  // 🔥 IMPORTANT

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
                                basket.setProduct(dbProduct);  // ✅ use managed object
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
                        ex.printStackTrace(); // ✅ make sure exception shows up
                    }
                }
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

                        // Notify client that basket is updated (you may choose to send updated basket)
                        client.sendToClient(new Msg("BASKET_UPDATED", null));
                    } catch (Exception e) {
                        System.err.println("[Server] Error removing basket item: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

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

                case "NEW_ORDER" -> {
                    OrderDTO dto = (OrderDTO) data;
                    boolean ok = false;

                    double totalPrice = 0;
                    double vipDiscount = 0;
                    double deliveryFee = 0;
                    int newOrderId = 0;
                    try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                        Transaction tx = s.beginTransaction();

                        // 1. Fetch user
                        User user = s.createQuery(
                                        "FROM User WHERE username = :u", User.class)
                                .setParameter("u", dto.getUsername())
                                .uniqueResult();

                        if (user == null) {
                            client.sendToClient(new Msg("ORDER_FAIL", "Unknown user"));
                            return;
                        }

                        // 2. Fetch basket lines with product using JOIN FETCH
                        List<Basket> basketItems = s.createQuery("""
                SELECT b FROM Basket b
                JOIN FETCH b.product
                WHERE b.id IN (:ids)
                  AND b.user = :u
                  AND b.order IS NULL
            """, Basket.class)
                                .setParameter("ids", dto.getBasketIds())
                                .setParameter("u", user)
                                .list();

                        if (basketItems.size() != dto.getBasketIds().size()) {
                            client.sendToClient(new Msg("ORDER_FAIL", "Basket mismatch"));
                            return;
                        }

                        // 3. PICK-UP stock validation (if applicable)
                        int branchId = -1;
                        if ("PICKUP".equals(dto.getFulfilType())) {

                            if (dto.getFulfilInfo() == null || dto.getFulfilInfo().isBlank()) {
                                client.sendToClient(new Msg("ORDER_FAIL", "No pick-up branch selected"));
                                return;
                            }

                            try {
                                branchId = Integer.parseInt(dto.getFulfilInfo());
                            } catch (NumberFormatException ex) {
                                client.sendToClient(new Msg("ORDER_FAIL", "Malformed branch id"));
                                return;
                            }

                            // Build requested product quantities
                            Map<Integer, Integer> requested = new HashMap<>();
                            for (Basket b : basketItems) {
                                requested.merge(b.getProduct().getId(), b.getAmount(), Integer::sum);
                            }

                            // Check branch stock
                            List<Object[]> chk = checkBranchStock(s, branchId, requested);
                            for (Object[] row : chk) {
                                Integer have = (Integer) row[2]; // available
                                int want = (Integer) row[1];     // requested

                                if (have == null || have < want) {
                                    client.sendToClient(new Msg("ORDER_FAIL", "Not enough items in selected pick-up branch!"));
                                    tx.rollback();
                                    return;
                                }
                            }

                            // Deduct stock
                            for (Object[] row : chk) {
                                int pid = (Integer) row[0];
                                int want = (Integer) row[1];

                                Storage st = s.createQuery("""
                        FROM Storage
                        WHERE product.product_id = :pid
                          AND branch.branch_id   = :bid
                    """, Storage.class)
                                        .setParameter("pid", pid)
                                        .setParameter("bid", branchId)
                                        .setMaxResults(1)
                                        .uniqueResult();

                                st.setQuantity(st.getQuantity() - want);
                                s.merge(st);
                            }
                        }

                        // 4. Create Order and link baskets
                        Order order = new Order();
                        order.setUser(user);
                        order.setReceived(false);

                        if ("PICKUP".equals(dto.getFulfilType())) {
                            Branch pickedBranch = s.get(Branch.class, branchId);
                            if (pickedBranch == null) {
                                client.sendToClient(new Msg("ORDER_FAIL", "Branch not found"));
                                tx.rollback();
                                return;
                            }
                            order.setBranch(pickedBranch);
                        } else if ("DELIVERY".equals(dto.getFulfilType())) {
                            if (dto.getFulfilInfo() == null || dto.getFulfilInfo().isBlank()) {
                                client.sendToClient(new Msg("ORDER_FAIL", "Delivery address missing"));
                                tx.rollback();
                                return;
                            }
                            order.setDelivery(dto.getFulfilInfo());
                        } else {
                            client.sendToClient(new Msg("ORDER_FAIL", "Bad fulfil type"));
                            tx.rollback();
                            return;
                        }

                        // 5. Calculate discount
                        double discount = 0;
                        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                            List<Sale> sales = session.createQuery("FROM Sale", Sale.class).list();
                            for (Sale sale : sales) {
                                sale.setProductIds(session.createNativeQuery(
                                                "SELECT product_id FROM sale_products WHERE sale_id = :saleId", Integer.class)
                                        .setParameter("saleId", sale.getId())
                                        .getResultList());
                            }
                            discount = Sale.calculateTotalDiscount(basketItems, sales);
                        }

                        double grandTotal = basketItems.stream()
                                .mapToDouble(Basket::getPrice)
                                .sum();

                        vipDiscount = user.isVIP()
                                ? (grandTotal - discount) * 0.10
                                : 0.0;
                        deliveryFee = "DELIVERY".equals(dto.getFulfilType())
                                ? 10.0
                                : 0.0;

                        totalPrice = grandTotal - discount - vipDiscount + deliveryFee;
                        order.setTotalPrice(totalPrice);

                        // 6. Set Deadline
                        order.setDeadline(dto.getDeadline());

                        // 7. Set recipient and greetings data
                        order.setRecipient(dto.getRecipient());
                        order.setGreeting(dto.getGreeting());

                        s.persist(order);

                        // Link baskets to order
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
                    Map<String, Object> result = Map.of(
                            "orderId", newOrderId,
                            "totalPrice", totalPrice,
                            "vipDiscount", vipDiscount,
                            "deliveryFee", deliveryFee
                    );
                    client.sendToClient(new Msg(ok ? "ORDER_OK" : "ORDER_FAIL", result));
                }



                case "HAS_CARD" -> {
                    /* data == username (String) */
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
                case "LIST_BRANCHES" -> {
                    System.out.println("[Server] Listing BRANCHES");
                    try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                        List<Branch> list = s.createQuery("FROM Branch", Branch.class).list();

                        // Debug print branches sent
                        System.out.println("[Server] Branches found in DB:");
                        for (Branch b : list) {
                            System.out.println("[Server] Branch: " + b.getName() + ", ID: " + b.getBranchId());
                        }

                        // force Hibernate to initialize collections before clearing
                        list.forEach(b -> {
                            b.getStockLines().size();  // trigger fetch to avoid LazyInitEx
                            b.setManager(null);        // manager is still a proxy — okay
                            b.getStockLines().clear(); // now we can clear safely
                        });

                        System.out.println("[Server] Sending BRANCHES_OK with " + list.size() + " branches");
                        client.sendToClient(new Msg("BRANCHES_OK", list));
                    } catch (Exception ex) {
                        System.err.println("[Server] Failed to send branches: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }


                case "STOCK_BY_BRANCH" -> {
                    Integer branchId = (Integer) data;   // null ⇒ all branches

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


                case "ADD_STOCK" -> {        // payload = int[productId, branchId, qty]
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
                            st.setProduct(s.get(Product.class, pid));   // product_id FK
                            st.setBranch (s.get(Branch.class,  bid));   // branch_id  FK
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
                case "FETCH_STOCK_SINGLE" -> {
                    /* payload = { productId, branchId } */
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

                        /* if null → no row yet → treat as 0 */
                        client.sendToClient(new Msg("STOCK_SINGLE_OK",
                                qty == null ? 0 : qty));
                    }
                }
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

                            String status = o.isReceived() ? "Received"
                                    : (o.getDelivery() != null ? "Out for delivery" : "Awaiting pickup");

                            double totalPrice = session.createQuery(
                                            "SELECT SUM(b.price) FROM Basket b WHERE b.order.id = :oid", Double.class)
                                    .setParameter("oid", o.getOrderId())
                                    .uniqueResultOptional()
                                    .orElse(0.0);

                            displayList.add(new OrderDisplayDTO(
                                    o.getOrderId(),
                                    o.getUser().getUsername(),
                                    fulfilment,
                                    status,
                                    totalPrice,
                                    o.getDeadline(),
                                    o.getRecipient(),
                                    o.getGreeting(),
                                    o.isReceived()
                            ));
                        }

                        client.sendToClient(new Msg("FETCH_ORDERS_OK", displayList));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        client.sendToClient(new Msg("FETCH_ORDERS_OK", List.of()));
                    }
                }

                case "MARK_ORDER_RECEIVED" -> {
                    int orderId = (int) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        session.beginTransaction();

                        Order order = session.get(Order.class, orderId);
                        if (order != null && !order.isReceived()) {
                            order.setReceived(true);
                            session.merge(order);
                        }

                        session.getTransaction().commit();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    client.sendToClient(new Msg("MARK_ORDER_RECEIVED_OK", List.of()));
                }
                case "FETCH_ORDER_PRODUCTS" -> {
                    System.out.println("[Server] FETCH_ORDER_PRODUCTS Received");
                    int orderId = (int) massage.getData();
                    List<Product> products = fetchProductsByOrderId(orderId); // You implement this method
                    client.sendToClient(new Msg("FETCH_ORDER_PRODUCTS_OK", products));
                }

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

                case "FREEZE_USER" -> {
                    int userId = (int) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        session.beginTransaction();
                        User user = session.get(User.class, userId);
                        if (user != null) {
                            user.setActive(false);
                            session.merge(user);
                        }
                        session.getTransaction().commit();
                        client.sendToClient(new Msg("USER_FREEZE_OK", userId));
                    }
                }

                case "UNFREEZE_USER" -> {
                    int userId = (int) data;
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        session.beginTransaction();
                        User user = session.get(User.class, userId);
                        if (user != null) {
                            user.setActive(true);
                            session.merge(user);
                        }
                        session.getTransaction().commit();
                        client.sendToClient(new Msg("USER_UNFREEZE_OK", userId));
                    }
                }

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
                    }
                }
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
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        session.beginTransaction();

                        User user = session.get(User.class, userId);
                        if (user == null) {
                            client.sendToClient(new Msg("UPDATE_USER_FAILED", "User not found"));
                            return;
                        }

                        List<User> conflicts = session.createQuery(
                                        "FROM User WHERE (username = :username OR email = :email) AND id != :id", User.class)
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
                        user.setActive(newActive);
                        if (newVip != null) {
                            user.setVIP(newVip);
                        }
                        // Update password if provided
                        if (newPassword != null && !newPassword.isBlank()) {
                            user.setPassword(newPassword);
                        }

                        if (newBranchName != null && !newBranchName.isBlank()) {
                            Branch branch = session.createQuery("FROM Branch WHERE branch_name = :name", Branch.class)
                                    .setParameter("name", newBranchName)
                                    .uniqueResult();
                            if (branch != null) {
                                user.setBranch(branch);
                            } else {
                                client.sendToClient(new Msg("UPDATE_USER_FAILED", "Branch not found"));
                                session.getTransaction().rollback();
                                return;
                            }
                        }

                        session.merge(user);
                        session.getTransaction().commit();

                        client.sendToClient(new Msg("UPDATE_USER_OK", null));
                        System.out.println("[Server] Updated user ID " + userId + " successfully.");
                    } catch (Exception e) {
                        e.printStackTrace();
                        client.sendToClient(new Msg("UPDATE_USER_FAILED", "Error updating user"));
                    }
                }
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




    private void deleteProduct(int productId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            /* 1. detach from baskets (prevents FK violation) */
            int removed = session.createQuery(
                            "DELETE FROM Basket b WHERE b.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();
            System.out.println("[Server] Removed " + removed + " basket rows");

            /* 2. delete the product itself */
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


    private List<Object[]> checkBranchStock(Session s,
                                            int branchId,
                                            Map<Integer,Integer> requested) {

        // HQL fetches only needed rows from storage
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

        /* put into map → product_id → quantityInBranch */
        Map<Integer,Integer> available = new HashMap<>();
        stock.forEach(row -> available.put( (Integer)row[0] ,
                ((Long)row[1]).intValue() ));

        // build answer list in same order as requested map
        List<Object[]> result = new ArrayList<>();
        for (var e : requested.entrySet()) {
            Integer pid    = e.getKey();
            int      want  = e.getValue();
            Integer have   = available.get(pid);   // may be null
            result.add(new Object[]{pid, want, have});
        }
        return result;
    }



    @Override
    protected void serverStopped() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        System.out.println("Server stopped. Scheduler shut down.");
    }
}

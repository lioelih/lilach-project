package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.Session;
import java.util.List;

public class HibernateUtil {
    public static void printAllProducts() { // This is only used as a server-side check up on the items, there isn't much more to this
        try (Session session = getSessionFactory().openSession()) {
            List<Product> products = session.createQuery("FROM Product", Product.class).list();
            for (Product product : products) {
                System.out.println(product);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SessionFactory sessionFactory;

    static {
        try {
            Configuration configuration = new Configuration();
            configuration.configure(); // looks for hibernate.cfg.xml
            configuration.addAnnotatedClass(Product.class); // link entity
            configuration.addAnnotatedClass(User.class);
            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties()).build();
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}

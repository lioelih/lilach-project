package il.cshaifasweng.OCSFMediatorExample.client;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBUtil {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = DBUtil.class.getClassLoader().getResourceAsStream("hibernate.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                throw new RuntimeException("Could not load hibernate.properties from resources!");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load DB properties", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = props.getProperty("hibernate.connection.url");
        String user = props.getProperty("hibernate.connection.username");
        String pass = props.getProperty("hibernate.connection.password");
        return DriverManager.getConnection(url, user, pass);
    }
}

package connectDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Simple DB connection utility for MariaDB.
 */
public class DBConnection {
    private static final String URL = "jdbc:mariadb://localhost:3306/cafe_pos";
    private static final String USER = "root";
    private static final String PASS = "2707";

    static {
        try {
            // ensure driver is loaded
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // Driver not found; applications will get SQLException when trying to connect
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

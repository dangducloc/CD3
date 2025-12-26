package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB_handler {
    private static final String URL = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:mysql://localhost:3306/pentest_final";
    private static final String USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
    private static final String PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "";

    public static Connection getConnection() throws SQLException {
        try {
            System.out.printf("DB_URL: %s%n", URL); 
            System.out.printf("Connecting to database at %s with user %s and pass %s %n", URL, USER, PASSWORD);
            Class.forName("com.mysql.cj.jdbc.Driver"); // ✅ Load driver explicitly
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found!", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}


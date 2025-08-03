/*
 * Database Configuration Class
 * Centralized database connection management
 */
package airlinemanagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 *
 * @author divya
 */
public class DatabaseConfig {
    
    // Database configuration constants
    private static final String DB_URL = "jdbc:mysql://localhost:3306/airlinedb";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    
    /**
     * Get database connection
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Load MySQL driver
            Class.forName(DB_DRIVER);
            
            // Create and return connection
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "MySQL Driver not found: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            throw new SQLException("MySQL Driver not found", e);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database connection failed: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }
    
    /**
     * Close database connection safely
     * @param connection Connection to close
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Test database connection
     * @return true if connection successful, false otherwise
     */
    public static boolean testConnection() {
        Connection testConn = null;
        try {
            testConn = getConnection();
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                    "Database connection test failed:\n" + e.getMessage(), 
                    "Connection Test", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            closeConnection(testConn);
        }
    }
    
    /**
     * Get database URL
     * @return Database URL
     */
    public static String getDbUrl() {
        return DB_URL;
    }
    
    /**
     * Get database user
     * @return Database username
     */
    public static String getDbUser() {
        return DB_USER;
    }
    
    /**
     * Get database password
     * @return Database password
     */
    public static String getDbPassword() {
        return DB_PASSWORD;
    }
} 
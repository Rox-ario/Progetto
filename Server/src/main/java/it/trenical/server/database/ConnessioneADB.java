package it.trenical.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnessioneADB
{
    private static final String URL = "jdbc:mysql://localhost:3306/trenical";
    private static final String USER = "root";
    private static final String PASSWORD = "Trenical2024!"; // Inserisci la tua password

    static
    {
        try
        { //qui carico il driver mysql
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void closeConnection(Connection conn)
    {
        if (conn != null)
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }
}

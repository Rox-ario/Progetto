package it.trenical.server.database;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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

    public static void inizializzaDatabase(String pathFileSql) throws Exception
    {
        String sql = new String(Files.readAllBytes(Paths.get(pathFileSql)));

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD))
        {
            Statement stmt = conn.createStatement();
            String[] statements = sql.split(";");

            for (String query : statements)
            {
                System.out.println("query prima: "+query);
                query = query.trim();

                if (query.isEmpty() || query.startsWith("--") || query.startsWith("#")) {
                    continue;
                }

                query = query.replaceAll("--.*", "").trim();

                if (!query.isEmpty())
                {
                    System.out.println("query dopo: "+ query);
                    stmt.execute(query);
                }
            }

        }
    }
}

package it.trenical.server.domain.gestore;

import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.cliente.ClienteBanca;
import it.trenical.server.dto.DatiBancariDTO;
import it.trenical.server.observer.Promozione.ObserverPromozione;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GestoreClienti
{
    private static GestoreClienti instance = null;
    private final Map<String, Cliente> clientiById;
    private final Map<String, Cliente> clientiByEmail;

    private GestoreClienti()
    {
        clientiById = new HashMap<>();
        clientiByEmail = new HashMap<>();
        //Carico i clienti dal database all'avvio
        caricaClientiDaDB();
    }

    private void caricaClientiDaDB()
    {
        String sql = "SELECT * FROM clienti";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection(); //creo la connessione
            Statement stmt = conn.createStatement(); //questo mi serve per scrivere le richieste
            ResultSet rs = stmt.executeQuery(sql); //questo invece mi esegue le query

            while (rs.next())
            {
                Cliente cliente = new Cliente.Builder()
                        .ID(rs.getString("id"))
                        .Nome(rs.getString("nome"))
                        .Cognome(rs.getString("cognome"))
                        .Email(rs.getString("email"))
                        .Password(rs.getString("password"))
                        .isFedelta(rs.getBoolean("is_fedelta"))
                        .riceviNotifiche(rs.getBoolean("ricevi_notifiche"))
                        .riceviPromozioni(rs.getBoolean("ricevi_promozioni"))
                        .build();

                clientiById.put(cliente.getId(), cliente);
                clientiByEmail.put(cliente.getEmail(), cliente);
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore caricamento clienti: " + e.getMessage());
        } finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public static synchronized GestoreClienti getInstance()
    {
        if (instance == null) {
            instance = new GestoreClienti();
        }
        return instance;
    }

    public boolean esisteClienteID(String id)
    {
        if(clientiById.containsKey(id))
        {
           return true;
        }
        return false;
    }

    public boolean esisteClienteEmail(String email)
    {
        if(clientiByEmail.containsKey(email))
        {
            return true;
        }
        return false;
    }

    public Cliente getClienteById(String id)
    {
        if(clientiById.containsKey(id))
            return clientiById.get(id);
        return null;
    }

    public Cliente getClienteByEmail(String email)
    {
        if(clientiByEmail.containsKey(email))
            return clientiByEmail.get(email);
        return null;
    }

    public void aggiungiCliente(Cliente c, DatiBancariDTO datiBancariCustom)
    {
        if (datiBancariCustom == null) {
            throw new IllegalArgumentException("I dati bancari sono obbligatori per la registrazione");
        }

        String id = c.getId();
        String email = c.getEmail();

        clientiById.putIfAbsent(id, c);
        clientiByEmail.putIfAbsent(email, c);

        salvaClienteInDB(c, datiBancariCustom);

        System.out.println("Dati bancari = " + datiBancariCustom.toString());
        GestoreBanca.getInstance().registraClienteBanca(
                new ClienteBanca(
                        c.getId(),
                        c.getNome(),
                        c.getCognome(),
                        datiBancariCustom.getNumeroCarta(),
                        datiBancariCustom.getSaldo()
                )
        );
    }

    private void salvaClienteInDB(Cliente c, DatiBancariDTO datiBancariDTO)
    {
        if (datiBancariDTO == null || datiBancariDTO.getNumeroCarta() == null) {
            throw new IllegalArgumentException("I dati bancari sono obbligatori");
        }
        String sql = "INSERT INTO clienti (id, nome, cognome, email, password, is_fedelta, " +
                "ricevi_notifiche, ricevi_promozioni) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, c.getId());
            pstmt.setString(2, c.getNome());
            pstmt.setString(3, c.getCognome());
            pstmt.setString(4, c.getEmail());
            pstmt.setString(5, c.getPassword());
            pstmt.setBoolean(6, c.haAdesioneFedelta());
            pstmt.setBoolean(7, c.isRiceviNotifiche());
            pstmt.setBoolean(8, c.isRiceviPromozioni());

            pstmt.executeUpdate();

            String numeroCarta, banca;

            if (datiBancariDTO != null && datiBancariDTO.getNumeroCarta() != null)
            {
                numeroCarta = datiBancariDTO.getNumeroCarta();
                banca = datiBancariDTO.getNomeBanca() != null ?
                        datiBancariDTO.getNomeBanca() : "Banca Trenical";

                if (!isValidCardFormat(numeroCarta))
                {
                    throw new IllegalArgumentException("Formato carta non valido: " + numeroCarta);
                }
            }
            else
            {
                throw new IllegalArgumentException("La carta non può essere inventata");
            }

            String sqlBanca = "INSERT INTO clienti_banca (cliente_id, cliente_nome, cliente_cognome, " +
                    "banca_cliente, cliente_numeroCarta, saldo) VALUES (?, ?, ?, ?, ?, 1000.00)";
            PreparedStatement pstmtBanca = conn.prepareStatement(sqlBanca);
            pstmtBanca.setString(1, c.getId());
            pstmtBanca.setString(2, c.getNome());
            pstmtBanca.setString(3, c.getCognome());
            pstmtBanca.setString(4, banca);
            pstmtBanca.setString(5, numeroCarta);
            pstmtBanca.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore salvataggio cliente: " + e.getMessage());
            throw new RuntimeException("Impossibile salvare il cliente", e);
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private boolean isValidCardFormat(String carta) {
        if (carta == null) return false;

        //rimuovo i trattini e controllo che siano solo numeri
        String numeriSolo = carta.replace("-", "").replace(" ", "");

        //deve essere 16 cifre
        if (numeriSolo.length() != 16) return false;

        //deve contenere solo numeri
        return numeriSolo.matches("\\d{16}");
    }

    public boolean autenticaCliente(String email, String password)
    {
        if(!esisteClienteEmail(email))
            return false;
        String pwC = clientiByEmail.get(email).getPassword();
        return pwC.equals(password);
    }

    public void aggiornaCliente(String idCliente, Cliente nuovo)
    {
        if(!esisteClienteID(idCliente))
            return;
        Cliente vecchio = clientiById.get(idCliente);
        String email = vecchio.getEmail();

        clientiById.put(idCliente, nuovo);

        if(!email.equals(nuovo.getEmail()))
        {
            clientiByEmail.remove(email);
        }

        clientiByEmail.put(nuovo.getEmail(), nuovo);

        aggiornaClienteInDB(nuovo);
    }

    private void aggiornaClienteInDB(Cliente c)
    {
        String sql = "UPDATE clienti SET nome=?, cognome=?, email=?, password=?, " +
                "is_fedelta=?, ricevi_notifiche=?, ricevi_promozioni=? WHERE id=?";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, c.getNome());
            pstmt.setString(2, c.getCognome());
            pstmt.setString(3, c.getEmail());
            pstmt.setString(4, c.getPassword());
            pstmt.setBoolean(5, c.haAdesioneFedelta());
            pstmt.setBoolean(6, c.isRiceviNotifiche());
            pstmt.setBoolean(7, c.isRiceviPromozioni());
            pstmt.setString(8, c.getId());

            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore aggiornamento cliente: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public List<Cliente> getClientiFedeltaConNotifiche()
    {
        List<Cliente> clientiFedelta = new ArrayList<>();

        for (Cliente cliente : clientiById.values()) {
            if (cliente.haAdesioneFedelta() &&
                    cliente.isRiceviPromozioni())
            {
                clientiFedelta.add(cliente);
            }
        }
        return clientiFedelta;
    }
}

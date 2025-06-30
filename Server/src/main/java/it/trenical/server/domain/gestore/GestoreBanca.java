package it.trenical.server.domain.gestore;

import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.cliente.ClienteBanca;
import it.trenical.server.dto.RimborsoDTO;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public final class GestoreBanca
{
    private static GestoreBanca instance = null;

    private final Map<String, ClienteBanca> clienti;

    private GestoreBanca()
    {
        clienti = new HashMap<>();
        caricaClientiDaDB();
    }

    private void caricaClientiDaDB()
    {
        String sql = "SELECT * FROM clienti_banca";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            Statement stm = conn.createStatement();
            ResultSet res = stm.executeQuery(sql);

            while(res.next())
            {
                ClienteBanca clienteBanca = new ClienteBanca
                        (res.getString("cliente_id"),
                        res.getString("cliente_nome"),
                        res.getString("cliente_cognome"),
                        res.getString("banca_cliente"),
                        res.getString("cliente_numeroCarta"),
                        res.getDouble("saldo"));

                clienti.put(clienteBanca.getIdCliente(), clienteBanca);
            }
        }
        catch(SQLException e)
        {
            System.err.println(e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public static synchronized GestoreBanca getInstance() {
        if (instance == null) {
            instance = new GestoreBanca();
        }
        return instance;
    }

    public void registraClienteBanca(ClienteBanca cb)
    {
        if(cb == null)
            throw new IllegalArgumentException("Errore: Il cliente non può essere null");
        clienti.put(cb.getIdCliente(), cb);
    }

    public ClienteBanca getClienteBanca(String id)
    {
        return clienti.get(id);
    }

    public boolean eseguiPagamento(String id, double importo)
    {
        ClienteBanca cb = clienti.get(id);
        if (cb != null && cb.getSaldo() >= importo)
        {
            String sql = "UPDATE clienti_banca SET saldo = saldo + ? WHERE cliente_id = ?";
            Connection conn = null;
            try//addebito il prezzo
            {
                conn = ConnessioneADB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);

                pstmt.setDouble(1, -importo);
                pstmt.setString(2, cb.getIdCliente()); //con questo ci metto l'id del cliente

                pstmt.executeUpdate();
                cb.addebita(importo);
                return true;
            }
            catch (SQLException e)
            {
                System.err.println(e.getMessage());
            }
            finally
            {
                ConnessioneADB.closeConnection(conn);
            }
        }
        return false;
    }

    public void rimborsa(RimborsoDTO dto) {
        //estraggo i dati
        String idBiglietto = dto.getIdBiglietto();
        String idCliente = dto.getIdClienteRimborsato();
        double saldo = dto.getImportoRimborsato();

        if(!clienti.containsKey(idCliente))
            throw new IllegalArgumentException("Errore: Il cliente "+ idCliente+" non esiste nella banca");
        else
        {
            ClienteBanca cb = clienti.get(idCliente);

            String sql = "UPDATE clienti_banca SET saldo = saldo + ? WHERE cliente_id = ?";
            Connection conn = null;
            try
            {
                conn = ConnessioneADB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);

                pstmt.setDouble(1, cb.getSaldo()+saldo);
                pstmt.setString(2, cb.getIdCliente());

                pstmt.executeUpdate();
                cb.accredita(saldo);
            }
            catch(SQLException e)
            {
                System.err.println(e.getMessage());
            }
            finally
            {
                ConnessioneADB.closeConnection(conn);
            }
        }
    }
}


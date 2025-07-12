package it.trenical.server.domain.gestore;

import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.NotificheListener;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.dto.NotificaDTO;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class GestoreNotifiche
{
    private static GestoreNotifiche instance = null;

    //Listener per notifiche real-time
    private final Map<String, NotificheListener> listenerPerCliente;
    private final List<NotificheListener> listenersGlobali;

    //ExecutorService per notifiche asincrone
    private final ExecutorService notificheExecutor;

    //Timestamp ultima lettura notifiche per cliente (in memoria)
    private final Map<String, Timestamp> ultimaLetturaPerCliente;

    private GestoreNotifiche()
    {
        listenerPerCliente = new ConcurrentHashMap<>();
        listenersGlobali = Collections.synchronizedList(new ArrayList<>());
        ultimaLetturaPerCliente = new ConcurrentHashMap<>();

        int numeroThreads = 10;
        notificheExecutor = Executors.newFixedThreadPool(numeroThreads,
                r -> {
            Thread t = new Thread(r);
            t.setName("NotificheThread-" + Thread.activeCount());
            t.setDaemon(true);
            return t;
        }
        );

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static synchronized GestoreNotifiche getInstance()
    {
        if (instance == null)
        {
            instance = new GestoreNotifiche();
        }
        return instance;
    }

    public void inviaNotifica(String idCliente, NotificaDTO notifica)
    {
        //Validazione
        GestoreClienti gc = GestoreClienti.getInstance();
        Cliente cliente = gc.getClienteById(idCliente);

        if (cliente == null || !cliente.isRiceviNotifiche())
        {
            return;
        }

        //Invio in modo asincrono
        CompletableFuture.runAsync(() -> {
            try {
                //calvo nel database
                salvaNotificaInDB(idCliente, notifica);

                //notifica listener real-time se presente
                NotificheListener listener = listenerPerCliente.get(idCliente);
                if (listener != null)
                {
                    try
                    {
                        listener.onNuovaNotifica(idCliente, notifica);
                    }
                    catch (Exception e)
                    {
                        System.err.println("Errore nel listener: " + e.getMessage());
                    }
                }

                //notifica listener globali
                for (NotificheListener globalListener : listenersGlobali)
                {
                    try
                    {
                        globalListener.onNuovaNotifica(idCliente, notifica);
                    }
                    catch (Exception e)
                    {
                        System.err.println("Errore nel listener globale: " + e.getMessage());
                    }
                }

            }
            catch (Exception e)
            {
                System.err.println("Errore invio notifica: " + e.getMessage());
                e.printStackTrace();
            }
        }, notificheExecutor);
    }

    public void inviaNotificaPromozionale(String idCliente, NotificaDTO notifica)
    {
        GestoreClienti gc = GestoreClienti.getInstance();
        Cliente cliente = gc.getClienteById(idCliente);

        if (cliente == null || !cliente.isRiceviNotifiche() || !cliente.isRiceviPromozioni())
        {
            return;
        }

        inviaNotifica(idCliente, notifica);
    }


    private void salvaNotificaInDB(String idCliente, NotificaDTO notifica)
    {
        String sql = "INSERT INTO notifiche (id, cliente_id, messaggio, timestamp) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConnessioneADB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, idCliente);
            pstmt.setString(3, notifica.getMessaggio());
            pstmt.setTimestamp(4, new Timestamp(notifica.getTimestamp().getTimeInMillis()));

            pstmt.executeUpdate();
        }
        catch (SQLException e)
        {
            System.err.println("Errore salvataggio notifica: " + e.getMessage());
        }
    }

    public synchronized List<NotificaDTO> getNotificheNonLette(String idCliente)
    {
        List<NotificaDTO> notifiche = new ArrayList<>();

        //Ottiengo il timestamp ultima lettura (se esiste)
        Timestamp ultimaLettura = ultimaLetturaPerCliente.get(idCliente);

        String sql;
        if (ultimaLettura == null)
        {
            //prima volta che legge, prendi tutte
            sql = "SELECT id, messaggio, timestamp FROM notifiche " +
                    "WHERE cliente_id = ? " +
                    "ORDER BY timestamp DESC";
        }
        else
        {
            //prendi solo quelle dopo l'ultima lettura
            sql = "SELECT id, messaggio, timestamp FROM notifiche " +
                    "WHERE cliente_id = ? AND timestamp > ? " +
                    "ORDER BY timestamp DESC";
        }

        Connection conn = null;
        Timestamp nuovaUltimaLettura = new Timestamp(System.currentTimeMillis());

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, idCliente);

            if (ultimaLettura != null)
            {
                pstmt.setTimestamp(2, ultimaLettura);
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next())
            {
                String messaggio = rs.getString("messaggio");
                Timestamp timestamp = rs.getTimestamp("timestamp");

                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(timestamp.getTime());
                NotificaDTO notifica = new NotificaDTO(messaggio, cal);

                notifiche.add(notifica);
            }

            //aggiorno il timestamp di ultima lettura
            ultimaLetturaPerCliente.put(idCliente, nuovaUltimaLettura);

        }
        catch (SQLException e)
        {
            System.err.println("Errore recupero notifiche non lette: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }

        return notifiche;
    }

    public List<NotificaDTO> getStoricoNotifiche(String idCliente)
    {
        return getStoricoNotifiche(idCliente, null, null);
    }

    private List<NotificaDTO> getStoricoNotifiche(String idCliente, Timestamp da, Timestamp a)
    {
        List<NotificaDTO> notifiche = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT id, messaggio, timestamp FROM notifiche WHERE cliente_id = ?"
        );

        if (da != null) {
            sql.append(" AND timestamp >= ?");
        }
        if (a != null) {
            sql.append(" AND timestamp <= ?");
        }

        sql.append(" ORDER BY timestamp DESC");

        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql.toString());

            int paramIndex = 1;
            pstmt.setString(paramIndex++, idCliente);

            if (da != null) {
                pstmt.setTimestamp(paramIndex++, da);
            }
            if (a != null) {
                pstmt.setTimestamp(paramIndex++, a);
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next())
            {
                String messaggio = rs.getString("messaggio");
                Timestamp timestamp = rs.getTimestamp("timestamp");

                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(timestamp.getTime());
                NotificaDTO notifica = new NotificaDTO(messaggio, cal);

                notifiche.add(notifica);
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore recupero storico notifiche: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }

        return notifiche;
    }

    public int contaNotificheNonLette(String idCliente)
    {
        Timestamp ultimaLettura = ultimaLetturaPerCliente.get(idCliente);

        String sql;
        if (ultimaLettura == null) {
            sql = "SELECT COUNT(*) FROM notifiche WHERE cliente_id = ?";
        } else {
            sql = "SELECT COUNT(*) FROM notifiche WHERE cliente_id = ? AND timestamp > ?";
        }

        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, idCliente);

            if (ultimaLettura != null) {
                pstmt.setTimestamp(2, ultimaLettura);
            }

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore conteggio notifiche: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }

        return 0;
    }

    public void pulisciNotificheVecchie(int giorniDaMantenere)
    {
        String sql = "DELETE FROM notifiche WHERE timestamp < ?";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -giorniDaMantenere);
            pstmt.setTimestamp(1, new Timestamp(cal.getTimeInMillis()));

            int deleted = pstmt.executeUpdate();
            conn.commit();

            System.out.println("Eliminate " + deleted + " notifiche vecchie");

        }
        catch (SQLException e)
        {
            System.err.println("Errore pulizia notifiche: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    // Metodi per gestione listener (invariati)
    public void registraListener(String idCliente, NotificheListener listener)
    {
        listenerPerCliente.put(idCliente, listener);
    }

    public void registraListenerGlobale(NotificheListener listener)
    {
        listenersGlobali.add(listener);
    }

    public void rimuoviListenerGlobale(NotificheListener listener)
    {
        listenersGlobali.remove(listener);
    }

    public void rimuoviListener(String idCliente)
    {
        listenerPerCliente.remove(idCliente);
    }

    public void shutdown() {
        System.out.println("Chiusura GestoreNotifiche...");
        notificheExecutor.shutdown();
        try {
            if (!notificheExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                notificheExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            notificheExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
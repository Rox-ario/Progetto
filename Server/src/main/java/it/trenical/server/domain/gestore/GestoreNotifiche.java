package it.trenical.server.domain.gestore;

import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.NotificheListener;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.dto.NotificaDTO;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GestoreNotifiche
{
    private static GestoreNotifiche instance = null;

    private final Map<String, List<NotificaDTO>> notifichePerCliente;
    //Coda di notifiche per ogni cliente (mi servirà per lo storico delle notifiche)
    private final Map<String, NotificheListener> listenerPerCliente;
    //Listener per notifiche real-time per la sessione in cui il cliente è attivo
    private final List<NotificheListener> listenersGlobali;
    //Mi servono se eventualmente voglio osservare tutte le notifiche inviate nel sistema (indipendetnemente dal tipo)

    private GestoreNotifiche()
    {
        notifichePerCliente = new ConcurrentHashMap<>();
        listenerPerCliente = new ConcurrentHashMap<>();
        listenersGlobali = new ArrayList<NotificheListener>();

        //solo quando è richiesto, carico i dati da DB
    }

    public static synchronized GestoreNotifiche getInstance() {
        if (instance == null) {
            instance = new GestoreNotifiche();
        }
        return instance;
    }

    //avverto un cliente
    public void inviaNotifica(String idCliente, NotificaDTO notifica)
    {
        GestoreClienti gc = GestoreClienti.getInstance();
        Cliente cliente = gc.getClienteById(idCliente);

        if (cliente == null || !cliente.isRiceviNotifiche())
        {
            return;
        }

        //Aggiungo alla coda delle notifiche la notifica
        if (!notifichePerCliente.containsKey(cliente))
            notifichePerCliente.put(idCliente, new ArrayList<>());
        System.out.println("Size prima: "+ notifichePerCliente.get(idCliente).size());
        notifichePerCliente.get(idCliente).add(notifica);
        System.out.println("La size dopo: "+ notifichePerCliente.size());
        for(NotificaDTO notificaDTO : notifichePerCliente.get(cliente.getId()))
        {
            System.out.println("Notifica "+ notificaDTO.getMessaggio());
        }

        //passo al Listener del cliente e lo/li avviso
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

        //Notifico i listener globali di quanto accaduto
        for(NotificheListener notificheListener :listenersGlobali)
        {
            try
            {
                notificheListener.onNuovaNotifica(idCliente, notifica);
            }
            catch (Exception e)
            {
                System.err.println("Errore nel listener globale: " + e.getMessage());
            }
        }
    }

    public void inviaNotificaPromozionale(String idCliente, NotificaDTO notifica)
    {
        GestoreClienti gc = GestoreClienti.getInstance();
        Cliente cliente = gc.getClienteById(idCliente);

        if (cliente == null || !cliente.isRiceviNotifiche() || !cliente.isRiceviPromozioni()) {
            return;
        }
        salvaNotificaInDB(idCliente, notifica);

        inviaNotifica(idCliente, notifica);
    }

    private void salvaNotificaInDB(String idCliente, NotificaDTO notifica)
    {
        String sql = "INSERT INTO notifiche (id, cliente_id, messaggio, timestamp) VALUES (?, ?, ?, ?)";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

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
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private List<NotificaDTO> caricaNotificheDaDB(String idCliente)
    {
        List<NotificaDTO> notifiche = new ArrayList<>();
        String sql = "SELECT messaggio, timestamp FROM notifiche WHERE cliente_id = ? ORDER BY timestamp DESC";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, idCliente);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next())
            {
                String messaggio = rs.getString("messaggio");
                Timestamp timestamp = rs.getTimestamp("timestamp");

                NotificaDTO notifica = new NotificaDTO(messaggio);
                Calendar timestampNew = Calendar.getInstance();
                timestampNew.setTimeInMillis(timestamp.getTime());
                notifica.setTimestamp(timestampNew);

                notifiche.add(notifica);
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore caricamento notifiche: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }

        return notifiche;
    }



    /*
    public void inviaNotificaBroadcast(List<String> idClienti, NotificaDTO notifica) {
        for (String idCliente : idClienti) {
            inviaNotifica(idCliente, notifica);
        }
    }
    */


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

    //vediamo tutte le notifiche non lette
    public List<NotificaDTO> getNotificheNonLette(String idCliente)
    {
        List<NotificaDTO> coda = notifichePerCliente.get(idCliente);
        if (coda == null || coda.isEmpty())
        {
            return new ArrayList<>();
        }

        List<NotificaDTO> notifiche = new ArrayList<>(coda);
        coda.clear(); //Svuoto dopo la lettura
        return notifiche;
    }

    //recupero tutte le notifiche, anche quelle lette, senza cancellarle
    public List<NotificaDTO> getStoricoNotifiche(String idCliente)
    {
        //leggp da DB
        return caricaNotificheDaDB(idCliente);
    }


    public int contaNotificheNonLette(String idCliente)
    {
        return notifichePerCliente.get(idCliente).size();
    }
}

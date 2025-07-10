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

    private final Map<String, List<NotificaDTO>> notifichePerCliente;
    //Coda di notifiche per ogni cliente (mi servirà per lo storico delle notifiche)
    private final Map<String, NotificheListener> listenerPerCliente;
    //Listener per notifiche real-time per la sessione in cui il cliente è attivo
    private final List<NotificheListener> listenersGlobali;
    //Mi servono se eventualmente voglio osservare tutte le notifiche inviate nel sistema (indipendetnemente dal tipo)

    private final ExecutorService notificheExecutor;
    private final Map<String, Future<?>> notificheInCorso;
    //Per tracciare lo stato delle notifiche in tempo reale

    private GestoreNotifiche()
    {
        notifichePerCliente = new ConcurrentHashMap<>();
        listenerPerCliente = new ConcurrentHashMap<>();
        listenersGlobali = Collections.synchronizedList(new ArrayList<>());

        //solo quando è richiesto, carico i dati da DB
        int numeroThreads = Runtime.getRuntime().availableProcessors() * 2;
        notificheExecutor = Executors.newFixedThreadPool(numeroThreads, new ThreadFactory()
        {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r)
            {
                Thread t = new Thread(r);
                t.setName("NotificheThread-" + counter++);
                t.setDaemon(true); //Thread daemon per non bloccare lo shutdown
                return t;
            }
        });

        notificheInCorso = new ConcurrentHashMap<>();

        //aggiungo uno shutdown hook per chiudere l'executor correttamente
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

    //avverto un cliente con invio asincrono
    public void inviaNotifica(String idCliente, NotificaDTO notifica)
    {
        GestoreClienti gc = GestoreClienti.getInstance();
        Cliente cliente = gc.getClienteById(idCliente);

        if (cliente == null || !cliente.isRiceviNotifiche())
        {
            return;
        }

        //invio la notifica in modo asincrono
        Future<?> futureNotifica = notificheExecutor.submit(() -> {
            try
            {
                //aggiungo alla coda delle notifiche
                notifichePerCliente.computeIfAbsent(idCliente,
                                k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(notifica);

                System.out.println("[Thread: " + Thread.currentThread().getName() +
                        "] Notifica aggiunta per cliente: " + idCliente);

                //lo salvo nel database
                salvaNotificaInDB(idCliente, notifica);

                //notifico il listener del cliente se presente
                NotificheListener listener = listenerPerCliente.get(idCliente);
                if (listener != null)
                {
                    try
                    {
                        listener.onNuovaNotifica(idCliente, notifica);
                        System.out.println("[Thread: " + Thread.currentThread().getName() +
                                "] Listener notificato per cliente: " + idCliente);
                    }
                    catch (Exception e)
                    {
                        System.err.println("Errore nel listener per cliente " + idCliente +
                                ": " + e.getMessage());
                    }
                }

                //notifico i listener globali
                notificaListenersGlobali(idCliente, notifica);

            }
            catch (Exception e)
            {
                System.err.println("Errore durante l'invio della notifica: " + e.getMessage());
                e.printStackTrace();
            }
        });

        //traccio la notifica in corso con la chiave unica
        notificheInCorso.put(idCliente + "_" + System.nanoTime(), futureNotifica);

        //rimuovo le notifiche completate dalla mappa, controllando periodicamente lo stato del Future
        notificheExecutor.submit(() -> {
            try
            {
                //attendo che il Future corrente termini
                futureNotifica.get();
            }
            catch (Exception ignore) {}
            //rimozione pulita dei Future completati
            notificheInCorso.values().removeIf(Future::isDone);
        });
    }

    private void notificaListenersGlobali(String idCliente, NotificaDTO notifica)
    {
        List<NotificheListener> copiaListeners = new ArrayList<>(listenersGlobali);
        for (NotificheListener listener : copiaListeners)
        {
            try
            {
                listener.onNuovaNotifica(idCliente, notifica);
            }
            catch (Exception e)
            {
                System.err.println("Errore nel listener globale: " + e.getMessage());
            }
        }
    }

    public void inviaNotificaPromozionale(String idCliente, NotificaDTO notifica)
    {
        notificheExecutor.submit(() -> {
            GestoreClienti gc = GestoreClienti.getInstance();
            Cliente cliente = gc.getClienteById(idCliente);

            if (cliente == null || !cliente.isRiceviNotifiche() || !cliente.isRiceviPromozioni()) {
                return;
            }

            inviaNotifica(idCliente, notifica);
        });
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
    public synchronized List<NotificaDTO> getNotificheNonLette(String idCliente)
    {
        List<NotificaDTO> coda = notifichePerCliente.get(idCliente);
        if (coda == null || coda.isEmpty())
        {
            return new ArrayList<>();
        }

        //creo una copia thread-safe
        List<NotificaDTO> notifiche = new ArrayList<>(coda);
        coda.clear(); //svuoto dopo la lettura
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

    //metodo per chiudere correttamente l'executor
    public void shutdown()
    {
        System.out.println("Chiusura del GestoreNotifiche...");
        notificheExecutor.shutdown();
        try
        {
            //attendo massimo 30 secondi per il completamento delle notifiche pendenti
            if (!notificheExecutor.awaitTermination(30, TimeUnit.SECONDS))
            {
                notificheExecutor.shutdownNow();
                if (!notificheExecutor.awaitTermination(10, TimeUnit.SECONDS))
                {
                    System.err.println("L'executor delle notifiche non si è chiuso correttamente");
                }
            }
        }
        catch (InterruptedException e)
        {
            notificheExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("GestoreNotifiche chiuso.");
    }
}

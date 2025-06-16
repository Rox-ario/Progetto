package it.trenical.server.domain.gestore;

import it.trenical.server.domain.NotificheListener;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.dto.NotificaDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GestoreNotifiche
{
    private static GestoreNotifiche instance = null;

    private final Map<String, List<NotificaDTO>> notifichePerCliente;
    //Coda di notifiche per ogni cliente (mi servirà per lo storico delle notifiche)
    private final Map<String, NotificheListener> listenerPerCliente;
    //Listener per notifiche real-time di sessione
    private final List<NotificheListener> listenersGlobali;
    //Mi servono se eventualmente voglio osservare tutte le notifiche inviate nel sistema (indipendetnemente dal tipo)

    private GestoreNotifiche() {
        notifichePerCliente = new ConcurrentHashMap<>();
        listenerPerCliente = new ConcurrentHashMap<>();
        listenersGlobali = new ArrayList<NotificheListener>();
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

        if (cliente == null || !cliente.isRiceviNotifiche()) {
            return;
        }

        //Aggiungo alla coda delle notifiche la notifica
        if (!notifichePerCliente.containsKey(cliente))
            notifichePerCliente.put(idCliente, new ArrayList<>());
        notifichePerCliente.get(idCliente).add(notifica);

        //passo al Listener del cliente e lo/li avviso
        NotificheListener listener = listenerPerCliente.get(idCliente);
        if (listener != null) {
            try {
                listener.onNuovaNotifica(idCliente, notifica);
            } catch (Exception e) {
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

        inviaNotifica(idCliente, notifica);
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
        coda.clear(); // Svuota dopo la lettura
        return notifiche;
    }

    //recupero tutte le notifiche, anche quelle lette, senza cancellarle
    public List<NotificaDTO> getStoricoNotifiche(String idCliente)
    {
        List<NotificaDTO> coda = notifichePerCliente.get(idCliente);
        if (coda == null)
        {
            return new ArrayList<>();
        }
        return new ArrayList<>(coda);
    }


    public int contaNotificheNonLette(String idCliente)
    {
        return notifichePerCliente.size();
    }
}

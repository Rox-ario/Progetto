package it.trenical.server.domain.gestore;

import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.Viaggio;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.dto.NotificaDTO;
import it.trenical.server.dto.RimborsoDTO;
import it.trenical.server.observer.ViaggioETreno.NotificatoreClienteViaggio;
import it.trenical.server.observer.ViaggioETreno.ObserverViaggio;

import java.util.*;

public class GestoreBiglietti
{
    private static GestoreBiglietti instance = null;

    private Map<String, Biglietto> bigliettiPerID;
    private Map<String, List<Biglietto>> bigliettiPerUtente;
    private Map<String, List<Biglietto>> bigliettiPerViaggio;

    private GestoreBiglietti()
    {
        bigliettiPerID = new HashMap<>();
        bigliettiPerUtente = new HashMap<>();
        bigliettiPerViaggio = new HashMap<>();
    }

    public static synchronized GestoreBiglietti getInstance()
    {
        if(instance == null)
            instance = new GestoreBiglietti();
        return instance;
    }

    public void creaBiglietto(String IDViaggio, String IDUtente, ClasseServizio classeServizio)
    {
        GestoreViaggi gv = GestoreViaggi.getInstance();
        GestoreClienti gc = GestoreClienti.getInstance();

        if(gv.getViaggio(IDViaggio) == null)
        {
            throw new IllegalArgumentException("ERRORE: IMPOSSIBILE CREARE IL BIGLIETTO, Il viaggio "+ IDViaggio+" non esiste");
        }
        if(!gc.esisteClienteID(IDUtente))
        {
            throw new IllegalArgumentException("ERRORE: IMPOSSIBILE CREARE IL BIGLIETTO, l'utente "+ IDUtente+" non esiste");
        }
        Biglietto b = new Biglietto(IDViaggio, IDUtente, classeServizio);
        b.inizializzaPrezzoBiglietto(gv.getViaggio(IDViaggio)); //inizializzo il prezzo

        //applico eventuali promozioni
        Cliente clienteBiglietto = gc.getClienteById(IDUtente);
        b.applicaPromozione(clienteBiglietto);

        aggiungiBiglietto(b, IDViaggio, IDUtente);
    }

    private void aggiungiBiglietto(Biglietto b, String IDViaggio, String IDUtente)
    {
        bigliettiPerID.put(b.getID(), b);
        if(!bigliettiPerUtente.containsKey(IDUtente)) //è la prima volta che acquista
        {
            bigliettiPerUtente.put(IDUtente, new ArrayList<>());
            bigliettiPerUtente.get(IDUtente).add(b);
        }
        //il controllo per il viaggio non lo faccio perché quando creo un viaggio aggiorno la mappa qui
        bigliettiPerViaggio.get(IDViaggio).add(b);

        registraClientePerNotificheViaggio(IDUtente, IDViaggio);
    }

    private void registraClientePerNotificheViaggio(String idCliente, String idViaggio)
    {
        GestoreViaggi gv = GestoreViaggi.getInstance();
        GestoreClienti gc = GestoreClienti.getInstance();

        Viaggio viaggio = gv.getViaggio(idViaggio);
        Cliente cliente = gc.getClienteById(idCliente);

        if (viaggio != null && cliente != null && cliente.isRiceviNotifiche())
        {
            ObserverViaggio notificatore = new NotificatoreClienteViaggio(cliente);
            viaggio.attach(notificatore);

            //invio una notifica per la conferma dell'acquisto
            NotificaDTO conferma = new NotificaDTO(
                    "Biglietto acquistato con successo!\n" +
                            "Viaggio: " + viaggio.getId() + "\n" +
                            "Da: " + viaggio.getTratta().getStazionePartenza().getCitta() + "\n" +
                            "A: " + viaggio.getTratta().getStazioneArrivo().getCitta() + "\n" +
                            "Riceverai aggiornamenti su questo viaggio."
            );
            GestoreNotifiche.getInstance().inviaNotifica(idCliente, conferma);
        }
    }

    public void aggiungiViaggio(String IDViaggio)
    {
        bigliettiPerViaggio.put(IDViaggio, new ArrayList<>());
    }

    public RimborsoDTO cancellaBiglietto(Biglietto b)
    {
        bigliettiPerID.remove(b.getID());
        bigliettiPerViaggio.get(b.getIDViaggio()).remove(b);
        bigliettiPerUtente.get(b.getIDCliente()).remove(b);

        //prendo i dati per il rimborso e li restituisco in un DTO
        String idbiglietto = b.getID();
        String idUtente = b.getIDCliente();
        double saldo = b.getPrezzo();
        return new RimborsoDTO(idbiglietto, idUtente, saldo);
    }

    public List<Biglietto> getBigliettiUtente(String IDUtente)
    {
        if(!bigliettiPerUtente.containsKey(IDUtente))
            throw new IllegalArgumentException("Errore: l'utente "+ IDUtente+" non ha biglietti acquistati");
        return bigliettiPerUtente.get(IDUtente);
    }

    public Collection<Biglietto> getBigliettiAttivi()
    {
        return bigliettiPerID.values();
    }

    public List<Biglietto> getBigliettiPerViaggio(String IDViaggio)
    {
        if(!bigliettiPerViaggio.containsKey(IDViaggio))
            throw new IllegalArgumentException("Errore: il viaggio "+ IDViaggio+" non esiste");
        return bigliettiPerViaggio.get(IDViaggio);
    }

    public Biglietto getBigliettoPerID(String ID)
    {
        return bigliettiPerID.get(ID);
    }

    private void modificaBigliettoUtente(String IDBiglietto, String IDUtente, ClasseServizio classeServizio)
    {
        for(Biglietto b : getBigliettiUtente(IDUtente))
        {
            if (b.getID().equals(IDBiglietto))
                b.modificaClasseServizio(classeServizio);
        }
    }

    private void modificaBigliettoViaggio(String IDBiglietto, String IDViaggio, ClasseServizio classeServizio)
    {
        for(Biglietto b : getBigliettiPerViaggio(IDViaggio))
        {
            if (b.getID().equals(IDBiglietto))
                b.modificaClasseServizio(classeServizio);
        }
    }

    public void modificaClasseServizio(String IDBiglietto, String IDUtente, String IDViaggio, ClasseServizio classeServizio)
    {
        bigliettiPerID.get(IDBiglietto).modificaClasseServizio(classeServizio);
        modificaBigliettoUtente(IDBiglietto, IDUtente, classeServizio);
        modificaBigliettoViaggio(IDBiglietto, IDViaggio, classeServizio);
    }
}

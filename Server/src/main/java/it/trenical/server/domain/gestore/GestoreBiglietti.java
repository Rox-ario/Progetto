package it.trenical.server.domain.gestore;

import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.enumerations.ClasseServizio;

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
    }

    public void aggiungiViaggio(String IDViaggio)
    {
        bigliettiPerViaggio.put(IDViaggio, new ArrayList<>());
    }

    public void cancellaBiglietto(Biglietto b)
    {
        bigliettiPerID.remove(b.getID());
        bigliettiPerViaggio.get(b.getIDViaggio()).remove(b);
        bigliettiPerUtente.get(b.getIDCliente()).remove(b);
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
}

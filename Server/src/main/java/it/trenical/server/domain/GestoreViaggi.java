package it.trenical.server.domain;

import java.util.*;

public final class GestoreViaggi
{
    private static GestoreViaggi instance = null;

    private final Map<String, Treno> treni;
    private final Map<String, Tratta> tratte;
    private final Map<String, Viaggio> viaggi;

    private GestoreViaggi() {
        this.treni = new HashMap<>();
        this.tratte = new HashMap<>();
        this.viaggi = new HashMap<>();
    }

    public static synchronized GestoreViaggi getInstance() {
        if (instance == null) {
            instance = new GestoreViaggi();
        }
        return instance;
    }

    public void aggiungiTreno(String id, String tipo, int posti) {
        if (!treni.containsKey(id)) {
            Treno t = new Treno(id, tipo, posti);
            treni.put(id, t);
        }
        else
            System.out.println("Treno "+ id+" già esistente");
    }

    public void aggiungiTratta(Tratta tratta)
    {
        tratte.putIfAbsent(tratta.getId(), tratta);
    }

    private boolean trenoUsato(Treno t, Calendar inizio, Calendar fine)
    {
        for(String id : viaggi.keySet())
        {
            Viaggio viaggio = viaggi.get(id);
            Treno trenoAssociato = viaggio.getTreno();
            Calendar dataInizio = viaggio.getInizioReale();
            Calendar dataFine = viaggio.getFineReale();
            if(t.equals(trenoAssociato))
            {
                if (inizio.before(dataFine) || fine.after(dataInizio))
                    return true; //il treno è usato e si sobrappongono gli orari
            }
        }
        return false;
    }

    public boolean programmaViaggio(String trenoId, String trattaId, Calendar inizio, Calendar fine)
    {
        if (!treni.containsKey(trenoId) || !tratte.containsKey(trattaId)) {
            throw new IllegalArgumentException("Treno o tratta non trovati");
        }
        Treno treno = treni.get(trenoId);
        Tratta tratta = tratte.get(trattaId);
        if(inizio == null || fine == null)
            throw new IllegalArgumentException("Inizio o Fine sono null");
        //controllo che il treno non sia già usato in un altro viaggio
        if(trenoUsato(treno, inizio, fine))
        {
            throw new IllegalStateException("Treno già usato in un altro viaggio e nello stesso orario");
        }
        String idViaggio = UUID.randomUUID().toString();
        Viaggio v = new Viaggio(idViaggio, inizio, fine, treno, tratta);
        viaggi.put(idViaggio, v);
        System.out.println("Viaggio programmato correttamente");
        return true;
    }

    public void cambiaStatoViaggio(String idViaggio, StatoViaggio nuovoStato)
    {
        if(!viaggi.containsKey(idViaggio))
            throw new IllegalArgumentException("Viaggio "+ idViaggio+ " non presente");
        Viaggio v = viaggi.get(idViaggio);

        StatoViaggio vecchioStato = v.getStato(); //giusto per capire se va o meno

        v.setStato(nuovoStato);
        System.out.println("Il viaggio "+ idViaggio+ " è stato aggiornato da "+ vecchioStato +" a "+ nuovoStato);
    }

    public Viaggio getViaggio(String id)
    {
        if(!viaggi.containsKey(id))
            throw new IllegalArgumentException("Viaggio "+ id+ " non presente");
        return viaggi.get(id);
    }

    public Collection<Treno> getTreni()
    {
        return treni.values();
    }

    public Collection<Tratta> getTratte()
    {
        return tratte.values();
    }

    public List<Viaggio> getViaggiPerData(Calendar intervalloinizio, Calendar intervalloFine)
    {
        List<Viaggio> res = new ArrayList<>();
        for(String idViaggio : viaggi.keySet())
        {
            Viaggio v = viaggi.get(idViaggio);
            Calendar inizio = v.getInizioReale(); //aggiornato con i ritardi
            Calendar fine = v.getFineReale(); //aggiornato con i ritardi
            if(fine.after(intervalloinizio) || inizio.before(intervalloFine))
            {
                res.add(v);
            }
        }
        if(res.isEmpty())
        {
           System.out.println("Non ci sono viaggi in queste date");
        }
        return res;
    }

    public void aggiornaRitardoViaggio(String idViaggio, int ritardoInMinuti)
    {
        if(!viaggi.containsKey(idViaggio))
            throw new IllegalArgumentException("Viaggio "+idViaggio+" non presente");
        Viaggio v = viaggi.get(idViaggio);
        v.aggiornaRitardo(ritardoInMinuti);
        System.out.println("Ritardo del Viaggio "+idViaggio+" aggiornato");
    }

    private boolean viaggioInCorso(String idViaggio)
    {
        Viaggio v = viaggi.get(idViaggio);
        if (v.getStato().equals(StatoViaggio.IN_CORSO)
                || v.getStato().equals(StatoViaggio.IN_RITARDO))
            return true;
        return false;
    }

    public void rimuoviViaggio(String idViaggio)
    {
        if (!viaggi.containsKey(idViaggio))
            throw new IllegalArgumentException("Viaggio "+ idViaggio+" non presente");
        if(viaggioInCorso(idViaggio))
            throw new IllegalStateException("Il Viaggio "+ idViaggio+" è in corso e non può essere annullato");
        viaggi.remove(idViaggio);
    }
}

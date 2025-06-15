package it.trenical.server.domain.gestore;

import it.trenical.server.domain.*;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoViaggio;
import it.trenical.server.domain.enumerations.TipoTreno;

import java.util.*;

public final class GestoreViaggi {
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

    public void aggiungiTreno(String id, TipoTreno tipo) {
        if (!treni.containsKey(id)) {
            Treno t = new Treno(id, tipo);
            treni.put(id, t);
        } else
            System.out.println("Treno " + id + " già esistente");
    }

    public void aggiungiTratta(Tratta tratta) {
        tratte.putIfAbsent(tratta.getId(), tratta);
    }

    private boolean trenoUsato(Treno t, Calendar inizio, Calendar fine) {
        for (String id : viaggi.keySet()) {
            Viaggio viaggio = viaggi.get(id);
            Treno trenoAssociato = viaggio.getTreno();
            Calendar dataInizio = viaggio.getInizioReale();
            Calendar dataFine = viaggio.getFineReale();
            if (t.equals(trenoAssociato)) {
                if (inizio.before(dataFine) || fine.after(dataInizio))
                    return true; //il treno è usato e si sobrappongono gli orari
            }
        }
        return false;
    }

    public boolean programmaViaggio(String trenoId, String trattaId, Calendar inizio, Calendar fine) {
        if (!treni.containsKey(trenoId) || !tratte.containsKey(trattaId)) {
            throw new IllegalArgumentException("Treno o tratta non trovati");
        }
        Treno treno = treni.get(trenoId);
        Tratta tratta = tratte.get(trattaId);
        if (inizio == null || fine == null)
            throw new IllegalArgumentException("Inizio o Fine sono null");
        //controllo che il treno non sia già usato in un altro viaggio
        if (trenoUsato(treno, inizio, fine)) {
            throw new IllegalStateException("Treno già usato in un altro viaggio e nello stesso orario");
        }
        String idViaggio = UUID.randomUUID().toString();
        Viaggio v = new Viaggio(idViaggio, inizio, fine, treno, tratta);
        viaggi.put(idViaggio, v);
        System.out.println("Viaggio programmato correttamente");

        GestoreBiglietti gb = GestoreBiglietti.getInstance();
        gb.aggiungiViaggio(v.getId());
        return true;
    }

    public void cambiaStatoViaggio(String idViaggio, StatoViaggio nuovoStato) {
        if (!viaggi.containsKey(idViaggio))
            throw new IllegalArgumentException("Viaggio " + idViaggio + " non presente");
        Viaggio v = viaggi.get(idViaggio);

        StatoViaggio vecchioStato = v.getStato(); //giusto per capire se va o meno

        v.setStato(nuovoStato);
        System.out.println("Il viaggio " + idViaggio + " è stato aggiornato da " + vecchioStato + " a " + nuovoStato);
    }

    public Viaggio getViaggio(String id) {
        if (!viaggi.containsKey(id))
            return null;
        return viaggi.get(id);
    }

    public Collection<Treno> getTreni() {
        return treni.values();
    }

    public Collection<Tratta> getTratte() {
        return tratte.values();
    }

    private boolean stessaData(Calendar data1, Calendar data2) {
        return data1.get(Calendar.DAY_OF_MONTH) == data2.get(Calendar.DAY_OF_MONTH)
                && data1.get(Calendar.MONTH) == data2.get(Calendar.MONTH)
                && data1.get(Calendar.YEAR) == data2.get(Calendar.YEAR);
    }

    public List<Viaggio> getViaggiPerData(Calendar inizioCercato) {
        List<Viaggio> res = new ArrayList<>();
        for (String idViaggio : viaggi.keySet()) {
            Viaggio v = viaggi.get(idViaggio);
            Calendar inizioViaggio = v.getInizioReale(); //aggiornato con i ritardi
            if (stessaData(inizioViaggio, inizioCercato)) {
                res.add(v);
            }
        }
        if (res.isEmpty()) {
            System.out.println("Non ci sono viaggi in queste date");
        }
        return res;
    }

    public void aggiornaRitardoViaggio(String idViaggio, int ritardoInMinuti) {
        if (!viaggi.containsKey(idViaggio))
            throw new IllegalArgumentException("Viaggio " + idViaggio + " non presente");
        Viaggio v = viaggi.get(idViaggio);
        v.aggiornaRitardo(ritardoInMinuti);
        System.out.println("Ritardo del Viaggio " + idViaggio + " aggiornato");
    }

    private boolean viaggioInCorso(String idViaggio) {
        Viaggio v = viaggi.get(idViaggio);
        if (v.getStato() == (StatoViaggio.IN_CORSO)
                || v.getStato() == (StatoViaggio.IN_RITARDO))
            return true;
        return false;
    }

    public void rimuoviViaggio(String idViaggio) {
        if (!viaggi.containsKey(idViaggio))
            throw new IllegalArgumentException("Viaggio " + idViaggio + " non presente");
        if (viaggioInCorso(idViaggio))
            throw new IllegalStateException("Il Viaggio " + idViaggio + " è in corso e non può essere annullato");
        viaggi.remove(idViaggio);
    }

    public Viaggio getViaggioPerTreno(String id) {
        for (String idViaggio : viaggi.keySet()) {
            Viaggio v = viaggi.get(idViaggio);
            Treno t = v.getTreno();
            if (t.getID().equals(id) && v.getStato() != (StatoViaggio.TERMINATO))
                return v;
        }
        throw new IllegalArgumentException("Nessun viaggio in corso per il treno " + id);
    }

    public List<Viaggio> getViaggiPerFiltro(FiltroPasseggeri filtroPasseggeri) {
        if (filtroPasseggeri.isSoloAndata())
            return getViaggiSoloAndata(filtroPasseggeri);
        else
            return getViaggiAndataERitorno(filtroPasseggeri);
    }

    private List<Viaggio> getViaggiSoloAndata(FiltroPasseggeri filtroPasseggeri) {
        //Tiro fuori i maledetti parametri
        int numeroPasseggeri = filtroPasseggeri.getNumero();
        ClasseServizio classeServizio = filtroPasseggeri.getClasseServizio();
        TipoTreno tipoTreno = filtroPasseggeri.getTipoTreno();
        Calendar dataInizio = filtroPasseggeri.getDataInizio();
        String cittaDiAndata = filtroPasseggeri.getCittaDiAndata();
        String cittaDiArrivo = filtroPasseggeri.getCittaDiArrivo();

        //Ottiengo i viaggi in quel giormo
        List<Viaggio> viaggiDelGiorno = getViaggiPerData(dataInizio);

        //Passo allo scanning, utilizzo una copia della lista così nel mentre posso modificare quella originaria
        for (Viaggio v : new ArrayList<Viaggio>(viaggiDelGiorno)) {
            int postiDisponibiliPerClasse = v.getPostiDisponibiliPerClasse(classeServizio);
            TipoTreno trenoUsato = v.getTreno().getTipo();
            String cittaAndata = v.getTratta().getStazionePartenza().getCitta();
            String cittaArrivo = v.getTratta().getStazioneArrivo().getCitta();
            if (postiDisponibiliPerClasse < numeroPasseggeri ||
                    trenoUsato != tipoTreno || !cittaAndata.equals(cittaDiAndata) || !cittaArrivo.equals(cittaDiArrivo))
                viaggiDelGiorno.remove(v);
        }

        return viaggiDelGiorno;
    }

    private List<Viaggio> getViaggiAndataERitorno(FiltroPasseggeri filtroPasseggeri) {
        int numeroPasseggeri = filtroPasseggeri.getNumero();
        ClasseServizio classeServizio = filtroPasseggeri.getClasseServizio();
        TipoTreno tipoTreno = filtroPasseggeri.getTipoTreno();
        Calendar dataRitorno = filtroPasseggeri.getDataRitorno();
        String cittaDiAndata = filtroPasseggeri.getCittaDiAndata();
        String cittaDiArrivo = filtroPasseggeri.getCittaDiArrivo();

        List<Viaggio> risultati = new ArrayList<>();

        //Per prima cosa cerco i viaggi di andata
        List<Viaggio> viaggiAndata = getViaggiSoloAndata(filtroPasseggeri);
        risultati.addAll(viaggiAndata);

        // dopodiché ritorno i viaggi di ritorno, mi sa che devo creare un filtro di ritorno
        FiltroPasseggeri filtroRitorno = new FiltroPasseggeri(
                numeroPasseggeri, classeServizio, tipoTreno,
                dataRitorno, null, true, cittaDiArrivo, cittaDiAndata);
        List<Viaggio> viaggiRitorno = getViaggiSoloAndata(filtroRitorno);
        risultati.addAll(viaggiRitorno);

        //TODO eventualmente ordinare la lista o restituire un HashMap
        return risultati;
    }
}

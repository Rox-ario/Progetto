package it.trenical.server.domain;

import it.trenical.server.observer.ObserverViaggio;
import it.trenical.server.observer.SoggettoViaggio;

import java.util.*;

public class Viaggio extends SoggettoViaggio
{
    private final String id;
    private final Calendar inizio;
    private final Calendar fine;
    private final Treno treno;
    private final Tratta tratta;
    private StatoViaggio stato;
    private int postiDisponibili;
    private final Map<String, Integer> binari; //string partenza/arrivo, int binario
    private int ritardoMinuti = 0;
    private List<ObserverViaggio> osservatori;

    public Viaggio(String id, Calendar inizio, Calendar fine, Treno treno, Tratta tratta)
    {
        this.id = id;
        this.inizio = inizio;
        this.fine = fine;
        this.treno = treno;
        this.tratta = tratta;
        this.binari = new HashMap<>();
       this.stato =  StatoViaggio.PROGRAMMATO;
        this.postiDisponibili = treno.getPosti();
        this.osservatori = new ArrayList<ObserverViaggio>();
    }

    public String getId() {
        return id;
    }

    public Treno getTreno() {
        return treno;
    }

    public Tratta getTratta() {
        return tratta;
    }

    public StatoViaggio getStato() {
        return stato;
    }

    public void setStato(StatoViaggio stato)
    {
        this.stato = stato;
    }

    public int getPostiDisponibili() {
        return postiDisponibili;
    }

    public boolean riduciPostiDisponibili(int n)
    {
        if(postiDisponibili - n < 0)
        {
            return false;
        }
        else
            postiDisponibili -= n;
        return true;
    }

    public void setBinario(String tipo, int numero) {
        binari.put(tipo, numero);
    }

    public int getBinario(String tipo) {
        return binari.getOrDefault(tipo, -1);
    }

    public void aggiornaRitardo(int minuti)
    {
        if(minuti < 0)
        {
            throw new IllegalArgumentException("Il valore di ritardo non può essere negativo");
        }
        ritardoMinuti += minuti;
        this.stato = StatoViaggio.IN_RITARDO;
        notifica();
    }

    public Calendar getInizioReale()
    {
        Calendar clone = (Calendar) inizio.clone(); //faccio la clone dell'inizio così non lo modifico, tanto devo solo mostrare il ritardo eventuale
        clone.add(Calendar.MINUTE, ritardoMinuti);
        return clone;
    }

    public Calendar getFineReale()
    {
        Calendar clone = (Calendar) fine.clone(); //idem per inizio
        clone.add(Calendar.MINUTE, ritardoMinuti);
        return clone;
    }

    @Override
    public void attach(ObserverViaggio ob)
    {
        osservatori.add(ob);
    }

    @Override
    public void detach(ObserverViaggio ob)
    {
        osservatori.remove(ob);
    }

    @Override
    public void notifica()
    {
        for(ObserverViaggio obs : osservatori)
        {
            obs.aggiorna(this);
        }
    }
}

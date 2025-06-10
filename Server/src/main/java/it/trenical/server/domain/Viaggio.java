package it.trenical.server.domain;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Viaggio
{
    private final String id;
    private final Calendar inizio;
    private final Calendar fine;
    private final Treno treno;
    private final Tratta tratta;
    private StatoViaggio stato;
    private int postiDisponibili;
    private final Map<String, Integer> binari; //string partenza/arrivo, int binario

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
    }

    public String getId() {
        return id;
    }

    public Calendar getInizio() {
        return inizio;
    }

    public Calendar getFine() {
        return fine;
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
}

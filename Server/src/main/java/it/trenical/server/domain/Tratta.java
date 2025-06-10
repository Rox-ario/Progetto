package it.trenical.server.domain;

import java.util.ArrayList;

public class Tratta
{
    private final String id;
    private ArrayList<Stazione> stazioni;

    public Tratta(String id) {
        this.id = id;
        this.stazioni = new ArrayList<Stazione>();
    }

    public String getId() {
        return id;
    }

    public ArrayList<Stazione> getStazioni() {
        return stazioni;
    }

    public void aggiungiStazione(Stazione s)
    {
        if(s != null)
        {
            stazioni.add(s);
        }
    }
}

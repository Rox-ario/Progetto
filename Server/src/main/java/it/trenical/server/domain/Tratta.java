package it.trenical.server.domain;

import java.util.ArrayList;

public class Tratta
{
    private final String id;
    private final Stazione StazionePartenza;
    private final Stazione StazioneArrivo;

    public Tratta(String id, Stazione stazionePartenza, Stazione stazioneArrivo) {
        this.id = id;
        if(stazioneArrivo.equals(stazionePartenza))
            throw new IllegalArgumentException("Stazione di Arrivo non può essere uguale alla stazione di partenza");
        StazionePartenza = stazionePartenza;
        StazioneArrivo = stazioneArrivo;
    }

    public Stazione getStazionePartenza() {
        return StazionePartenza;
    }

    public Stazione getStazioneArrivo() {
        return StazioneArrivo;
    }

    public String getId() {
        return id;
    }

}

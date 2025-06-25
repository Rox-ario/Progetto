package it.trenical.server.domain;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

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

    public Tratta(Stazione stazionePartenza, Stazione stazioneArrivo) {
        this.id = UUID.randomUUID().toString();
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tratta tratta)) return false;
        return Objects.equals(id, tratta.id) && Objects.equals(StazionePartenza, tratta.StazionePartenza) && Objects.equals(StazioneArrivo, tratta.StazioneArrivo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, StazionePartenza, StazioneArrivo);
    }

    @Override
    public String toString() {
        return "[" +
                "id='" + id + '\'' +
                ", StazionePartenza=" + StazionePartenza +
                ", StazioneArrivo=" + StazioneArrivo +
                ']';
    }
}

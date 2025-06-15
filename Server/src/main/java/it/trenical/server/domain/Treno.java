package it.trenical.server.domain;

import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.TipoTreno;

import java.util.HashMap;
import java.util.Map;

public class Treno
{
    private final String ID;
    private final TipoTreno tipo;
    private final Map<ClasseServizio, Integer> postiPerClasse;

    public Treno(String ID, TipoTreno tipo) {
        this.ID = ID;
        this.tipo = tipo;
        postiPerClasse = new HashMap<>();
        if(tipo == TipoTreno.ITALO)
        {
            postiPerClasse.put(ClasseServizio.LOW_COST, 100);
            postiPerClasse.put(ClasseServizio.ECONOMY, 70);
            postiPerClasse.put(ClasseServizio.BUSINESS, 50);
            postiPerClasse.put(ClasseServizio.FEDELTA, 70);
        }
        else if(tipo == TipoTreno.COMFORT)
        {
            postiPerClasse.put(ClasseServizio.LOW_COST, 70);
            postiPerClasse.put(ClasseServizio.ECONOMY, 60);
            postiPerClasse.put(ClasseServizio.BUSINESS, 50);
            postiPerClasse.put(ClasseServizio.FEDELTA, 30);
        }
        else if(tipo == TipoTreno.INTERCITY)
        {
            postiPerClasse.put(ClasseServizio.LOW_COST, 40);
            postiPerClasse.put(ClasseServizio.ECONOMY, 30);
            postiPerClasse.put(ClasseServizio.BUSINESS, 20);
            postiPerClasse.put(ClasseServizio.FEDELTA, 10);
        }
        else
            throw new IllegalArgumentException("Tipo treno "+ tipo + " non registrato");
    }

    public String getID() {
        return ID;
    }

    public int getPostiPerClasse(ClasseServizio classeServizio)
    {
        if(!postiPerClasse.containsKey(classeServizio))
            throw new IllegalArgumentException("Errore: classe di servizio "+ classeServizio+" non registrata");
        return postiPerClasse.get(classeServizio);
    }

    public Map<ClasseServizio, Integer> getPosti()
    {
        return postiPerClasse;
    }

    public TipoTreno getTipo()
    {
        return tipo;
    }

    @Override
    public String toString() {
        return "Treno{" +
                "ID='" + ID + '\'' +
                ", tipo=" + tipo +
                ", postiPerClasse=" + postiPerClasse +
                '}';
    }
}

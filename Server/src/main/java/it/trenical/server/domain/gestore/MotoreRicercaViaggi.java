package it.trenical.server.domain.gestore;

import it.trenical.server.domain.FiltroPasseggeri;
import it.trenical.server.domain.Viaggio;

import java.util.List;

public class MotoreRicercaViaggi
{
    private static MotoreRicercaViaggi instance = null;
    private final GestoreViaggi gestoreViaggi;


    private MotoreRicercaViaggi()
    {
        gestoreViaggi = GestoreViaggi.getInstance();
    }

    public static synchronized MotoreRicercaViaggi getInstance()
    {
        if(instance == null)
            instance = new MotoreRicercaViaggi();
        return instance;
    }

    //cerco Viaggi in base al filtro fornito
    public List<Viaggio> cercaViaggio(FiltroPasseggeri filtroPasseggeri)
    {
        return gestoreViaggi.getViaggiPerFiltro(filtroPasseggeri);
    }
}

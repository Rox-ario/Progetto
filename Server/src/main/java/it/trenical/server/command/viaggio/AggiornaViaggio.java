package it.trenical.server.command.viaggio;

import it.trenical.server.domain.gestore.GestoreViaggi;
import it.trenical.server.domain.enumerations.StatoViaggio;

public class AggiornaViaggio implements ComandoViaggio
{
    private final String idViaggio;
    private final StatoViaggio stato;

    public AggiornaViaggio(String idViaggio, StatoViaggio stato) {
        this.idViaggio = idViaggio;
        this.stato = stato;
    }

    @Override
    public void esegui() throws Exception
    {
        GestoreViaggi gc = GestoreViaggi.getInstance();
        gc.cambiaStatoViaggio(idViaggio, stato);
        //mi sono già anche qui occupato di controllare se il viaggio esista in GestoreViaggi
    }
}

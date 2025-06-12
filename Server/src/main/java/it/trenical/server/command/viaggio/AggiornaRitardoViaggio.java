package it.trenical.server.command.viaggio;

import it.trenical.server.domain.GestoreViaggi;

public class AggiornaRitardoViaggio implements ComandoViaggio
{
    private final String idViaggio;
    private final int ritardo;

    public AggiornaRitardoViaggio(String idViaggio, int ritardo) {
        this.idViaggio = idViaggio;
        this.ritardo = ritardo;
    }

    @Override
    public void esegui() throws Exception
    {
        GestoreViaggi gc = GestoreViaggi.getInstance();
        gc.aggiornaRitardoViaggio(idViaggio, ritardo);
        //mi sono già oxxupato in GestoreViaggi del caso in cui il viaggio non ci sia
    }
}

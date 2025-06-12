package it.trenical.server.command.viaggio;

import it.trenical.server.domain.gestore.GestoreViaggi;

public class AnnullaViaggio implements ComandoViaggio
{
    private final String idViaggio;

    public AnnullaViaggio(String idViaggio) {
        this.idViaggio = idViaggio;
    }

    @Override
    public void esegui() throws Exception
    {
        GestoreViaggi gc = GestoreViaggi.getInstance();
        gc.rimuoviViaggio(idViaggio);
    }
}

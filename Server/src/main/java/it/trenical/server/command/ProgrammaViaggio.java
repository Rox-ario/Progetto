package it.trenical.server.command;

import it.trenical.server.domain.GestoreViaggi;

import java.util.Calendar;

public class ProgrammaViaggio implements ComandoViaggio
{
    private final String idTreno;
    private final String idTratta;
    private final Calendar inizio;
    private final Calendar fine;

    public ProgrammaViaggio(String idTreno, String idTratta, Calendar inizio, Calendar fine) {
        this.idTreno = idTreno;
        this.idTratta = idTratta;
        this.inizio = inizio;
        this.fine = fine;
    }

    @Override
    public void esegui() throws Exception
    {
        GestoreViaggi gc = GestoreViaggi.getInstance();
        gc.programmaViaggio(idTreno, idTratta, inizio, fine);
    }
}

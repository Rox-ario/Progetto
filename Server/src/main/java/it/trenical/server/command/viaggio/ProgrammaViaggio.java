package it.trenical.server.command.viaggio;

import it.trenical.server.domain.Viaggio;
import it.trenical.server.domain.gestore.GestoreViaggi;

import java.util.Calendar;

public class ProgrammaViaggio implements ComandoViaggio
{
    private final String idTreno;
    private final String idTratta;
    private final Calendar inizio;
    private final Calendar fine;
    private Viaggio v;

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
        this.v = gc.programmaViaggio(idTreno, idTratta, inizio, fine);
    }

    public Viaggio getViaggio()
    {
        return v;
    }
}

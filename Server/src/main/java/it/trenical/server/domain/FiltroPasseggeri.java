package it.trenical.server.domain;

import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.TipoTreno;

import java.util.Calendar;

public class FiltroPasseggeri
{
    private final int numero;
    private final ClasseServizio classeServizio;
    private final TipoTreno tipoTreno;
    private final Calendar dataInizio;
    private final Calendar dataRitorno;
    private final boolean soloAndata;
    private final String cittaDiAndata;
    private final String cittaDiArrivo;

    public FiltroPasseggeri(int numero,
                            ClasseServizio classeServizio,
                            TipoTreno tipoTreno,
                            Calendar dataInizio, Calendar dataRitorno,
                            boolean soloAndata,
                            String cittaDiAndata,
                            String cittaDiArrivo) {
        this.numero = numero;
        this.classeServizio = classeServizio;
        this.tipoTreno = tipoTreno;
        this.dataInizio = dataInizio;
        this.dataRitorno = dataRitorno;
        this.soloAndata = soloAndata;
        this.cittaDiAndata = cittaDiAndata;
        this.cittaDiArrivo = cittaDiArrivo;
    }

    public int getNumero() {
        return numero;
    }

    public ClasseServizio getClasseServizio() {
        return classeServizio;
    }

    public TipoTreno getTipoTreno() {
        return tipoTreno;
    }

    public Calendar getDataInizio() {
        return dataInizio;
    }

    public Calendar getDataRitorno() {
        return dataRitorno;
    }

    public String getCittaDiAndata() {
        return cittaDiAndata;
    }

    public String getCittaDiArrivo() {
        return cittaDiArrivo;
    }

    public boolean isSoloAndata() {
        return soloAndata;
    }


}

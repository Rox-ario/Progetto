package it.trenical.server.domain;

import it.trenical.server.domain.enumerations.StatoPromozione;

import java.util.Calendar;
import java.util.UUID;

public class PromozioneFedelta implements Promozione
{
    StatoPromozione statoPromozione;
    String ID;
    private final Calendar dataInizio;
    private final Calendar dataFine;
    private final double percentualeSconto;

    public PromozioneFedelta(Calendar dataInizio, Calendar dataFine, double percentualeSconto, boolean perFedelta)
    {
        if(dataInizio.before(Calendar.getInstance()))
            throw new IllegalArgumentException("Errore: La promozione non può avere data di inizio PRIMA di oggi");
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.percentualeSconto = percentualeSconto;
        this.ID = UUID.randomUUID().toString();
        if(Calendar.getInstance().after(dataInizio))
            statoPromozione = StatoPromozione.ATTIVA;
        else
            statoPromozione = StatoPromozione.PROGRAMMATA;
    }

    public StatoPromozione getStatoPromozione()
    {
        return statoPromozione;
    }

    public String getID() {
        return ID;
    }

    public Calendar getDataInizio() {
        return dataInizio;
    }

    public Calendar getDataFine() {
        return dataFine;
    }

    public double getPercentualeSconto() {
        return percentualeSconto;
    }
}

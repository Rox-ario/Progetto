package it.trenical.server.domain;

import it.trenical.server.domain.enumerations.StatoPromozione;

import java.util.Calendar;
import java.util.UUID;

public class PromozioneTratta implements Promozione
{
    private final Tratta tratta;
    StatoPromozione statoPromozione;
    String ID;
    private final Calendar dataInizio;
    private final Calendar dataFine;
    private double percentualeSconto;


    public PromozioneTratta(Tratta tratta, Calendar dataInizio, Calendar dataFine, double percentualeSconto)
    {
        if(dataInizio.before(Calendar.getInstance()))
            throw new IllegalArgumentException("Errore: La promozione non può avere data di inizio PRIMA di oggi");
        if(dataInizio.after(dataFine))
            throw new IllegalArgumentException("Errore: La fine della promozione non può precedere l'inizio.");
        if(tratta == null)
            throw new IllegalArgumentException("Errore: la tratta non esiste");
        this.tratta = tratta;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.statoPromozione = StatoPromozione.PROGRAMMATA;
        this.percentualeSconto = percentualeSconto;
        this.ID = UUID.randomUUID().toString();
    }

    @Override
    public StatoPromozione getStatoPromozione() {
        return statoPromozione;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public Calendar getDataInizio() {
        return dataInizio;
    }

    @Override
    public Calendar getDataFine() {
        return dataFine;
    }

    @Override
    public void setStatoPromozioneATTIVA()
    {
        if(isProgrammata())
            statoPromozione = StatoPromozione.ATTIVA;
        else
            System.out.println("La promozione "+ ID+" è già ATTIVA");
    }

    @Override
    public void setStatoPromozionePROGRAMMATA()
    {
        if(isAttiva())
            System.out.println("La promozione "+ ID+" è già ATTIVA, NON PUO' ESSERE PROGRAMMATA");
        else
            statoPromozione = StatoPromozione.PROGRAMMATA;
    }

    @Override
    public double applicaSconto(double prezzo)
    {
        double differenza = prezzo * percentualeSconto;
        return prezzo - differenza;
    }

    @Override
    public boolean isAttiva()
    {
        return statoPromozione == StatoPromozione.ATTIVA;
    }

    @Override
    public boolean isProgrammata()
    {
        return statoPromozione == StatoPromozione.PROGRAMMATA;
    }

    @Override
    public double getPercentualeSconto()
    {
        return percentualeSconto;
    }

    @Override
    public void setPercentualeSconto(double NEWpercentualeSconto)
    {
        if(isAttiva())
            throw new IllegalStateException("La promozione "+ ID+" è ATTIVA, non si può modificare la percentuale di sconto");
        else
            this.percentualeSconto = NEWpercentualeSconto;
    }

    public boolean isApplicabile(Viaggio viaggio)
    {
        return viaggio.getTratta().equals(tratta);
    }

    public Tratta getTratta() {
        return tratta;
    }
}

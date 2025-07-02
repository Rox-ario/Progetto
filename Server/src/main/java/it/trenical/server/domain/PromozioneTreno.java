package it.trenical.server.domain;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.StatoPromozione;
import it.trenical.server.domain.enumerations.TipoPromozione;
import it.trenical.server.domain.enumerations.TipoTreno;

import java.util.Calendar;
import java.util.UUID;

public class PromozioneTreno implements Promozione
{
    StatoPromozione statoPromozione;
    String ID;
    private final Calendar dataInizio;
    private final Calendar dataFine;
    private double percentualeSconto;//non la facciamo final così se voglio cambiare la percentuale posso ancora farlo
    private TipoTreno tipoTreno;
    private final TipoPromozione tipo = TipoPromozione.TRENO;


    public PromozioneTreno(Calendar dataInizio, Calendar dataFine, double percentualeSconto, TipoTreno tipoTreno)
    {
       if(dataInizio.after(dataFine))
            throw new IllegalArgumentException("Errore: La fine della promozione non può precedere l'inizio.");
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.percentualeSconto = percentualeSconto;
        this.ID = UUID.randomUUID().toString();
        this.statoPromozione = StatoPromozione.PROGRAMMATA;
        this.tipoTreno = tipoTreno;
    }

    public TipoPromozione getTipo() {
        return tipo;
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

    public boolean isApplicabile(Viaggio v)
    {
        return v.getInizioReale().after(dataInizio) && v.getFineReale().before(dataFine);
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

    public TipoTreno getTipoTreno()
    {
        return tipoTreno;
    }

    @Override
    public String toString() {
        return "[" +
                "tipo=" + tipo +
                "statoPromozione=" + statoPromozione +
                ", ID='" + ID  +
                ", dataInizio=" + dataInizio +
                ", dataFine=" + dataFine +
                ", percentualeSconto=" + percentualeSconto +
                ", tipoTreno=" + tipoTreno +
                ']';
    }
}


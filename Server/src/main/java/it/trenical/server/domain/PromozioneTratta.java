package it.trenical.server.domain;

import it.trenical.server.domain.enumerations.StatoPromozione;
import it.trenical.server.domain.enumerations.TipoPromozione;

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
    private final TipoPromozione tipo = TipoPromozione.TRATTA;


    public PromozioneTratta(Tratta tratta, Calendar dataInizio, Calendar dataFine, double percentualeSconto)
    {
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

    public PromozioneTratta(String id, Tratta tratta, Calendar dataInizio, Calendar dataFine, double percentualeSconto)
    {
        if(dataInizio.after(dataFine))
            throw new IllegalArgumentException("Errore: La fine della promozione non può precedere l'inizio.");
        if(tratta == null)
            throw new IllegalArgumentException("Errore: la tratta non esiste");
        this.tratta = tratta;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.statoPromozione = StatoPromozione.PROGRAMMATA;
        this.percentualeSconto = percentualeSconto;
        this.ID = id;
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

    @Override
    public String toString() {
        return "[" +
                "tipo= " + tipo +
                ", tratta= " + tratta +
                ", statoPromozione= " + statoPromozione +
                ", ID= " + ID +
                ", dataInizio= " + dataInizio.get(Calendar.DAY_OF_MONTH) +"/"+(dataInizio.get(Calendar.MONTH)+1)+"/"+dataInizio.get(Calendar.YEAR)+
                ", dataFine= " + dataFine.get(Calendar.DAY_OF_MONTH) +"/"+(dataFine.get(Calendar.MONTH)+1)+"/"+dataFine.get(Calendar.YEAR) +
                ", percentualeSconto= " + percentualeSconto +
                ']';
    }
}

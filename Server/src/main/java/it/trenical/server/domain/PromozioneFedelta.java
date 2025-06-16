package it.trenical.server.domain;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.StatoPromozione;
import it.trenical.server.domain.enumerations.TipoPromozione;
import it.trenical.server.dto.NotificaDTO;
import it.trenical.server.observer.Promozione.ObserverPromozione;
import it.trenical.server.observer.Promozione.SoggettoPromozione;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class PromozioneFedelta extends SoggettoPromozione implements Promozione
{
    private List<ObserverPromozione> osservatori;
    StatoPromozione statoPromozione;
    String ID;
    private final Calendar dataInizio;
    private final Calendar dataFine;
    private double percentualeSconto;//non la facciamo final così se voglio cambiare la percentuale posso ancora farlo
    private final TipoPromozione tipo = TipoPromozione.FEDELTA;

    public PromozioneFedelta(Calendar dataInizio, Calendar dataFine, double percentualeSconto)
    {
        if(dataInizio.before(Calendar.getInstance()))
            throw new IllegalArgumentException("Errore: La promozione non può avere data di inizio PRIMA di oggi");
        if(dataInizio.after(dataFine))
            throw new IllegalArgumentException("Errore: La fine della promozione non può precedere l'inizio.");
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.percentualeSconto = percentualeSconto;
        this.ID = UUID.randomUUID().toString();
        this.statoPromozione = StatoPromozione.PROGRAMMATA;
        osservatori = new ArrayList<ObserverPromozione>();
    }

    public TipoPromozione getTipo() {
        return tipo;
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

    public double getPercentualeSconto()
    {
        return percentualeSconto;
    }

    public boolean isAttiva()
    {
        return statoPromozione == StatoPromozione.ATTIVA;
    }

    public boolean isProgrammata()
    {
        return statoPromozione == StatoPromozione.PROGRAMMATA;
    }

    public boolean isApplicabile(Cliente c)
    {
        return c.haAdesioneFedelta();
    }

    public void setPercentualeSconto(double NEWpercentualeSconto)
    {
        if(isAttiva())
            throw new IllegalStateException("La promozione "+ ID+" è ATTIVA, non si può modificare la percentuale di sconto");
        else
            this.percentualeSconto = NEWpercentualeSconto;
    }

    @Override
    public void attach(ObserverPromozione observerPromozione)
    {
        osservatori.add(observerPromozione);
    }

    @Override
    public void detach(ObserverPromozione observerPromozione)
    {
        osservatori.remove(observerPromozione);
    }

    @Override
    public void notifica()
    {
        for(ObserverPromozione observerPromozione : osservatori)
            observerPromozione.aggiorna(this);
    }

    public NotificaDTO getNotifica()
    {
        String messaggio = "Nuova promozione Fedelà!!: "+toString();
        NotificaDTO dto = new NotificaDTO(messaggio);
        return dto;
    }

    @Override
    public String toString() {
        return "PromozioneFedelta{" +
                ", statoPromozione=" + statoPromozione +
                ", ID='" + ID + '\'' +
                ", dataInizio=" + dataInizio +
                ", dataFine=" + dataFine +
                ", percentualeSconto=" + percentualeSconto +
                '}';
    }
}

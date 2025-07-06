package it.trenical.server.domain;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.StatoPromozione;
import it.trenical.server.domain.enumerations.TipoPromozione;
import it.trenical.server.dto.NotificaDTO;
import it.trenical.server.observer.Promozione.ObserverPromozione;
import it.trenical.server.observer.Promozione.ObserverPromozioneFedelta;
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
        if(dataInizio.after(dataFine))
            throw new IllegalArgumentException("Errore: La fine della promozione non può precedere l'inizio.");
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.percentualeSconto = percentualeSconto;
        this.ID = UUID.randomUUID().toString();
        this.statoPromozione = StatoPromozione.PROGRAMMATA;
        osservatori = new ArrayList<ObserverPromozione>();
    }

    //per il ritiro da DB
    public PromozioneFedelta(String id, Calendar dataInizio, Calendar dataFine, double percentualeSconto)
    {
        if(dataInizio.after(dataFine))
            throw new IllegalArgumentException("Errore: La fine della promozione non può precedere l'inizio.");
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.percentualeSconto = percentualeSconto;
        this.ID = id;
        this.statoPromozione = StatoPromozione.PROGRAMMATA;
        osservatori = new ArrayList<ObserverPromozione>();
    }

    public TipoPromozione getTipo() {
        return tipo;
    }

    public List<ObserverPromozione> getObservers()
    {
        return osservatori;
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
        // Verifica se l'observer è già presente per evitare duplicati
        if (observerPromozione instanceof ObserverPromozioneFedelta newObs)
        {
            String clienteId = newObs.getCliente().getId();

            // Controlla se esiste già un observer per questo cliente
            for (ObserverPromozione obs : osservatori)
            {
                if (obs instanceof ObserverPromozioneFedelta existingObs)
                {
                    if (existingObs.getCliente().getId().equals(clienteId))
                    {
                        System.out.println("Observer già registrato per cliente: " + clienteId);
                        return; //Non aggiungo duplicati
                    }
                }
            }
        }

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
        String messaggio = "Nuova promozione Fedeltà!!: "+toString();
        return new NotificaDTO(messaggio);
    }

    @Override
    public String toString() {
        return "[" +
                "tipo= " + tipo +
                ", statoPromozione= " + statoPromozione +
                ", ID= " + ID +
                ", dataInizio= " + dataInizio.get(Calendar.DAY_OF_MONTH) +"/"+(dataInizio.get(Calendar.MONTH)+1)+"/"+dataInizio.get(Calendar.YEAR)+
                ", dataFine= " + dataFine.get(Calendar.DAY_OF_MONTH) +"/"+(dataFine.get(Calendar.MONTH)+1)+"/"+dataFine.get(Calendar.YEAR) +
                ", percentualeSconto= " + percentualeSconto +
                ']';
    }
}

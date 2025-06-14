package it.trenical.server.command.promozione;

import it.trenical.server.domain.Promozione;
import it.trenical.server.domain.PromozioneFedelta;
import it.trenical.server.domain.Tratta;
import it.trenical.server.domain.Treno;
import it.trenical.server.domain.enumerations.TipoPromozione;
import it.trenical.server.domain.gestore.CatalogoPromozione;
import it.trenical.server.factoryMethod.PromozioneFactory;
import it.trenical.server.factoryMethod.PromozioneFactoryFedelta;
import it.trenical.server.factoryMethod.PromozioneFactoryTratta;
import it.trenical.server.factoryMethod.PromozioneFactoryTreno;

import java.util.Calendar;

public class CreaPromozioneCommand implements PromozioneCommand
{
    private final TipoPromozione tipo;
    private final Calendar dataInizio;
    private final Calendar dataFine;
    private double sconto;
    private final Tratta tratta;
    private final Treno treno;

    public CreaPromozioneCommand(TipoPromozione tipo, Calendar dataInizio, Calendar dataFine, double sconto, Tratta tratta, Treno treno) {
        this.tipo = tipo;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.sconto = sconto;
        this.tratta = tratta;
        this.treno = treno;
    }


    @Override
    public void esegui()
    {
        CatalogoPromozione cp = CatalogoPromozione.getInstance();
        if(tipo == TipoPromozione.FEDELTA)
        {
            PromozioneFactory pm = new PromozioneFactoryFedelta();
            Promozione pf = pm.creaPromozione(dataInizio, dataFine, sconto);
            cp.aggiungiPromozione(pf);
        }
        if(tipo == TipoPromozione.TRATTA)
        {
            if(tratta == null)
                throw new IllegalArgumentException("Errore: la tratta non può essere null se stai creando una promozione per Tratta");
            PromozioneFactory pm = new PromozioneFactoryTratta(tratta);
            Promozione pf = pm.creaPromozione(dataInizio, dataFine, sconto);
            cp.aggiungiPromozione(pf);
        }
        if(tipo == TipoPromozione.TRENO)
        {
            if(treno == null)
                throw new IllegalArgumentException("Errore: il treno non può essere null se stai creando una promozione per Treno");
            PromozioneFactory pm = new PromozioneFactoryTreno(treno.getTipo());
            Promozione pf = pm.creaPromozione(dataInizio, dataFine, sconto);
            cp.aggiungiPromozione(pf);
        }
    }
}

package it.trenical.server.factoryMethod;

import it.trenical.server.domain.Promozione;
import it.trenical.server.domain.PromozioneTreno;
import it.trenical.server.domain.enumerations.TipoTreno;

import java.util.Calendar;

public class PromozioneFactoryTreno extends PromozioneFactory
{
    private final TipoTreno tipoTreno;

    public PromozioneFactoryTreno(TipoTreno tipoTreno) {
        this.tipoTreno = tipoTreno;
    }

    @Override
    public PromozioneTreno creaPromozione(Calendar dataInizio, Calendar dataFine, double sconto)
    {
        return new PromozioneTreno(dataInizio,dataFine,sconto, tipoTreno);
    }
}

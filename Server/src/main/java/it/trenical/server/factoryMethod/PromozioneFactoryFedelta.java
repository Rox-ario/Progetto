package it.trenical.server.factoryMethod;

import it.trenical.server.domain.PromozioneFedelta;
import it.trenical.server.domain.PromozioneTratta;

import java.util.Calendar;

public class PromozioneFactoryFedelta extends PromozioneFactory
{
    public PromozioneFedelta creaPromozione(Calendar dataInizio, Calendar dataFine, double sconto)
    {
        return new PromozioneFedelta(dataInizio, dataFine, sconto);
    }
}

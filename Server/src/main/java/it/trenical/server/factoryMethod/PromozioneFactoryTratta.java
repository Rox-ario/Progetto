package it.trenical.server.factoryMethod;

import it.trenical.server.domain.PromozioneTratta;
import it.trenical.server.domain.Tratta;

import java.util.Calendar;

public class PromozioneFactoryTratta extends PromozioneFactory
{
    private final Tratta tratta;

    public PromozioneFactoryTratta(Tratta tratta)
    {
        if(tratta == null)
            throw new IllegalArgumentException("Errore: la tratta è null");
        this.tratta = tratta;
    }

    @Override
    public PromozioneTratta creaPromozione(Calendar dataInizio, Calendar dataFine, double sconto)
    {
        return new PromozioneTratta(tratta, dataInizio, dataFine, sconto);
    }
}

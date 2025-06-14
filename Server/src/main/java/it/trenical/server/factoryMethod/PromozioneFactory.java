package it.trenical.server.factoryMethod;

import it.trenical.server.domain.Promozione;

import java.util.Calendar;

public abstract class PromozioneFactory
{
    public abstract Promozione creaPromozione(Calendar dataInizio, Calendar dataFine, double sconto);
}

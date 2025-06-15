package it.trenical.server.observer.Promozione;


public abstract class SoggettoPromozione
{
    public abstract void attach(ObserverPromozione observerPromozione);
    public abstract void detach(ObserverPromozione observerPromozione);
    public abstract void notifica();
}

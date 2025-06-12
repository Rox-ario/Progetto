package it.trenical.server.observer;

public abstract class SoggettoViaggio
{
    public abstract void attach(ObserverViaggio ob);
    public abstract void detach(ObserverViaggio ob);
    public abstract void notificaCambiamentoViaggio();
}

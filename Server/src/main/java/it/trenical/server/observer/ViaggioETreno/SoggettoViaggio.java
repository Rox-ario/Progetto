package it.trenical.server.observer.ViaggioETreno;

public abstract class SoggettoViaggio
{
    public abstract void attach(ObserverViaggio ob);
    public abstract void detach(ObserverViaggio ob);
    public abstract void notificaCambiamentoViaggio();
}

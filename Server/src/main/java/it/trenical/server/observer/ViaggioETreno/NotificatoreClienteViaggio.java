package it.trenical.server.observer.ViaggioETreno;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.Viaggio;

public class NotificatoreClienteViaggio implements ObserverViaggio
{
    private final Cliente cliente;

    public NotificatoreClienteViaggio(Cliente cliente)
    {
        this.cliente = cliente;
    }

    @Override
    public void aggiorna(SoggettoViaggio viaggioOsservato)
    {
        Viaggio viaggio = (Viaggio) viaggioOsservato;

        System.out.println(viaggio.getNotificaViaggio());
    }
}

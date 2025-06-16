package it.trenical.server.observer.ViaggioETreno;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.Viaggio;
import it.trenical.server.domain.gestore.GestoreNotifiche;
import it.trenical.server.dto.NotificaDTO;

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
        NotificaDTO notifica = viaggio.getNotificaViaggio();

        GestoreNotifiche.getInstance().inviaNotifica(cliente.getId(), notifica);
    }
}

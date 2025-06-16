package it.trenical.server.observer.ViaggioETreno;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.Viaggio;
import it.trenical.server.domain.gestore.GestoreNotifiche;
import it.trenical.server.dto.NotificaDTO;

public class NotificatoreClienteTreno implements ObserverViaggio
{
    private final Cliente cliente;

    public NotificatoreClienteTreno(Cliente cliente)
    {
        this.cliente = cliente;
    }

    @Override
    public void aggiorna(SoggettoViaggio viaggioDelTreno)
    {
        Viaggio viaggio = (Viaggio) viaggioDelTreno;
        NotificaDTO notifica = viaggio.getNotificaTreno();

        GestoreNotifiche.getInstance().inviaNotifica(cliente.getId(), notifica);
    }
}

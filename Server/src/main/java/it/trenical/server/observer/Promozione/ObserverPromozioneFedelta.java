package it.trenical.server.observer.Promozione;

import it.trenical.server.domain.PromozioneFedelta;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.gestore.GestoreNotifiche;
import it.trenical.server.dto.NotificaDTO;

public class ObserverPromozioneFedelta implements ObserverPromozione
{
    private final Cliente cliente;

    public ObserverPromozioneFedelta(Cliente cliente) {
        this.cliente = cliente;
    }

    public void aggiorna(SoggettoPromozione soggettoPromozione)
    {
        PromozioneFedelta promozioneFedelta = (PromozioneFedelta) soggettoPromozione;
        NotificaDTO notifica = promozioneFedelta.getNotifica();

        GestoreNotifiche.getInstance().inviaNotificaPromozionale(cliente.getId(), notifica);
    }
}

package it.trenical.server.observer.Promozione;

import it.trenical.server.domain.PromozioneFedelta;
import it.trenical.server.domain.cliente.Cliente;

public class ObserverPromozioneFedelta implements ObserverPromozione
{
    private final Cliente cliente;

    public ObserverPromozioneFedelta(Cliente cliente) {
        this.cliente = cliente;
    }

    public void aggiorna(SoggettoPromozione soggettoPromozione)
    {
        PromozioneFedelta promozioneFedelta = (PromozioneFedelta) soggettoPromozione;
        System.out.println("Notifica: "+ promozioneFedelta.getNotifica());
    }
}

package it.trenical.server.observer.notifiche;

import it.trenical.server.domain.Cliente;
import it.trenical.server.domain.Treno;
import it.trenical.server.domain.Viaggio;
import it.trenical.server.observer.ObserverViaggio;
import it.trenical.server.observer.SoggettoViaggio;

import java.text.SimpleDateFormat;

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
        Treno treno = viaggio.getTreno();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        //lo uso per rendere più leggibile l'orario

        System.out.println(
                "Aggiornamento Treno "+ treno.getID()+
                "\nstato: "+viaggio.getStato()+
                ", orario di Partenza: "+ sdf.format(viaggio.getInizioReale().getTime())+
                ", orario di Arrivo: "+ sdf.format(viaggio.getFineReale().getTime())
                );
    }
}

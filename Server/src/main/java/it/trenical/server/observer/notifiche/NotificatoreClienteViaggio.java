package it.trenical.server.observer.notifiche;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.Viaggio;
import it.trenical.server.observer.ObserverViaggio;
import it.trenical.server.observer.SoggettoViaggio;

import java.text.SimpleDateFormat;

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

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        //lo uso per rendere più leggibile l'orario

        System.out.println("Aggiornamento Viaggio "+ viaggio.getId()+
                "\nstato: "+viaggio.getStato()+
                ", orario di Partenza: "+ sdf.format(viaggio.getInizioReale().getTime())+
                ", orario di Arrivo: "+ sdf.format(viaggio.getFineReale().getTime())+
                ", binario di Partenza: "+ viaggio.getBinario("partenza")+
                ", binario di Arrivo: "+ viaggio.getBinario("arrivo"));
    }
}

package it.trenical.server.grpc;

import it.trenical.server.command.biglietto.ComandoBiglietto;
import it.trenical.server.command.cliente.ComandoCliente;
import it.trenical.server.command.promozione.PromozioneCommand;
import it.trenical.server.command.viaggio.ComandoViaggio;
import it.trenical.server.domain.gestore.MotoreRicercaViaggi;

public class ControllerGRPC
{
    private static MotoreRicercaViaggi mrv;

    public static void eseguiComandoCliente(ComandoCliente cmd)
    {
        try
        {
            cmd.esegui();
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    public static void eseguiComandoViaggio(ComandoViaggio cmd)
    {
        try
        {
            cmd.esegui();
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    public static void eseguiComandoPromozione(PromozioneCommand cmd)
    {
        try
        {
            cmd.esegui();
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    public static void eseguiComandoBiglietto(ComandoBiglietto cmd)
    {
        try
        {
            cmd.esegui();
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
}


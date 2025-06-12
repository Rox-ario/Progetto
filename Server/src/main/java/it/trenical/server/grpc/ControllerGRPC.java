package it.trenical.server.grpc;

import it.trenical.server.command.cliente.ComandoCliente;
import it.trenical.server.command.viaggio.ComandoViaggio;

public class ControllerGRPC
{
    //private static MotoreRicercaViaggi mrv;

    public static void eseguiComandoCliente(ComandoCliente cmd) throws Exception
    {
        cmd.esegui();
    }

    public static void eseguiComandoViaggio(ComandoViaggio cmd) throws Exception
    {
        cmd.esegui();
    }
}


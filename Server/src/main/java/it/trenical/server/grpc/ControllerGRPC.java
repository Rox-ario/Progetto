package it.trenical.server.grpc;

import it.trenical.server.command.ComandoCliente;

public class ControllerGRPC
{
    //private static MotoreRicercaViaggi mrv;

    public static void eseguiComandoCliente(ComandoCliente cmd) throws Exception
    {
        cmd.esegui();
    }
}


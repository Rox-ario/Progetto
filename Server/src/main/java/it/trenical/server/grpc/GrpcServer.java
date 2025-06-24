package it.trenical.server.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GrpcServer
{
    private static final int PORT = 50051; // Porta standard per gRPC
    private Server server;

    //avvia il server Grpc sulla mka porta e gli aggiunge tutti i servizi
    public void start() throws IOException
    {
        server = ServerBuilder.forPort(PORT)
                .addService(new AuthServiceImpl())
                .addService(new ViaggioServiceImpl())
                .addService(new BigliettoServiceImpl())
                .build()
                .start();

        System.out.println("Server gRPC TreniCal avviato sulla porta " + PORT);

        //trovato su internet a quanto pare serve per lo shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Ricevuto segnale di shutdown");
            try
            {
                GrpcServer.this.stop();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace(System.err);
            }
            System.err.println("Server fermato");
        }));
    }


    public void stop() throws InterruptedException
    {
        if (server != null)
        {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public void blockUntilShutdown() throws InterruptedException
    {
        if (server != null)
        {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException
    {
        final GrpcServer server = new GrpcServer();

        server.start();

        System.out.println("\n===========================================");
        System.out.println("SERVER TRENICAL ATTIVO");
        System.out.println("===========================================");
        System.out.println("Porta: " + PORT);
        System.out.println("Stato: In ascolto per connessioni client");
        System.out.println("\nPremi CTRL+C per fermare il server");
        System.out.println("===========================================\n");

        server.blockUntilShutdown();
    }
}

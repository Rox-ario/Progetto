package it.trenical.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import it.trenical.client.singleton.SessioneCliente;
import it.trenical.grpc.*;

import java.util.concurrent.TimeUnit;


//Uso il pattern Singleton per garantire una sola connessione.

public class GrpcClient
{
    private static GrpcClient instance;
    private final ManagedChannel channel;

    //gli stub per i vari servizi ci vogliono
    private final AuthServiceGrpc.AuthServiceBlockingStub authStub;
    private final ViaggioServiceGrpc.ViaggioServiceBlockingStub viaggioStub;
    private final BigliettoServiceGrpc.BigliettoServiceBlockingStub bigliettoStub;

    private static final String HOST = "localhost";
    private static final int PORT = 50051;

    private GrpcClient()
    {
        //qua si crea il canale di comunicazione
        this.channel = ManagedChannelBuilder
                .forAddress(HOST, PORT)
                .usePlaintext() //ho trovato che questa comunicazione è non criptata quindi non sicura
                .build();

        this.authStub = AuthServiceGrpc.newBlockingStub(channel);
        this.viaggioStub = ViaggioServiceGrpc.newBlockingStub(channel);
        this.bigliettoStub = BigliettoServiceGrpc.newBlockingStub(channel);

        System.out.println("Connessione stabilita con server TreniCal su " + HOST + ":" + PORT);
    }

    public static synchronized GrpcClient getInstance()
    {
        if (instance == null) {
            instance = new GrpcClient();
        }
        return instance;
    }


    public AuthServiceGrpc.AuthServiceBlockingStub getAuthStub()
    {
        return authStub;
    }

    public ViaggioServiceGrpc.ViaggioServiceBlockingStub getViaggioStub()
    {
        return viaggioStub;
    }

    public BigliettoServiceGrpc.BigliettoServiceBlockingStub getBigliettoStub()
    {
        return bigliettoStub;
    }

    //chiuso la connessione col server
    public void shutdown() throws InterruptedException
    {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("Connessione chiusa");
    }

    public boolean isServerReachable()
    {
        try
        {
            LogoutRequest request = LogoutRequest.newBuilder()
                    .setClienteId(SessioneCliente.getInstance().getIdClienteLoggato())
                    .build();

            authStub.logout(request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

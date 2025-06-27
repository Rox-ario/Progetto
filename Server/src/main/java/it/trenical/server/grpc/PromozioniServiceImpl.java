package it.trenical.server.grpc;

import io.grpc.stub.StreamObserver;
import it.trenical.grpc.*;

import java.util.List;

//qua faccio invece il service proto per le Promozioni

public class PromozioniServiceImpl extends PromozioniServiceGrpc.PromozioniServiceImplBase
{
    private final ControllerGRPC controllerGRPC = ControllerGRPC.getInstance();

    @Override
    public void getPromozioniAttive(GetPromozioniRequest request,
                                    StreamObserver<GetPromozioniResponse> responseObserver)
    {
        System.out.println("Richiesta promozioni attive per cliente: " + request.getClienteId());

        try
        {
            List<String> promozioni = controllerGRPC.getPromozioniAttive(request.getClienteId());

            GetPromozioniResponse.Builder responseBuilder = GetPromozioniResponse.newBuilder();

            for (String descrizione : promozioni)
            {
                PromozioneDettagliata promo = PromozioneDettagliata.newBuilder()
                        .setDescrizione(descrizione)
                        .build();
                responseBuilder.addPromozioni(promo);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            System.out.println("Inviate " + promozioni.size() + " promozioni");

        }
        catch (Exception e)
        {
            System.err.println("Errore getPromozioniAttive: " + e.getMessage());

            GetPromozioniResponse response = GetPromozioniResponse.newBuilder().build(); //svuoto tutto
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}

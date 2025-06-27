package it.trenical.server.grpc;

import io.grpc.stub.StreamObserver;
import it.trenical.grpc.*;
import it.trenical.server.command.cliente.RecuperaNotificheCommand;
import it.trenical.server.dto.NotificaDTO;
import it.trenical.server.dto.NotificheClienteDTO;

import java.util.List;

//implementazone delle notifiche del proto

public class NotificheServiceImpl extends NotificheServiceGrpc.NotificheServiceImplBase
{

    private final ControllerGRPC controllerGRPC = ControllerGRPC.getInstance();

    @Override
    public void getNotifiche(GetNotificheRequest request,
                             StreamObserver<GetNotificheResponse> responseObserver)
    {
        System.out.println("Richiesta notifiche per cliente: " + request.getClienteId() +
                " (parametro 'solo non lette' settato a : " + request.getSoloNonLette() + ")");

        try
        {
            List<NotificaDTO> risultato = controllerGRPC.getNotifiche(request.getClienteId(), request.getSoloNonLette());

            GetNotificheResponse.Builder responseBuilder = GetNotificheResponse.newBuilder();

            for (NotificaDTO notifica : risultato)
            {
                NotificaDettagliata notificaProto = NotificaDettagliata.newBuilder()
                        .setMessaggio(notifica.getMessaggio())
                        .setTimestamp(notifica.getTimestamp().getTimeInMillis())
                        .build();
                responseBuilder.addNotifiche(notificaProto);
            }

            responseBuilder.setTotaleNonLette(risultato.size());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            System.out.println("Inviate " + risultato.size() + " notifiche");

        }
        catch (Exception e)
        {
            System.err.println("Errore getNotifiche: " + e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void contaNotificheNonLette(ContaNotificheRequest request,
                                       StreamObserver<ContaNotificheResponse> responseObserver)
    {
        System.out.println("Richiesta conteggio notifiche per: " + request.getClienteId());

        try
        {
            NotificheClienteDTO risultato = controllerGRPC.getNotificheClienteDTO(request.getClienteId());

            ContaNotificheResponse response = ContaNotificheResponse.newBuilder()
                    .setNonLette(risultato.getNumeroNotifiche())
                    .setTotali(risultato.getNumeroNotifiche())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        }
        catch (Exception e)
        {
            ContaNotificheResponse response = ContaNotificheResponse.newBuilder()
                    .setNonLette(0)
                    .setTotali(0)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}

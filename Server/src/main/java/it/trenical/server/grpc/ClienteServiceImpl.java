package it.trenical.server.grpc;

import io.grpc.stub.StreamObserver;
import it.trenical.grpc.*;
import it.trenical.server.command.cliente.*;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.dto.ModificaClienteDTO;

//qui implemento il service ClienteService nel proto

public class ClienteServiceImpl extends ClienteServiceGrpc.ClienteServiceImplBase
{
    private final ControllerGRPC controllerGRPC = ControllerGRPC.getInstance();

    @Override
    public void getProfilo(GetProfiloRequest request,
                           StreamObserver<GetProfiloResponse> responseObserver)
    {
        System.out.println("Richiesta profilo per cliente: " + request.getClienteId());

        try
        {
            ClienteDTO clienteDTO = controllerGRPC.getProfiloCliente(request.getClienteId());

            //conversione in proootobuff
            ClienteCompleto clienteCompleto = ClienteCompleto.newBuilder()
                    .setId(clienteDTO.getId())
                    .setNome(clienteDTO.getNome())
                    .setCognome(clienteDTO.getCognome())
                    .setEmail(clienteDTO.getEmail())
                    .setPassword(clienteDTO.getPassword())
                    .setIsFedelta(clienteDTO.isFedelta())
                    .setRiceviNotifiche(clienteDTO.isRiceviNotifiche())
                    .setRiceviPromozioni(clienteDTO.isRiceviPromozioni())
                    .build();

            GetProfiloResponse response = GetProfiloResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Profilo recuperato con successo")
                    .setCliente(clienteCompleto)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        }
        catch (Exception e)
        {
            GetProfiloResponse response = GetProfiloResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Errore: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void modificaProfilo(ModificaProfiloRequest request,
                                StreamObserver<ModificaProfiloResponse> responseObserver)
    {
        System.out.println("Richiesta modifica profilo per: " + request.getClienteId());

        try
        {
            ClienteDTO clienteAttuale = controllerGRPC.getProfiloCliente(request.getClienteId());

            ModificaClienteDTO dto = new ModificaClienteDTO();
            dto.setId(request.getClienteId());
            dto.setNome(request.getNome());
            dto.setCognome(request.getCognome());
            dto.setPassword(request.getPassword());
            dto.setFedelta(clienteAttuale.isFedelta());

            controllerGRPC.modificaProfiloCliente(dto);

            ModificaProfiloResponse response = ModificaProfiloResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Profilo modificato con successo")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        }
        catch (Exception e)
        {
            ModificaProfiloResponse response = ModificaProfiloResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Errore: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void aderisciAFedelta(AderisciAFedeltaRequest request,
                                 StreamObserver<AderisciAFedeltaResponse> responseObserver)
    {
        System.out.println("Richiesta adesione fedeltà per: " + request.getClienteId());

        try
        {
            controllerGRPC.aderisciAFedelta(request.getClienteId(), request.getAttivaNotifichePromozioni());

            AderisciAFedeltaResponse response = AderisciAFedeltaResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Adesione a servizio Fedeltà completata")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        }
        catch (Exception e)
        {
            AderisciAFedeltaResponse response = AderisciAFedeltaResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Errore: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void rimuoviFedelta(RimuoviFedeltaRequest request,
                               StreamObserver<RimuoviFedeltaResponse> responseObserver)
    {
        System.out.println("Richiesta rimozione fedeltà per: " + request.getClienteId());

        try
        {
            controllerGRPC.rimuoviFedelta(request.getClienteId());

            RimuoviFedeltaResponse response = RimuoviFedeltaResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Rimosso dal programma Fedeltà")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        }
        catch (Exception e)
        {
            RimuoviFedeltaResponse response = RimuoviFedeltaResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Errore: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}

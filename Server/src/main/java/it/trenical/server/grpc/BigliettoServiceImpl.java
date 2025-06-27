package it.trenical.server.grpc;

import io.grpc.stub.StreamObserver;
import it.trenical.grpc.*;
import it.trenical.server.command.biglietto.*;
import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.gestore.GestoreBiglietti;
import it.trenical.server.dto.BigliettoDTO;
import it.trenical.server.dto.ModificaBigliettoDTO;

import java.util.List;

//qui metto i metodi principali per il viglietto
public class BigliettoServiceImpl extends BigliettoServiceGrpc.BigliettoServiceImplBase
{
    private final ControllerGRPC controllerGRPC = ControllerGRPC.getInstance();

    @Override
    public void acquistaBiglietto(AcquistaBigliettoRequest request,
                                  StreamObserver<AcquistaBigliettoResponse> responseObserver)
    {

        System.out.println("Richiesta acquisto biglietto per viaggio: " + request.getViaggioId());

        try
        {
            ClasseServizio classe = ClasseServizio.valueOf(request.getClasseServizio().toUpperCase());

           String idBiglietto = controllerGRPC.acquistaBiglietto(
                    request.getViaggioId(),
                    request.getClienteId(),
                    classe
            );

            AcquistaBigliettoResponse response = AcquistaBigliettoResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Biglietto acquistato con successo")
                    .setBigliettoId(idBiglietto)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (Exception e)
        {
            AcquistaBigliettoResponse response = AcquistaBigliettoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Errore acquisto: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.err.println("Acquisto fallito: " + e.getMessage());
        }
    }

    @Override
    public void getBigliettiCliente(GetBigliettiRequest request,
                                    StreamObserver<GetBigliettiResponse> responseObserver)
    {

        System.out.println("Richiesta biglietti per cliente: " + request.getClienteId());

        try
        {
            List<Biglietto> biglietti = controllerGRPC.getBigliettiCliente(request.getClienteId());
            GetBigliettiResponse.Builder responseBuilder = GetBigliettiResponse.newBuilder();

            for (Biglietto biglietto : biglietti)
            {
                BigliettoInfo info = convertiBigliettoToProto(biglietto);

                responseBuilder.addBiglietti(info);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            System.out.println("Inviati " + biglietti.size() + " biglietti");

        }
        catch (Exception e)
        {
            GetBigliettiResponse response = GetBigliettiResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.out.println("Nessun biglietto trovato per: " + request.getClienteId());
        }
    }

    @Override
    public void modificaBiglietto(ModificaBigliettoRequest request,
                                  StreamObserver<ModificaBigliettoResponse> responseObserver)
    {

        System.out.println("Richiesta modifica biglietto: " + request.getBigliettoId());

        try
        {
            ClasseServizio nuovaClasse = ClasseServizio.valueOf(request.getNuovaClasse().toUpperCase());

            controllerGRPC.modificaBiglietto(request.getBigliettoId(), nuovaClasse);

            ModificaBigliettoResponse response = ModificaBigliettoResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Biglietto modificato con successo")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.out.println("Biglietto modificato: " + request.getBigliettoId());

        }
        catch (Exception e)
        {
            ModificaBigliettoResponse response = ModificaBigliettoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Errore modifica: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.err.println("Modifica fallita: " + e.getMessage());
        }
    }

    @Override
    public void cancellaBiglietto(CancellaBigliettoRequest request,
                                  StreamObserver<CancellaBigliettoResponse> responseObserver)
    {

        System.out.println("Richiesta cancellazione biglietto: " + request.getBigliettoId());

        try
        {
            controllerGRPC.cancellaBiglietto(request.getBigliettoId());

            CancellaBigliettoResponse response = CancellaBigliettoResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Biglietto cancellato con successo")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.out.println("Biglietto cancellato: " + request.getBigliettoId());

        } catch (Exception e) {
            CancellaBigliettoResponse response = CancellaBigliettoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Errore cancellazione: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.err.println("Cancellazione fallita: " + e.getMessage());
        }
    }

    @Override
    public void getBiglietto(GetBigliettoRequest request,
                             StreamObserver<GetBigliettoResponse> responseObserver)
    {
        System.out.println("Richiesta biglietto: " + request.getBigliettoId());

        try
        {
            BigliettoDTO biglietto = controllerGRPC.getBiglietto(request.getBigliettoId());

            if (!request.getClienteId().isEmpty() &&
                    !biglietto.getIDCliente().equals(request.getClienteId())) {
                throw new IllegalAccessException("Biglietto non autorizzato per questo cliente");
            }

            BigliettoInfo bigliettoInfo = BigliettoInfo.newBuilder()
                    .setId(biglietto.getID())
                    .setViaggioId(biglietto.getIDViaggio())
                    .setClasseServizio(biglietto.getClasseServizio().toString())
                    .setPrezzo(biglietto.getPrezzo())
                    .setStato(biglietto.getStatoBiglietto().toString())
                    .setDataAcquisto(biglietto.getDataAcquisto().getTimeInMillis())
                    .build();

            GetBigliettoResponse response = GetBigliettoResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Biglietto trovato")
                    .setBiglietto(bigliettoInfo)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        }
        catch (Exception e)
        {
            GetBigliettoResponse response = GetBigliettoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Errore: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getStoricoBiglietti(GetStoricoBigliettiRequest request,
                                    StreamObserver<GetStoricoBigliettiResponse> responseObserver)
    {
        System.out.println("Richiesta storico biglietti per: " + request.getClienteId());

        try
        {
            GetBigliettiRequest bigliettiRequest = GetBigliettiRequest.newBuilder()
                    .setClienteId(request.getClienteId())
                    .build();

            //creo uno stream observer temporaneo per catturare la response
            StreamObserver<GetBigliettiResponse> tempObserver = new StreamObserver<GetBigliettiResponse>()
            {
                @Override
                //serve per inviare la risposta al client (completa o incompleya che sia)
                public void onNext(GetBigliettiResponse value)
                {
                    //convertiamo la risposta
                    GetStoricoBigliettiResponse.Builder responseBuilder =
                            GetStoricoBigliettiResponse.newBuilder();

                    for (BigliettoInfo biglietto : value.getBigliettiList())
                    {
                        if (!request.getStatoFiltro().isEmpty() &&
                                !biglietto.getStato().equals(request.getStatoFiltro())) {
                            continue;
                        }

                        if (request.getDataDa() > 0 &&
                                biglietto.getDataAcquisto() < request.getDataDa()) {
                            continue;
                        }

                        if (request.getDataA() > 0 &&
                                biglietto.getDataAcquisto() > request.getDataA()) {
                            continue;
                        }

                        responseBuilder.addBiglietti(biglietto);
                    }

                    responseBuilder.setTotale(responseBuilder.getBigliettiCount());
                    responseObserver.onNext(responseBuilder.build());
                }

                @Override
                public void onError(Throwable t)
                {
                    responseObserver.onError(t);
                }

                @Override
                public void onCompleted()
                {
                    responseObserver.onCompleted();
                }
            };

            getBigliettiCliente(bigliettiRequest, tempObserver);

        } catch (Exception e) {
            System.err.println("Errore storico biglietti: " + e.getMessage());
            responseObserver.onError(e);
        }
    }

    private BigliettoInfo convertiBigliettoToProto(Biglietto biglietto)
    {
        return BigliettoInfo.newBuilder()
                .setId(biglietto.getID())
                .setViaggioId(biglietto.getIDViaggio())
                .setClasseServizio(biglietto.getClasseServizio().toString())
                .setPrezzo(biglietto.getPrezzo())
                .setStato(biglietto.getStato().toString())
                .setDataAcquisto(biglietto.getDataAcquisto().getTimeInMillis())
                .build();
    }
}

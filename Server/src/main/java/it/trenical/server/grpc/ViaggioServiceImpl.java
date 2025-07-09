package it.trenical.server.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import it.trenical.grpc.*;
import it.trenical.server.domain.*;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.TipoTreno;
import it.trenical.server.domain.gestore.GestoreViaggi;
import it.trenical.server.domain.gestore.MotoreRicercaViaggi;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/*
  Implementazione del servizio viaggi gRPC.
  Gestisce la ricerca dei viaggi e i dettagli dei singoli viaggi.
 */
public class ViaggioServiceImpl extends ViaggioServiceGrpc.ViaggioServiceImplBase
{
    private final ControllerGRPC controllerGRPC = ControllerGRPC.getInstance();

    @Override
    public void cercaViaggi(CercaViaggiRequest request,
                            StreamObserver<CercaViaggiResponse> responseObserver)
    {
        System.out.println("Ricerca viaggi: " + request.getCittaPartenza() +
                " -> " + request.getCittaArrivo());

        try
        {
            Calendar dataViaggio = Calendar.getInstance();
            dataViaggio.setTime(new Date(request.getDataViaggio()));

            ClasseServizio classe = ClasseServizio.valueOf(request.getClasseServizio().toUpperCase());
            TipoTreno tipoTreno = TipoTreno.valueOf(request.getTipoTreno().toUpperCase());

            //desumo il filtro per la ricerca
            FiltroPasseggeri filtro = new FiltroPasseggeri(
                    request.getNumeroPasseggeri(),
                    classe,
                    tipoTreno,
                    dataViaggio,
                    null,  //per ora solo andata
                    true,
                    request.getCittaPartenza(),
                    request.getCittaArrivo()
            );

            List<Viaggio> viaggiTrovati = controllerGRPC.cercaViaggio(filtro);

            CercaViaggiResponse.Builder responseBuilder = CercaViaggiResponse.newBuilder();

            for (Viaggio viaggio : viaggiTrovati)
            {
                ViaggioInfo viaggioInfo = convertiViaggioToProto(viaggio, classe);
                responseBuilder.addViaggi(viaggioInfo);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            System.out.println("Trovati " + viaggiTrovati.size() + " viaggi");

        } catch (Exception e)
        {
            System.err.println("Errore ricerca viaggi: " + e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void getDettagliViaggio(GetViaggioRequest request,
                                   StreamObserver<ViaggioInfo> responseObserver)
    {

        System.out.println("Richiesta dettagli viaggio: " + request.getViaggioId());

        try
        {
            Viaggio viaggio = controllerGRPC.getViaggio(request.getViaggioId());

            if (viaggio == null)
            {
                throw new IllegalArgumentException("Viaggio non trovato: " + request.getViaggioId());
            }

            //Assumiamo classe LowCost per i dettagli generici
            ViaggioInfo viaggioInfo = convertiViaggioGenericoToProto(viaggio);

            responseObserver.onNext(viaggioInfo);
            responseObserver.onCompleted();

            System.out.println("Dettagli inviati per viaggio: " + request.getViaggioId());

        } catch (Exception e)
        {
            System.err.println("Errore recupero viaggio: " + e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void seguiTreno(SeguiTrenoRequest request,
                           StreamObserver<SeguiTrenoResponse> responseObserver) {
        System.out.println("Richiesta seguiTreno da cliente " + request.getClienteId() +
                " per treno " + request.getTrenoId());

        try
        {
            controllerGRPC.seguiTreno(request.getClienteId(), request.getTrenoId());

            SeguiTrenoResponse response = SeguiTrenoResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Ti sei iscritto con successo al treno " + request.getTrenoId())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        }
        catch (Exception e)
        {
            SeguiTrenoResponse response = SeguiTrenoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void smettiDiSeguireTreno(SmettiDiSeguireTrenoRequest request,
                                     StreamObserver<SmettiDiSeguireTrenoResponse> responseObserver)
    {
        System.out.println("Richiesta smettiDiSeguireTreno da cliente " + request.getClienteId());

        try
        {
            controllerGRPC.smettiDiSeguireTreno(request.getClienteId(), request.getTrenoId());

            SmettiDiSeguireTrenoResponse response = SmettiDiSeguireTrenoResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Hai smesso di seguire il treno " + request.getTrenoId())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        }
        catch (Exception e)
        {
            SmettiDiSeguireTrenoResponse response = SmettiDiSeguireTrenoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getTreniSeguiti(GetTreniSeguitiRequest request,
                                StreamObserver<GetTreniSeguitiResponse> responseObserver)
    {
        System.out.println("Richiesta getTreniSeguiti per cliente " + request.getClienteId());

        try
        {
            List<String> treniInfo = controllerGRPC.getTreniSeguiti(request.getClienteId());

            GetTreniSeguitiResponse.Builder responseBuilder = GetTreniSeguitiResponse.newBuilder();

            for (String info : treniInfo)
            {
                String[] parti = info.split("\\|");
                if (parti.length == 2)
                {
                    responseBuilder.addTreniIds(parti[0]);
                    responseBuilder.addTreniTipi(it.trenical.grpc.TipoTreno.valueOf(parti[1]));
                }
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        }
        catch (Exception e)
        {
            System.err.println("Errore in getTreniSeguiti: " + e.getMessage());
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getTreniDisponibili(GetTreniDisponibiliRequest request,
                                    StreamObserver<GetTreniDisponibiliResponse> responseObserver)
    {
        try
        {
            List<Treno> treni = controllerGRPC.getTuttiITreni();

            GetTreniDisponibiliResponse.Builder responseBuilder = GetTreniDisponibiliResponse.newBuilder();

            for (Treno treno : treni)
            {
                TrenoInfo trenoInfo = TrenoInfo.newBuilder()
                        .setId(treno.getID())
                        .setTipo(it.trenical.grpc.TipoTreno.valueOf(treno.getTipo().name()))
                        .build();

                responseBuilder.addTreni(trenoInfo);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            System.out.println("Inviati " + treni.size() + " treni disponibili");

        }
        catch (Exception e)
        {
            System.err.println("Errore in getTreniDisponibili: " + e.getMessage());
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    private ViaggioInfo convertiViaggioGenericoToProto(Viaggio viaggio)
    {
        int postiDisponibili = viaggio.getPostiDisponibiliPerClasse(ClasseServizio.LOW_COST) +
                viaggio.getPostiDisponibiliPerClasse(ClasseServizio.ECONOMY) +
                viaggio.getPostiDisponibiliPerClasse(ClasseServizio.BUSINESS) +
                viaggio.getPostiDisponibiliPerClasse(ClasseServizio.FEDELTA);


        it.trenical.grpc.TipoTreno grpcTipo =
                it.trenical.grpc.TipoTreno.valueOf(
                viaggio.getTreno()
                        .getTipo()
                        .name()
        );

        return ViaggioInfo.newBuilder()
                .setId(viaggio.getId())
                .setCittaPartenza(viaggio.getTratta().getStazionePartenza().getCitta())
                .setCittaArrivo(viaggio.getTratta().getStazioneArrivo().getCitta())
                .setOrarioPartenza(viaggio.getInizioReale().getTimeInMillis())
                .setOrarioArrivo(viaggio.getFineReale().getTimeInMillis())
                .setStato(viaggio.getStato().toString())
                .setPostiDisponibili(postiDisponibili)
                .setTipoTreno(grpcTipo)
                .build();
    }

    private ViaggioInfo convertiViaggioToProto(Viaggio viaggio, ClasseServizio classeServizio)
    {
        int postiDisponibili = viaggio.getPostiDisponibiliPerClasse(classeServizio);

        it.trenical.grpc.TipoTreno grpcTipo =
                it.trenical.grpc.TipoTreno.valueOf(
                        viaggio.getTreno()
                                .getTipo()
                                .name()
                );
        return ViaggioInfo.newBuilder()
                .setId(viaggio.getId())
                .setCittaPartenza(viaggio.getTratta().getStazionePartenza().getCitta())
                .setCittaArrivo(viaggio.getTratta().getStazioneArrivo().getCitta())
                .setOrarioPartenza(viaggio.getInizioReale().getTimeInMillis())
                .setOrarioArrivo(viaggio.getFineReale().getTimeInMillis())
                .setStato(viaggio.getStato().toString())
                .setPostiDisponibili(postiDisponibili)
                .setTipoTreno(grpcTipo)
                .setKilometri(viaggio.getKilometri())
                .build();
    }
}

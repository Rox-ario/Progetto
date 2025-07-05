package it.trenical.server.grpc;

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

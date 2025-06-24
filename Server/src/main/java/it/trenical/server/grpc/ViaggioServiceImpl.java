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
    private final MotoreRicercaViaggi motoreRicerca = MotoreRicercaViaggi.getInstance();
    private final GestoreViaggi gestoreViaggi = GestoreViaggi.getInstance();

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

            List<Viaggio> viaggiTrovati = motoreRicerca.cercaViaggio(filtro);

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
            Viaggio viaggio = gestoreViaggi.getViaggio(request.getViaggioId());

            if (viaggio == null)
            {
                throw new IllegalArgumentException("Viaggio non trovato: " + request.getViaggioId());
            }

            //Assumiamo classe LowCost per i dettagli generici
            ViaggioInfo viaggioInfo = convertiViaggioToProto(viaggio, ClasseServizio.LOW_COST);

            responseObserver.onNext(viaggioInfo);
            responseObserver.onCompleted();

            System.out.println("Dettagli inviati per viaggio: " + request.getViaggioId());

        } catch (Exception e)
        {
            System.err.println("Errore recupero viaggio: " + e.getMessage());
            responseObserver.onError(e);
        }
    }

    private ViaggioInfo convertiViaggioToProto(Viaggio viaggio, ClasseServizio classe)
    {
        int postiDisponibili = viaggio.getPostiDisponibiliPerClasse(classe);

        return ViaggioInfo.newBuilder()
                .setId(viaggio.getId())
                .setCittaPartenza(viaggio.getTratta().getStazionePartenza().getCitta())
                .setCittaArrivo(viaggio.getTratta().getStazioneArrivo().getCitta())
                .setOrarioPartenza(viaggio.getInizioReale().getTimeInMillis())
                .setOrarioArrivo(viaggio.getFineReale().getTimeInMillis())
                .setStato(viaggio.getStato().toString())
                .setPostiDisponibili(postiDisponibili)
                .setTipoTreno(viaggio.getTreno().getTipo().toString())
                .build();
    }
}

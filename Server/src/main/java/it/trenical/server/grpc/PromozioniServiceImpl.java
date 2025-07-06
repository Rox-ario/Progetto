package it.trenical.server.grpc;

import io.grpc.stub.StreamObserver;
import it.trenical.grpc.*;
import it.trenical.server.domain.Promozione;
import it.trenical.server.domain.PromozioneTratta;
import it.trenical.server.domain.PromozioneTreno;

import java.util.Calendar;
import java.util.List;

//qua faccio invece il service proto per le Promozioni

public class PromozioniServiceImpl extends PromozioniServiceGrpc.PromozioniServiceImplBase
{
    private final ControllerGRPC controllerGRPC = ControllerGRPC.getInstance();

    @Override
    public void getPromozioniAttive(GetPromozioniRequest request,
                                    StreamObserver<GetPromozioniResponse> responseObserver) {
        System.out.println("Richiesta promozioni attive per cliente: " + request.getClienteId());

        try {
            // Ora restituisce List<Promozione> invece di List<String>
            List<Promozione> promozioni = controllerGRPC.getPromozioniAttive(request.getClienteId());

            GetPromozioniResponse.Builder responseBuilder = GetPromozioniResponse.newBuilder();

            for (Promozione promo : promozioni)
            {
                StringBuilder desc = new StringBuilder();
                desc.append("Sconto: ").append(String.format("%.0f%%", promo.getPercentualeSconto() * 100));

                if (promo.getTipo() == it.trenical.server.domain.enumerations.TipoPromozione.FEDELTA)
                {
                    desc.append(" (Applicabile - sei cliente fedeltà!)");
                }
                else if (promo.getTipo() == it.trenical.server.domain.enumerations.TipoPromozione.TRATTA)
                {
                    PromozioneTratta pt = (PromozioneTratta) promo;
                    desc.append(" su ").append(pt.getTratta().getStazionePartenza().getCitta())
                            .append(" -> ").append(pt.getTratta().getStazioneArrivo().getCitta());
                }
                else if (promo.getTipo() == it.trenical.server.domain.enumerations.TipoPromozione.TRENO)
                {
                    PromozioneTreno pt = (PromozioneTreno) promo;
                    desc.append(" per treni ").append(pt.getTipoTreno());
                }

                desc.append(" (fino al ")
                        .append(promo.getDataFine().get(Calendar.DAY_OF_MONTH))
                        .append("/")
                        .append(promo.getDataFine().get(Calendar.MONTH) + 1)
                        .append("/")
                        .append(promo.getDataFine().get(Calendar.YEAR))
                        .append(")");

                PromozioneDettagliata.Builder promoBuilder = PromozioneDettagliata.newBuilder()
                        .setId(promo.getID())
                        .setDescrizione(desc.toString())
                        .setPercentualeSconto(promo.getPercentualeSconto())
                        .setDataInizio(promo.getDataInizio().getTimeInMillis())
                        .setDataFine(promo.getDataFine().getTimeInMillis());

                switch (promo.getTipo())
                {
                    case FEDELTA:
                        promoBuilder.setTipo(TipoPromozione.FEDELTA);
                        break;
                    case TRATTA:
                        promoBuilder.setTipo(TipoPromozione.TRATTA);
                        PromozioneTratta pt = (PromozioneTratta) promo;
                        promoBuilder.setTrattaId(pt.getTratta().getId());
                        break;
                    case TRENO:
                        promoBuilder.setTipo(TipoPromozione.TRENO);
                        PromozioneTreno ptreno = (PromozioneTreno) promo;
                        promoBuilder.setTipoTreno(ptreno.getTipoTreno().name());
                        break;
                }

                responseBuilder.addPromozioni(promoBuilder.build());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            System.out.println("Inviate " + promozioni.size() + " promozioni complete con tutti i dati");

        }
        catch (Exception e)
        {
            System.err.println("Errore getPromozioniAttive: " + e.getMessage());
            e.printStackTrace();
            GetPromozioniResponse response = GetPromozioniResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}

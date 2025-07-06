package it.trenical.client.grpc;

import io.grpc.StatusRuntimeException;
import it.trenical.grpc.*;
import it.trenical.server.domain.FiltroPasseggeri;
import it.trenical.server.domain.Treno;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoBiglietto;
import it.trenical.server.domain.enumerations.TipoTreno;
import it.trenical.server.dto.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

//E' un proxy di tipo remoto che serve per nascondere la complessità delle chiamate grpc al client

public class ServerProxy
{

    private final GrpcClient grpcClient;
    private static ServerProxy instance;

    private ServerProxy()
    {
        this.grpcClient = GrpcClient.getInstance();
    }

    public static synchronized ServerProxy getInstance()
    {
        if (instance == null)
        {
            instance = new ServerProxy();
        }
        return instance;
    }


    public static void registraCliente(ClienteDTO dto, String numeroCarta) throws Exception
    {
        try
        {
            RegistraRequest request = RegistraRequest.newBuilder()
                    .setNome(dto.getNome())
                    .setCognome(dto.getCognome())
                    .setEmail(dto.getEmail())
                    .setPassword(dto.getPassword())
                    .setIsFedelta(dto.isFedelta())
                    .setRiceviNotifiche(dto.isRiceviNotifiche())
                    .setRiceviPromozioni(dto.isRiceviPromozioni())
                    .setNumeroCarta(numeroCarta)
                    .build();
            System.out.println("Richiesta: "+ request);

            RegistraResponse response = getInstance().grpcClient.getAuthStub().registra(request);

            if (!response.getSuccess())
            {
                throw new Exception(response.getMessage());
            }
        }
        catch (Exception e)
        {
            throw new Exception("Errore di comunicazione con il server: " + e.getMessage());
        }
    }

    public static ClienteDTO login(String email, String password) throws Exception
    {
        try
        {
            LoginRequest request = LoginRequest.newBuilder()
                    .setEmail(email)
                    .setPassword(password)
                    .build();

            LoginResponse response = getInstance().grpcClient.getAuthStub().login(request);

            if (!response.getSuccess())
            {
                throw new Exception(response.getMessage());
            }

            ClienteInfo info = response.getCliente();
            return new ClienteDTO(
                    info.getId(),
                    info.getNome(),
                    info.getCognome(),
                    info.getEmail(),
                    password,
                    info.getIsFedelta(),
                    info.getWantsNotificheViaggi(),
                    info.getWantsNotifichePromo()
            );
        }
        catch (Exception e)
        {
            throw new Exception("Errore di comunicazione con il server: " + e.getMessage());
        }
    }

    public static List<ViaggioDTO> cercaViaggio(FiltroPasseggeri filtro) throws Exception
    {
        try
        {
            CercaViaggiRequest request = CercaViaggiRequest.newBuilder()
                    .setCittaPartenza(filtro.getCittaDiAndata())
                    .setCittaArrivo(filtro.getCittaDiArrivo())
                    .setDataViaggio(filtro.getDataInizio().getTimeInMillis())
                    .setNumeroPasseggeri(filtro.getNumero())
                    .setClasseServizio(filtro.getClasseServizio().toString())
                    .setTipoTreno(filtro.getTipoTreno().name())
                    .build();

            CercaViaggiResponse response = getInstance().grpcClient.getViaggioStub().cercaViaggi(request);

            List<ViaggioDTO> risultati = new ArrayList<>();
            for (ViaggioInfo info : response.getViaggiList())
            {
                ViaggioDTO dto = convertiViaggioInfoToDTO(info);
                risultati.add(dto);
            }

            return risultati;
        }
        catch (Exception e)
        {
            throw new Exception("Errore nella ricerca viaggi: " + e.getMessage());
        }
    }

    public static String acquistaBiglietto(String idViaggio, String idCliente, ClasseServizio classe) throws Exception
    {
        try
        {
            AcquistaBigliettoRequest request = AcquistaBigliettoRequest.newBuilder()
                    .setViaggioId(idViaggio)
                    .setClienteId(idCliente)
                    .setClasseServizio(classe.toString())
                    .build();

            AcquistaBigliettoResponse response = getInstance().grpcClient.getBigliettoStub()
                    .acquistaBiglietto(request);

            if (!response.getSuccess())
            {
                throw new Exception(response.getMessage());
            }

            return response.getBigliettoId();
        }
        catch (Exception e)
        {
            throw new Exception("Errore nell'acquisto biglietto: " + e.getMessage());
        }
    }


    public static List<BigliettoDTO> getBigliettiCliente(String idCliente) throws Exception
    {
        try
        {
            GetBigliettiRequest request = GetBigliettiRequest.newBuilder()
                    .setClienteId(idCliente)
                    .build();

            GetBigliettiResponse response = getInstance().grpcClient.getBigliettoStub()
                    .getBigliettiCliente(request);

            List<BigliettoDTO> biglietti = new ArrayList<>();
            for (BigliettoInfo info : response.getBigliettiList())
            {
                BigliettoDTO dto = convertiBigliettoInfoToDTO(info);
                biglietti.add(dto);
            }

            return biglietti;
        }
        catch (StatusRuntimeException e)
        {
            throw new Exception("Errore nel recupero biglietti: " + e.getMessage());
        }
    }

    public static void modificaBiglietto(String idBiglietto, ClasseServizio nuovaClasse) throws Exception
    {
        try
        {
            ModificaBigliettoRequest request = ModificaBigliettoRequest.newBuilder()
                    .setBigliettoId(idBiglietto)
                    .setNuovaClasse(nuovaClasse.toString())
                    .build();

            ModificaBigliettoResponse response = getInstance().grpcClient.getBigliettoStub()
                    .modificaBiglietto(request);

            if (!response.getSuccess())
            {
                throw new Exception(response.getMessage());
            }
        }
        catch (StatusRuntimeException e)
        {
            throw new Exception("Errore nella modifica biglietto: " + e.getMessage());
        }
    }

    public static void cancellaBiglietto(String idBiglietto) throws Exception
    {
        try
        {
            CancellaBigliettoRequest request = CancellaBigliettoRequest.newBuilder()
                    .setBigliettoId(idBiglietto)
                    .build();

            CancellaBigliettoResponse response = getInstance().grpcClient.getBigliettoStub()
                    .cancellaBiglietto(request);

            if (!response.getSuccess())
            {
                throw new Exception(response.getMessage());
            }
        }
        catch (StatusRuntimeException e)
        {
            throw new Exception("Errore nella cancellazione biglietto: " + e.getMessage());
        }
    }


    public static BigliettoDTO getBiglietto(String idBiglietto, String idCliente) throws Exception
    {
        try
        {
            GetBigliettoRequest request = GetBigliettoRequest.newBuilder()
                    .setBigliettoId(idBiglietto)
                    .setClienteId(idCliente)
                    .build();

            GetBigliettoResponse response = getInstance().grpcClient.getBigliettoStub().getBiglietto(request);

            if (!response.getSuccess())
            {
                throw new Exception(response.getMessage());
            }

            return convertiBigliettoInfoToDTO(response.getBiglietto());
        }
        catch (StatusRuntimeException e)
        {
            throw new Exception("Errore nel recuper del biglietto: " + e.getMessage());
        }
    }

    public static ClienteDTO getProfiloCliente(String idCliente) throws Exception
    {
        try
        {
            GetProfiloRequest request = GetProfiloRequest.newBuilder()
                    .setClienteId(idCliente)
                    .build();

            GetProfiloResponse response = getInstance().grpcClient.getClienteStub().getProfilo(request);

            if (!response.getSuccess())
            {
                throw new Exception(response.getMessage());
            }

            return convertiClienteInfoToDTO(response.getCliente());
        }
        catch (StatusRuntimeException e)
        {
            throw new Exception("Errore nel recuper del biglietto: " + e.getMessage());
        }
    }

    public static void modificaProfiloCliente(ModificaClienteDTO dto) throws Exception
    {
        try
        {
            ModificaProfiloRequest request = ModificaProfiloRequest.newBuilder()
                    .setClienteId(dto.getId())
                    .setNome(dto.getNome())
                    .setCognome(dto.getCognome())
                    .setPassword(dto.getPassword())
                    .build();

            ModificaProfiloResponse response = getInstance().grpcClient.getClienteStub().modificaProfilo(request);

            if (!response.getSuccess())
            {
                throw new Exception(response.getMessage());
            }
        }
        catch (StatusRuntimeException e)
        {
            throw new Exception("Errore nella modifica biglietto: " + e.getMessage());
        }
    }

    public static void aderisciAFedelta(String idCliente, boolean attivaNotifichePromozioni) throws Exception
    {
        try
        {
            AderisciAFedeltaRequest request = AderisciAFedeltaRequest.newBuilder()
                    .setClienteId(idCliente)
                    .setAttivaNotifichePromozioni(attivaNotifichePromozioni)
                    .build();

            AderisciAFedeltaResponse response = getInstance().grpcClient.getClienteStub().aderisciAFedelta(request);

            if (!response.getSuccess())
            {
                throw new Exception(response.getMessage());
            }
        }
        catch (StatusRuntimeException e)
        {
            throw new Exception("Errore nella modifica biglietto: " + e.getMessage());
        }
    }

    public static void rimuoviFedelta(String idCliente) throws Exception
    {
        try
        {
            RimuoviFedeltaRequest request = RimuoviFedeltaRequest.newBuilder()
                    .setClienteId(idCliente)
                    .build();

            RimuoviFedeltaResponse response = getInstance().grpcClient.getClienteStub().rimuoviFedelta(request);

            if (!response.getSuccess())
            {
                throw new Exception(response.getMessage());
            }
        }
        catch (StatusRuntimeException e)
        {
            throw new Exception("Errore nella modifica biglietto: " + e.getMessage());
        }
    }

    public static List<NotificaDTO> getNotifiche(String idCliente, boolean soloNonLette) throws Exception
    {
        try
        {
            GetNotificheRequest request = GetNotificheRequest.newBuilder()
                    .setClienteId(idCliente)
                    .build();

            GetNotificheResponse response = getInstance().grpcClient.getNotificheStub().getNotifiche(request);

            List<NotificaDTO> notifiche = new ArrayList<>();
            for (NotificaDettagliata info : response.getNotificheList())
            {
                NotificaDTO dto = convertiNotificaDettagliataToDTO(info);
                notifiche.add(dto);
            }

            return notifiche;
        }
        catch (StatusRuntimeException e)
        {
            throw new Exception("Errore nella modifica biglietto: " + e.getMessage());
        }
    }

    public static List<String> getPromozioniAttive(String idCliente) throws Exception
    {
        try
        {
            GetPromozioniRequest request = GetPromozioniRequest.newBuilder()
                    .setClienteId(idCliente)
                    .build();

            GetPromozioniResponse response = getInstance().grpcClient.getPromozioniStub().getPromozioniAttive(request);

            List<String> promozioni = new ArrayList<>();
            for (PromozioneDettagliata info : response.getPromozioniList())
            {
                Calendar dataInizioMillis = Calendar.getInstance();
                dataInizioMillis.setTimeInMillis(info.getDataInizio());
                Calendar dataFineMillis = Calendar.getInstance();
                dataFineMillis.setTimeInMillis(info.getDataFine());

                PromozioneDTO dto = convertiPromozioneDettagliataToDTO(info, dataInizioMillis, dataFineMillis);
                promozioni.add(dto.toString());
            }

            return promozioni;
        }
        catch (StatusRuntimeException e)
        {
            throw new Exception("Errore nella modifica biglietto: " + e.getMessage());
        }
    }

    private static ViaggioDTO convertiViaggioInfoToDTO(ViaggioInfo info)
    {
        it.trenical.server.domain.enumerations.TipoTreno tipoTreno =
                it.trenical.server.domain.enumerations.TipoTreno.valueOf(info.getTipoTreno().name());
        ViaggioDTO dto = new ViaggioDTO();
        dto.setID(info.getId());
        dto.setCittaPartenza(info.getCittaPartenza());
        dto.setCittaArrivo(info.getCittaArrivo());
        dto.setTipo(tipoTreno);
        dto.setStato(it.trenical.server.domain.enumerations.StatoViaggio.valueOf(info.getStato()));
        Calendar partenza = Calendar.getInstance();
        partenza.setTimeInMillis(info.getOrarioPartenza());
        dto.setInizio(partenza);
        dto.setKilometri(info.getKilometri());
        Calendar arrivo = Calendar.getInstance();
        arrivo.setTimeInMillis(info.getOrarioArrivo());
        dto.setFine(arrivo);

        dto.setPostiDisponibili(info.getPostiDisponibili());
        return dto;
    }

    private static BigliettoDTO convertiBigliettoInfoToDTO(BigliettoInfo info)
    {
        Calendar dataAcquisto = Calendar.getInstance();
        dataAcquisto.setTimeInMillis(info.getDataAcquisto());

        return new BigliettoDTO(
                info.getId(),
                info.getViaggioId(),
                ClasseServizio.valueOf(info.getClasseServizio()),
                "",
                dataAcquisto,
                StatoBiglietto.valueOf(info.getStato()),
                info.getPrezzo()
        );
    }

    private static ClienteDTO convertiClienteInfoToDTO(ClienteCompleto info)
    {
        return new ClienteDTO(
                info.getId(),
                info.getNome(),
                info.getCognome(),
                info.getEmail(),
                info.getPassword(),
                info.getIsFedelta(),
                info.getRiceviNotifiche(),
                info.getRiceviPromozioni()
        );
    }

    private static NotificaDTO convertiNotificaDettagliataToDTO(NotificaDettagliata info)
    {
        return new NotificaDTO(
                info.getMessaggio()
        );
    }

    private static PromozioneDTO convertiPromozioneDettagliataToDTO(PromozioneDettagliata info, Calendar dataInizio, Calendar dataFine)
    {
        //conviene desumerne il tipo
        it.trenical.server.domain.enumerations.TipoPromozione tipoConvertito =
                it.trenical.server.domain.enumerations.TipoPromozione.valueOf(info.getTipo().name());
        return new PromozioneDTO(
                info.getId(),
                info.getDescrizione(),
                info.getPercentualeSconto(),
                dataInizio,
                dataFine,
                tipoConvertito,
                info.getTrattaId(),
                info.getTipoTreno()
        );
    }
}

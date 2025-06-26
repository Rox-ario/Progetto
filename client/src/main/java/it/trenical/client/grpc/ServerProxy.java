package it.trenical.client.grpc;

import io.grpc.StatusRuntimeException;
import it.trenical.grpc.*;
import it.trenical.server.domain.FiltroPasseggeri;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoBiglietto;
import it.trenical.server.dto.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/*
    E' un proxy di tipo remoto che serve per nascondere la complessità delle chiamate grpc al client
    Lo uso come singleton
 */
public class ServerProxy
{

    private final GrpcClient grpcClient;
    private static ServerProxy instance;

    private ServerProxy() {
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


    public static void registraCliente(ClienteDTO dto) throws Exception
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
                    .build();

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

            //converto ClienteInfo (protobuf) in ClienteDTO
            ClienteInfo info = response.getCliente();
            return new ClienteDTO(
                    info.getId(),
                    info.getNome(),
                    info.getCognome(),
                    info.getEmail(),
                    password,
                    info.getIsFedelta(),
                    true,
                    false
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
                    .setTipoTreno(filtro.getTipoTreno() != null ? filtro.getTipoTreno().toString() : "COMFORT")
                    .build();

            CercaViaggiResponse response = getInstance().grpcClient.getViaggioStub().cercaViaggi(request);

            List<ViaggioDTO> risultati = new ArrayList<>();
            for (ViaggioInfo info : response.getViaggiosList())
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


    public static BigliettoDTO getBiglietto(String idBiglietto) throws Exception
    {
        throw new UnsupportedOperationException("Metodo non implementato nel servizio gRPC");
    }

    // ==================== METODI PROFILO CLIENTE ====================


    public static ClienteDTO getProfiloCliente(String idCliente) throws Exception
    {
        throw new UnsupportedOperationException("Metodo non implementato nel servizio gRPC");
    }

    /**
     * Modifica il profilo di un cliente
     */
    public static void modificaProfiloCliente(ModificaClienteDTO dto) throws Exception {
        // Workaround: dovrebbe esserci un endpoint specifico
        throw new UnsupportedOperationException("Metodo non implementato nel servizio gRPC");
    }

    /**
     * Aderisce al programma fedeltà
     */
    public static void aderisciAFedelta(String idCliente, boolean attivaNotifichePromozioni) throws Exception {
        // Workaround: dovrebbe esserci un endpoint specifico
        throw new UnsupportedOperationException("Metodo non implementato nel servizio gRPC");
    }

    /**
     * Rimuove l'adesione al programma fedeltà
     */
    public static void rimuoviFedelta(String idCliente) throws Exception {
        // Workaround: dovrebbe esserci un endpoint specifico
        throw new UnsupportedOperationException("Metodo non implementato nel servizio gRPC");
    }

    /**
     * Recupera le notifiche di un cliente
     */
    public static List<NotificaDTO> getNotifiche(String idCliente, boolean soloNonLette) throws Exception {
        // Workaround: dovrebbe esserci un endpoint specifico
        throw new UnsupportedOperationException("Metodo non implementato nel servizio gRPC");
    }

    /**
     * Recupera le promozioni attive
     */
    public static List<String> getPromozioniAttive(String idCliente) throws Exception {
        // Workaround: dovrebbe esserci un endpoint specifico
        throw new UnsupportedOperationException("Metodo non implementato nel servizio gRPC");
    }

    // ==================== METODI DI CONVERSIONE ====================

    private static ViaggioDTO convertiViaggioInfoToDTO(ViaggioInfo info)
    {
        ViaggioDTO dto = new ViaggioDTO();
        dto.setID(info.getId());
        dto.setCittaPartenza(info.getCittaPartenza());
        dto.setCittaArrivo(info.getCittaArrivo());

        Calendar partenza = Calendar.getInstance();
        partenza.setTimeInMillis(info.getOrarioPartenza());
        dto.setInizio(partenza);

        Calendar arrivo = Calendar.getInstance();
        arrivo.setTimeInMillis(info.getOrarioArrivo());
        dto.setFine(arrivo);

        dto.setPostiDisponibili(info.getPostiDisponibili());

        // Nota: alcuni campi come Treno e Tratta non sono disponibili nel proto
        // quindi non possiamo settarli nel DTO

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
                "", // ID cliente non è nel proto
                dataAcquisto,
                StatoBiglietto.valueOf(info.getStato()),
                info.getPrezzo()
        );
    }
}

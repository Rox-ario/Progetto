package it.trenical.server.grpc;

import it.trenical.server.command.biglietto.*;
import it.trenical.server.command.cliente.*;
import it.trenical.server.command.promozione.PromozioneCommand;
import it.trenical.server.command.viaggio.ComandoViaggio;
import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.FiltroPasseggeri;
import it.trenical.server.domain.Viaggio;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoBiglietto;
import it.trenical.server.domain.gestore.GestoreBiglietti;
import it.trenical.server.domain.gestore.GestoreViaggi;
import it.trenical.server.domain.gestore.MotoreRicercaViaggi;
import it.trenical.server.dto.*;
import it.trenical.server.utils.Assembler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class ControllerGRPC
{
    private static MotoreRicercaViaggi mrv;

    private static void eseguiComandoCliente(ComandoCliente cmd)
    {
        try
        {
            cmd.esegui();
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static void eseguiComandoViaggio(ComandoViaggio cmd)
    {
        try
        {
            cmd.esegui();
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static void eseguiComandoPromozione(PromozioneCommand cmd)
    {
        try
        {
            cmd.esegui();
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static void eseguiComandoBiglietto(ComandoBiglietto cmd)
    {
        try
        {
            cmd.esegui();
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    public static void registraCliente(ClienteDTO clienteDTO) throws Exception
    {
        try
        {
            RegistraClienteCommand command = new RegistraClienteCommand(clienteDTO, clienteDTO.getPassword());
            eseguiComandoCliente(command);

            System.out.println("Cliente registrato con successo: " + clienteDTO.getEmail());

        } catch (Exception e)
        {
            System.err.println("Errore durante la registrazione: " + e.getMessage());
            throw new Exception("Registrazione fallita: " + e.getMessage());
        }
    }

    public static ClienteDTO login(String email, String password) throws Exception
    {
        try
        {
            LoginClienteCommand command = new LoginClienteCommand(email, password);
            eseguiComandoCliente(command);

            //il command ha eseguito l'autenticazione, ora recupero il DTO
            ClienteDTO clienteDTO = command.getClienteDTO();

            System.out.println("Login effettuato con successo per: " + clienteDTO.getEmail());
            return clienteDTO;
        }
        catch (Exception e)
        {
            System.err.println("Errore durante il login: " + e.getMessage());
            throw new Exception("Login fallito: " + e.getMessage());
        }
    }

    public static ClienteDTO getProfiloCliente(String idCliente) throws Exception
    {
        try
        {
            GetProfiloClienteCommand command = new GetProfiloClienteCommand(idCliente);
            eseguiComandoCliente(command);

            ClienteDTO clienteDTO = command.getProfilo();

            System.out.println("Profilo recuperato per cliente: " + clienteDTO.getEmail());
            return clienteDTO;

        }
        catch (Exception e)
        {
            System.err.println("Errore nel recupero profilo: " + e.getMessage());
            throw new Exception("Impossibile recuperare il profilo: " + e.getMessage());
        }
    }

    public static void modificaProfiloCliente(ModificaClienteDTO modificaDTO) throws Exception
    {
        try
        {
            ModificaClienteCommand command = new ModificaClienteCommand(modificaDTO.getId(), modificaDTO);
            eseguiComandoCliente(command);

            System.out.println("Profilo modificato con successo per cliente: " + modificaDTO.getId());

        }
        catch (Exception e)
        {
            System.err.println("Errore nella modifica profilo: " + e.getMessage());
            throw new Exception("Modifica profilo fallita: " + e.getMessage());
        }
    }

    public static void aderisciAFedelta(String idCliente, boolean attivaNotifichePromozioni) throws Exception
    {
        try
        {
            AderisciAFedeltaCommand command = new AderisciAFedeltaCommand(idCliente, attivaNotifichePromozioni);
            eseguiComandoCliente(command);

            System.out.println("Cliente " + idCliente + " iscritto al programma fedeltà");

        } catch (Exception e)
        {
            System.err.println("Errore nell'adesione a fedeltà: " + e.getMessage());
            throw new Exception("Adesione fedeltà fallita: " + e.getMessage());
        }
    }


    public static void rimuoviFedelta(String idCliente) throws Exception
    {
        try
        {
            RimuoviFedeltaCommand command = new RimuoviFedeltaCommand(idCliente);
            eseguiComandoCliente(command);

            System.out.println("Cliente " + idCliente + " rimosso dal programma fedeltà");

        } catch (Exception e)
        {
            System.err.println("Errore nella rimozione fedeltà: " + e.getMessage());
            throw new Exception("Rimozione fedeltà fallita: " + e.getMessage());
        }
    }

    public static List<NotificaDTO> getNotifiche(String idCliente, boolean soloNonLette) throws Exception
    {
        try
        {
            RecuperaNotificheCommand command = new RecuperaNotificheCommand(idCliente, soloNonLette);
            eseguiComandoCliente(command);

            NotificheClienteDTO risultato = command.getRisultato();

            System.out.println("Recuperate " + risultato.getNumeroNotifiche() +
                    " notifiche per cliente: " + idCliente);

            return risultato.getNotifiche();

        } catch (Exception e) {
            System.err.println("Errore nel recupero notifiche: " + e.getMessage());
            throw new Exception("Impossibile recuperare le notifiche: " + e.getMessage());
        }
    }

    public static List<ViaggioDTO> cercaViaggio(FiltroPasseggeri filtro) throws Exception
    {
        try
        {
            if (filtro == null)
            {
                throw new IllegalArgumentException("Filtro di ricerca non può essere nullo");
            }

            if (mrv == null)
            {
                mrv = MotoreRicercaViaggi.getInstance();
            }

            System.out.println("Ricerca viaggi da " + filtro.getCittaDiAndata() +
                    " a " + filtro.getCittaDiArrivo() +
                    " per " + filtro.getNumero() + " passeggeri");

            List<Viaggio> viaggiTrovati = mrv.cercaViaggio(filtro);

            //converto in DTO per il client
            List<ViaggioDTO> viaggiDTO = new ArrayList<>();
            for(Viaggio v : viaggiTrovati)
            {
                viaggiDTO.add(Assembler.viaggioToDTO(v));
            }

            System.out.println("Ricerca completata: trovati " + viaggiDTO.size() + " viaggi");

            if (viaggiDTO.isEmpty())
            {
                System.out.println("Suggerimento: prova a modificare le date o le preferenze di viaggio");
            }

            return viaggiDTO;

        }
        catch (Exception e)
        {
            System.err.println("Errore nella ricerca viaggi: " + e.getMessage());
            throw new Exception("Ricerca viaggi fallita: " + e.getMessage());
        }
    }

    public static String getStatoSistema()
    {
        try
        {
            StringBuilder stato = new StringBuilder();
            stato.append("SISTEMA TRENICAL - STATO OPERATIVO\n");
            stato.append("=======================================\n");

            //info sui gestori
            GestoreBiglietti gb = GestoreBiglietti.getInstance();
            stato.append("Biglietti attivi: ").append(gb.getBigliettiAttivi().size()).append("\n");

            stato.append("Sistema operativo e pronto per le richieste\n");

            return stato.toString();

        }
        catch (Exception e)
        {
            return "Errore nel recupero stato sistema: " + e.getMessage();
        }
    }


    public static void acquistaBiglietto(String idViaggio, String idCliente, ClasseServizio classeServizio) throws Exception
    {
        try
        {
            if (idViaggio == null || idViaggio.trim().isEmpty())
            {
                throw new IllegalArgumentException("ID viaggio non può essere vuoto");
            }
            if (idCliente == null || idCliente.trim().isEmpty())
            {
                throw new IllegalArgumentException("ID cliente non può essere vuoto");
            }
            if (classeServizio == null)
            {
                throw new IllegalArgumentException("Classe di servizio non può essere nulla");
            }

            System.out.println("Inizio acquisto biglietto per viaggio: " + idViaggio);
            System.out.println("Cliente: " + idCliente + " | Classe: " + classeServizio);

            AssegnaBiglietto commandAssegna = new AssegnaBiglietto(idViaggio, idCliente, classeServizio);
            eseguiComandoBiglietto(commandAssegna);

            Biglietto bigliettoCreato = commandAssegna.getBiglietto();

            if (bigliettoCreato == null)
            {
                throw new RuntimeException("Errore interno: nessun biglietto restituito dal command di assegnazione");
            }

            String idBiglietto = bigliettoCreato.getID();

            try
            {
                PagaBiglietto commandPaga = new PagaBiglietto(idBiglietto);
                eseguiComandoBiglietto(commandPaga);

                System.out.println("ACQUISTO COMPLETATO CON SUCCESSO!");
                System.out.println("ID Biglietto: " + idBiglietto);
                System.out.println("Importo addebitato: €" + String.format("%.2f", bigliettoCreato.getPrezzo()));
                System.out.println("Classe: " + classeServizio);

            } catch (Exception pagamentoError)
            {
                //cancello il biglietto se il pagamento fallisce
                System.err.println("Pagamento fallito: " + pagamentoError.getMessage());
                System.out.println("Annullamento biglietto in corso...");

                try
                {
                    CancellaBiglietto commandCancella = new CancellaBiglietto(idBiglietto);
                    eseguiComandoBiglietto(commandCancella);
                    System.out.println("Rollback biglietto completato");
                }
                catch (Exception rollbackError)
                {
                    System.err.println("ERRORE CRITICO: Impossibile cancellare il biglietto dopo pagamento fallito!");
                    System.err.println("ID Biglietto rimasto orfano: " + idBiglietto);
                }

                throw new Exception("Acquisto fallito durante il pagamento: " + pagamentoError.getMessage());
            }

        }
        catch (Exception e)
        {
            System.err.println("Errore durante l'acquisto biglietto: " + e.getMessage());
            throw new Exception("Acquisto biglietto fallito: " + e.getMessage());
        }
    }


    public static void modificaBiglietto(String idBiglietto, ClasseServizio nuovaClasse) throws Exception
    {
        try
        {
            if (idBiglietto == null || idBiglietto.trim().isEmpty())
            {
                throw new IllegalArgumentException("ID biglietto non può essere vuoto");
            }
            if (nuovaClasse == null)
            {
                throw new IllegalArgumentException("Nuova classe di servizio non può essere nulla");
            }

            System.out.println("Modifica biglietto " + idBiglietto + " a classe " + nuovaClasse);

            ModificaBigliettoDTO dto = new ModificaBigliettoDTO(idBiglietto, nuovaClasse);
            ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);
            eseguiComandoBiglietto(command);

            System.out.println("Biglietto " + idBiglietto + " modificato con successo");

        }
        catch (Exception e)
        {
            System.err.println("Errore nella modifica biglietto: " + e.getMessage());
            throw new Exception("Modifica biglietto fallita: " + e.getMessage());
        }
    }

    public static void cancellaBiglietto(String idBiglietto) throws Exception
    {
        try
        {
            if (idBiglietto == null || idBiglietto.trim().isEmpty())
            {
                throw new IllegalArgumentException("ID biglietto non può essere vuoto");
            }

            System.out.println("Cancellazione biglietto " + idBiglietto);

            CancellaBiglietto command = new CancellaBiglietto(idBiglietto);
            eseguiComandoBiglietto(command);

            System.out.println("Biglietto " + idBiglietto + " cancellato con successo");

        } catch (Exception e)
        {
            System.err.println("Errore nella cancellazione biglietto: " + e.getMessage());
            throw new Exception("Cancellazione biglietto fallita: " + e.getMessage());
        }
    }

    public static List<BigliettoDTO> getBigliettiCliente(String idCliente) throws Exception
    {
        try {
            if (idCliente == null || idCliente.trim().isEmpty())
            {
                throw new IllegalArgumentException("ID cliente non può essere vuoto");
            }

            System.out.println("🔍 Recupero biglietti per cliente: " + idCliente);

            GestoreBiglietti gb = GestoreBiglietti.getInstance();
            List<Biglietto> biglietti = gb.getBigliettiUtente(idCliente);

            List<BigliettoDTO> bigliettiDTO = biglietti.stream()
                    .map(Assembler::bigliettoToDTO)
                    .collect(Collectors.toList());

            System.out.println("Recuperati " + bigliettiDTO.size() + " biglietti per cliente: " + idCliente);
            return bigliettiDTO;

        } catch (Exception e)
        {
            System.err.println("Errore nel recupero biglietti: " + e.getMessage());
            throw new Exception("Impossibile recuperare i biglietti: " + e.getMessage());
        }
    }


    public static BigliettoDTO getBiglietto(String idBiglietto) throws Exception
    {
        try
        {
            if (idBiglietto == null || idBiglietto.trim().isEmpty())
            {
                throw new IllegalArgumentException("ID biglietto non può essere vuoto");
            }

            GestoreBiglietti gb = GestoreBiglietti.getInstance();
            Biglietto biglietto = gb.getBigliettoPerID(idBiglietto);

            if (biglietto == null)
            {
                throw new IllegalArgumentException("Biglietto non trovato: " + idBiglietto);
            }

            System.out.println("Biglietto recuperato: " + idBiglietto);
            return Assembler.bigliettoToDTO(biglietto);

        }
        catch (Exception e)
        {
            System.err.println("Errore nel recupero biglietto: " + e.getMessage());
            throw new Exception("Impossibile recuperare il biglietto: " + e.getMessage());
        }
    }

    private String formatCalendar(Calendar cal)
    {
        if (cal == null) return "N/A";

        return cal.get(Calendar.DAY_OF_MONTH)+"/"+cal.get(Calendar.MONTH)+"/"+cal.get(Calendar.YEAR)
                +" "+cal.get(Calendar.HOUR)+":"+cal.get(Calendar.MINUTE);
    }


    public static List<BigliettoDTO> getBigliettiPerStato(String idCliente, String stato) throws Exception
    {
        try
        {
            List<BigliettoDTO> tuttiBiglietti = getBigliettiCliente(idCliente);

            List<BigliettoDTO> filtrati = new ArrayList<>();
            for(BigliettoDTO bigliettoDTO : tuttiBiglietti)
            {
                if (bigliettoDTO.getStatoBiglietto() == StatoBiglietto.valueOf(stato))
                    filtrati.add(bigliettoDTO);
            }

            System.out.println("Filtrati " + filtrati.size() + " biglietti con stato: " + stato);
            return filtrati;

        }
        catch (Exception e)
        {
            System.err.println("Errore nel filtraggio biglietti: " + e.getMessage());
            throw new Exception("Impossibile filtrare i biglietti: " + e.getMessage());
        }
    }
}


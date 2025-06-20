package it.trenical.server.grpc;

import it.trenical.server.command.biglietto.ComandoBiglietto;
import it.trenical.server.command.cliente.*;
import it.trenical.server.command.promozione.PromozioneCommand;
import it.trenical.server.command.viaggio.ComandoViaggio;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.gestore.MotoreRicercaViaggi;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.dto.ModificaClienteDTO;
import it.trenical.server.dto.NotificaDTO;
import it.trenical.server.dto.NotificheClienteDTO;

import java.util.List;

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


}


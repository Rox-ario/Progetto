package it.trenical.client.domain;

import io.grpc.Server;
import it.trenical.client.grpc.ServerProxy;
import it.trenical.client.singleton.SessioneCliente;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.dto.ModificaClienteDTO;
import it.trenical.server.dto.NotificaDTO;
import it.trenical.server.grpc.ControllerGRPC;

import java.util.ArrayList;
import java.util.List;

/*
 Controller per la gestione del profilo utente.
 Permette di visualizzare e modificare i dati del profilo, gestire notifiche e promozioni.
 */
public class ProfiloController
{
    public ClienteDTO visualizzaProfilo()
    {
        try
        {
            if (!Loggato())
            {
                return null;
            }

            String idCliente = SessioneCliente.getInstance().getIdClienteLoggato();
            ClienteDTO profilo = ServerProxy.getProfiloCliente(idCliente);

            mostraDettagliProfilo(profilo);
            return profilo;

        }
        catch (Exception e)
        {
            System.err.println("Errore nel recupero profilo: " + e.getMessage());
            return null;
        }
    }


    public boolean modificaProfilo(ModificaClienteDTO dto)
    {
        try
        {
            if (!Loggato())
            {
                return false;
            }

            if (dto == null)
            {
                System.err.println("Dati di modifica non validi");
                return false;
            }

            String idCliente = SessioneCliente.getInstance().getIdClienteLoggato();
            dto.setId(idCliente); //il set vede se l'id è quello del cliente loggato

            //validazioni base
            if (!validaDatiModifica(dto))
            {
                return false;
            }

            System.out.println("Modifica profilo in corso...");

            ServerProxy.modificaProfiloCliente(dto);

            //aggiorna la sessione locale con i nuovi dati
            aggiornaSessioneLocale(dto);

            System.out.println("Profilo modificato con successo!");
            return true;

        }
        catch (Exception e)
        {
            System.err.println("Errore nella modifica profilo: " + e.getMessage());
            return false;
        }
    }

    public List<String> visualizzaPromozioni()
    {
        try
        {
            if (!Loggato())
            {
                return new ArrayList<>();
            }

            String idCliente = SessioneCliente.getInstance().getIdClienteLoggato();
            List<String> promozioni = ServerProxy.getPromozioniAttive(idCliente);

            if (promozioni.isEmpty())
            {
                System.out.println("Non ci sono promozioni attive al momento");
                System.out.println("Torna più tardi per vedere nuove offerte!");
            }
            else
            {
                mostraPromozioni(promozioni);
            }

            return promozioni;

        }
        catch (Exception e)
        {
            System.err.println("Errore nel recupero promozioni: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<NotificaDTO> getNotifiche()
    {
        return getNotifiche(true); // Solo non lette per default
    }


    public List<NotificaDTO> getNotifiche(boolean soloNonLette)
    {
        try
        {
            if (!Loggato())
            {
                return new ArrayList<>();
            }

            String idCliente = SessioneCliente.getInstance().getIdClienteLoggato();
            List<NotificaDTO> notifiche = ServerProxy.getNotifiche(idCliente, soloNonLette);

            if (notifiche.isEmpty())
            {
                if(soloNonLette)
                {
                    String tipo = "nuove notifiche";
                    System.out.println("Non hai " + tipo + " al momento");
                }
                else
                {
                    String tipo =  "notifiche";
                    System.out.println("Non hai " + tipo + " al momento");
                }
            }
            else
            {
                mostraNotifiche(notifiche, soloNonLette);
            }

            return notifiche;

        }
        catch (Exception e)
        {
            System.err.println("Errore nel recupero notifiche: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void visualizzaStoricoNotifiche()
    {
        System.out.println("\nNOTIFICHE");
        System.out.println("--------------------------------");
        getNotifiche(false); //prendo tutte le notifiche
    }


    private boolean Loggato()
    {
        if (!SessioneCliente.getInstance().isLoggato())
        {
            System.err.println("Devi effettuare il login per accedere al profilo!");
            System.out.println("Usa il menu 'Accedi' per autenticarti");
            return false;
        }
        return true;
    }


    private boolean validaDatiModifica(ModificaClienteDTO dto)
    {
        if (dto.getNome() == null || dto.getNome().trim().isEmpty())
        {
            System.err.println("Il nome non può essere vuoto");
            return false;
        }

        if (dto.getCognome() == null || dto.getCognome().trim().isEmpty())
        {
            System.err.println("Il cognome non può essere vuoto");
            return false;
        }

        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty())
        {
            System.err.println("La password non può essere vuota");
            return false;
        }

        if (dto.getPassword().length() < 6)
        {
            System.err.println("La password deve essere di almeno 6 caratteri");
            return false;
        }

        return true;
    }

    private void aggiornaSessioneLocale(ModificaClienteDTO dto)
    {
        ClienteDTO clienteCorrente = SessioneCliente.getInstance().getClienteCorrente();

        //aggiorno i dati nella sessione nel caso in cui cambi il nome, cognome, etc
        clienteCorrente.setNome(dto.getNome());
        clienteCorrente.setCognome(dto.getCognome());
        clienteCorrente.setPassword(dto.getPassword());
        clienteCorrente.setFedelta(dto.isFedelta());

        System.out.println("Sessione locale aggiornata");
    }

    public boolean aggiornaPreferenze(boolean riceviNotifiche, boolean riceviPromozioni)
    {
        try
        {
            if (!Loggato())
            {
                return false;
            }

            String idCliente = SessioneCliente.getInstance().getIdClienteLoggato();
            ClienteDTO clienteCorrente = SessioneCliente.getInstance().getClienteCorrente();

            ModificaClienteDTO dto = new ModificaClienteDTO(
                    idCliente,
                    clienteCorrente.getNome(),
                    clienteCorrente.getCognome(),
                    clienteCorrente.getPassword(),
                    clienteCorrente.isFedelta(),
                    riceviNotifiche
            );

            ServerProxy.modificaProfiloCliente(dto);

            clienteCorrente.setRiceviNotifiche(riceviNotifiche);
            clienteCorrente.setRiceviPromozioni(riceviPromozioni);

            System.out.println("Preferenze aggiornate con successo!");
            return true;

        }
        catch (Exception e)
        {
            System.err.println("Errore nell'aggiornamento preferenze: " + e.getMessage());
            return false;
        }
    }


    private void mostraDettagliProfilo(ClienteDTO profilo)
    {
        if (profilo == null)
        {
            System.err.println("Profilo non disponibile");
            return;
        }

        System.out.println("\nIL TUO PROFILO");
        System.out.println("-----------------------------------");
        System.out.println("ID: " + profilo.getId());
        System.out.println("Nome: " + profilo.getNome());
        System.out.println("Cognome: " + profilo.getCognome());
        System.out.println("Email: " + profilo.getEmail());

        if (profilo.isFedelta())
        {
            System.out.println("Status: Cliente FedeltàTreno");
            System.out.println("Vantaggi: Sconti esclusivi e promozioni speciali");
            if (profilo.isRiceviPromozioni())
            {
                System.out.println("Notifiche promozioni: Attive");
            }
            else
            {
                System.out.println("Notifiche promozioni: Disattivate");
            }
        }
        else
        {
            System.out.println("Status: Cliente Standard");
            System.out.println("Suggerimento: Aderisci a FedeltàTreno per sconti esclusivi!");
        }

        if(profilo.isRiceviNotifiche())
        {
            System.out.println("Notifiche Attive");
        }
        else
            System.out.println("Notifiche Disattivate");
        System.out.println("--------------------------------------------------------------\n");
    }

    private void mostraPromozioni(List<String> promozioni)
    {
        System.out.println("\nPROMOZIONI ATTIVE");
        System.out.println("----------------------------------------------");

        for (int i = 0; i < promozioni.size(); i++)
        {
            System.out.println((i + 1) + ". " + promozioni.get(i));
        }

        System.out.println("------------------------------------------------");
        System.out.println("Le promozioni vengono applicate automaticamente all'acquisto!");

        ClienteDTO cliente = SessioneCliente.getInstance().getClienteCorrente();
        if (!cliente.isFedelta())
        {
            System.out.println("Aderisci a FedeltàTreno per accedere a promozioni esclusive!");
        }

        System.out.println();
    }


    private void mostraNotifiche(List<NotificaDTO> notifiche, boolean soloNonLette)
    {
        String titolo = null;
        if(soloNonLette)
        {
            titolo = "NOTIFICHE NON LETTE";
        }
        else
            titolo = "TUTTE NOTIFICHE";
        System.out.println("\n" + titolo);
        System.out.println("-------------------------------");

        for (int i = 0; i < notifiche.size()-1; i++)
        {
            NotificaDTO notifica = notifiche.get(i);
            System.out.println(notifica.getTempoStampato());

            //spezzo il messaggio in righe per migliore leggibilità
            String[] righe = notifica.getMessaggio().split("\n");
            for (String riga : righe)
            {
                System.out.println("   " + riga);
            }
            if (i < notifiche.size() - 1) {
                System.out.println("==============================");
            }
        }
        System.out.println("-------------------------------");
    }

    public int contaNotificheNonLette()
    {
        try
        {
            if (!Loggato())
            {
                return 0;
            }

            List<NotificaDTO> nonLette = getNotifiche(true);
            return nonLette.size();

        }
        catch (Exception e)
        {
            System.err.println("Errore nel conteggio notifiche: " + e.getMessage());
            return 0;
        }
    }


    public boolean hasPromozioniDisponibili()
    {
        try
        {
            List<String> promozioni = visualizzaPromozioni();
            return !promozioni.isEmpty();
        }
        catch (Exception e)
        {
            return false;
        }
    }


    public void mostraRiepilogoRapido()
    {
        try
        {
            if (!Loggato())
            {
                return;
            }

            ClienteDTO cliente = SessioneCliente.getInstance().getClienteCorrente();
            int notifiche = contaNotificheNonLette();
            boolean hasPromo = hasPromozioniDisponibili();

            System.out.println("Ciao, " + cliente.getNome() + "!");

            if (cliente.isFedelta())
            {
                System.out.println("Status FedeltàTreno attivo");
            }

            if (notifiche > 0)
            {
                System.out.println("HAi " + notifiche + " notifiche non lette");
            }

            if (hasPromo)
            {
                System.out.println("Promozioni attive disponibili!");
            }

        }
        catch (Exception e)
        {
            System.err.println("Errore nel riepilogo: " + e.getMessage());
        }
    }
}
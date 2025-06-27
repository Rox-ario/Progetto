package it.trenical.server.grpc;

import it.trenical.server.command.biglietto.*;
import it.trenical.server.command.cliente.*;
import it.trenical.server.command.promozione.CreaPromozioneCommand;
import it.trenical.server.command.promozione.PromozioneCommand;
import it.trenical.server.command.viaggio.AggiornaRitardoViaggio;
import it.trenical.server.command.viaggio.AggiornaViaggio;
import it.trenical.server.command.viaggio.ComandoViaggio;
import it.trenical.server.command.viaggio.ProgrammaViaggio;
import it.trenical.server.domain.*;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.*;
import it.trenical.server.domain.gestore.*;
import it.trenical.server.dto.*;
import it.trenical.server.utils.Assembler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

//facade + singleton
public class ControllerGRPC
{
    private static ControllerGRPC instance;
    private MotoreRicercaViaggi mrv;

    private ControllerGRPC(){this.mrv = MotoreRicercaViaggi.getInstance();}

    public static synchronized ControllerGRPC getInstance() {
        if (instance == null)
        {
            instance = new ControllerGRPC();
        }
        return instance;
    }

    private void eseguiComandoCliente(ComandoCliente cmd)
    {
        try
        {
            cmd.esegui();
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private void eseguiComandoViaggio(ComandoViaggio cmd)
    {
        try
        {
            cmd.esegui();
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private void eseguiComandoPromozione(PromozioneCommand cmd)
    {
        try
        {
            cmd.esegui();
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private void eseguiComandoBiglietto(ComandoBiglietto cmd)
    {
        try
        {
            cmd.esegui();
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    public void registraCliente(ClienteDTO clienteDTO) throws Exception
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

    public ClienteDTO login(String email, String password) throws Exception
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

    public ClienteDTO getProfiloCliente(String idCliente) throws Exception
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

    public void modificaProfiloCliente(ModificaClienteDTO modificaDTO) throws Exception
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

    public void aderisciAFedelta(String idCliente, boolean attivaNotifichePromozioni) throws Exception
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


    public void rimuoviFedelta(String idCliente) throws Exception
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

    public List<NotificaDTO> getNotifiche(String idCliente, boolean soloNonLette) throws Exception
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

    public List<Viaggio> cercaViaggio(FiltroPasseggeri filtro) throws Exception
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

            System.out.println("Ricerca completata: trovati " + viaggiTrovati.size() + " viaggi");

            if (viaggiTrovati.isEmpty())
            {
                System.out.println("Suggerimento: prova a modificare le date o le preferenze di viaggio");
            }

            return viaggiTrovati;

        }
        catch (Exception e)
        {
            System.err.println("Errore nella ricerca viaggi: " + e.getMessage());
            throw new Exception("Ricerca viaggi fallita: " + e.getMessage());
        }
    }


    public String acquistaBiglietto(String idViaggio, String idCliente, ClasseServizio classeServizio) throws Exception
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

            return idBiglietto;

        }
        catch (Exception e)
        {
            System.err.println("Errore durante l'acquisto biglietto: " + e.getMessage());
            throw new Exception("Acquisto biglietto fallito: " + e.getMessage());
        }
    }


    public void modificaBiglietto(String idBiglietto, ClasseServizio nuovaClasse) throws Exception
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

    public void cancellaBiglietto(String idBiglietto) throws Exception
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

    public List<Biglietto> getBigliettiCliente(String idCliente) throws Exception
    {
        try {
            if (idCliente == null || idCliente.trim().isEmpty())
            {
                throw new IllegalArgumentException("ID cliente non può essere vuoto");
            }

            System.out.println("🔍 Recupero biglietti per cliente: " + idCliente);

            GestoreBiglietti gb = GestoreBiglietti.getInstance();
            List<Biglietto> biglietti = gb.getBigliettiUtente(idCliente);

            System.out.println("Recuperati " + biglietti.size() + " biglietti per cliente: " + idCliente);
            return biglietti;

        } catch (Exception e)
        {
            System.err.println("Errore nel recupero biglietti: " + e.getMessage());
            throw new Exception("Impossibile recuperare i biglietti: " + e.getMessage());
        }
    }


    public BigliettoDTO getBiglietto(String idBiglietto) throws Exception
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

    public Viaggio getViaggio(String idViaggio)
    {
        if(idViaggio == null)
            throw new IllegalArgumentException("L'id del viaggio non può essere null");
        try
        {
           return GestoreViaggi.getInstance().getViaggio(idViaggio);
        }catch(Exception e)
        {
            System.err.println("Errore: "+ e.getMessage());
            return null;
        }
    }

    private String formatCalendar(Calendar cal)
    {
        if (cal == null) return "N/A";

        return cal.get(Calendar.DAY_OF_MONTH)+"/"+cal.get(Calendar.MONTH)+"/"+cal.get(Calendar.YEAR)
                +" "+cal.get(Calendar.HOUR)+":"+cal.get(Calendar.MINUTE);
    }


    public List<BigliettoDTO> getBigliettiPerStato(String idCliente, String stato) throws Exception
    {
        try
        {
            List<Biglietto> tuttiBiglietti = getBigliettiCliente(idCliente);

            List<BigliettoDTO> filtrati = new ArrayList<>();
            for(Biglietto biglietto : tuttiBiglietti)
            {
                if (biglietto.getStatoBiglietto() == StatoBiglietto.valueOf(stato))
                    filtrati.add(Assembler.bigliettoToDTO(biglietto));
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

    public List<String> getPromozioniAttive(String idCliente) throws Exception
    {
        try
        {
            if (idCliente == null || idCliente.trim().isEmpty())
            {
                throw new IllegalArgumentException("ID cliente non può essere vuoto");
            }

            GestoreClienti gc = GestoreClienti.getInstance();
            Cliente cliente = gc.getClienteById(idCliente);

            if (cliente == null)
            {
                throw new IllegalArgumentException("Cliente non trovato: " + idCliente);
            }

            CatalogoPromozione cp = CatalogoPromozione.getInstance();
            List<Promozione> promozioniAttive = cp.getPromozioniAttive();

            List<String> descrizioni = new ArrayList<>();

            for (Promozione promo : promozioniAttive)
            {
                StringBuilder desc = new StringBuilder();
                desc.append("Sconto: ").append(String.format("%.0f%%", promo.getPercentualeSconto() * 100));

                if (promo.getTipo().name().equals("FEDELTA"))
                {
                    if (cliente.haAdesioneFedelta())
                    {
                        desc.append(" (Applicabile - sei cliente fedeltà!)");
                    }
                    else
                    {
                        desc.append(" (Non applicabile - richiede fedeltà)");
                    }
                }
                else if (promo.getTipo().name().equals("TRATTA"))
                {
                    PromozioneTratta pt = (PromozioneTratta) promo;
                    desc.append(" su ").append(pt.getTratta().getStazionePartenza().getCitta())
                            .append(" -> ").append(pt.getTratta().getStazioneArrivo().getCitta());
                }
                else if (promo.getTipo().name().equals("TRENO"))
                {
                    PromozioneTreno pt = (PromozioneTreno) promo;
                    desc.append(" per treni ").append(pt.getTipoTreno());
                }

                desc.append(" (fino al ").append(formatCalendar(promo.getDataFine())).append(")");

                descrizioni.add(desc.toString());
            }

            System.out.println("Recuperate " + descrizioni.size() + " promozioni attive");
            return descrizioni;

        }
        catch (Exception e)
        {
            System.err.println("Errore nel recupero promozioni: " + e.getMessage());
            throw new Exception("Impossibile recuperare le promozioni: " + e.getMessage());
        }
    }


    public void aggiungiTreno(String id, TipoTreno tipo) throws Exception
    {
        GestoreViaggi gv = GestoreViaggi.getInstance();
        gv.aggiungiTreno(id, tipo);
        System.out.println("Treno aggiunto: " + id + " (" + tipo + ")");
    }

    public void aggiungiTratta(Tratta tratta) throws Exception
    {
        GestoreViaggi gv = GestoreViaggi.getInstance();
        gv.aggiungiTratta(tratta);
        System.out.println("Tratta aggiunta: " + tratta.getId());
    }

    public void creaPromozione(TipoPromozione tipo, Calendar dataInizio,
                               Calendar dataFine, double sconto,
                               Tratta tratta, Treno treno)
    {
        try
        {
            CreaPromozioneCommand command = new CreaPromozioneCommand(
                    tipo, dataInizio, dataFine, sconto, tratta, treno
            );
            eseguiComandoPromozione(command);

            System.out.println("Promozione creata: " + tipo);

        }
        catch (Exception e)
        {
            System.err.println("Errore: "+ e.getMessage());
        }
    }

    public List<Treno> getTuttiITreni()
    {
        GestoreViaggi gv = GestoreViaggi.getInstance();
        return gv.getTuttiITreni();
    }

    public Treno getTreno(String id)
    {
        return GestoreViaggi.getInstance().getTreno(id);
    }

    public Stazione getStazione(String id){return GestoreViaggi.getInstance().getStazione(id);}

    public List<Tratta> getTutteLeTratte()
    {
        return (List<Tratta>) GestoreViaggi.getInstance().getTratte();
    }

    public void aggiungiStazione(Stazione stazione)
    {
        GestoreViaggi gv = GestoreViaggi.getInstance();
        gv.aggiungiStazione(stazione);
    }

    public List<Stazione> getTutteLeStazioni()
    {
        return GestoreViaggi.getInstance().getTutteLeStazioni();
    }


    public Viaggio programmaViaggio(String idTreno, String idTratta, Calendar partenza, Calendar arrivo)
    {
        ProgrammaViaggio command = new ProgrammaViaggio(idTreno, idTratta, partenza, arrivo);
        eseguiComandoViaggio(command);
        return command.getViaggio();
    }

    public void aggiornaStatoViaggio(String idViaggio, StatoViaggio nuovoStato)
    {
        AggiornaViaggio command = new AggiornaViaggio(idViaggio, nuovoStato);
        eseguiComandoViaggio(command);
    }

    public void aggiornaRitardoViaggio(String id, int ritardo)
    {
        AggiornaRitardoViaggio commando = new AggiornaRitardoViaggio(id, ritardo);
        eseguiComandoViaggio(commando);
    }

    public List<Viaggio> getViaggiPerStato(StatoViaggio statoViaggio)
    {
       return GestoreViaggi.getInstance().getViaggiPerStato(statoViaggio);
    }

    public List<Viaggio> getViaggiPerData(Calendar dataDa, Calendar dataA)
    {
        return GestoreViaggi.getInstance().getViaggiPerData(dataDa, dataA);
    }

    public List<Promozione> getPromoPerTipo(TipoPromozione tipo)
    {
        return CatalogoPromozione.getInstance().getPromoPerTipo(tipo);
    }

    public List<Promozione> getPromo()
    {
        return CatalogoPromozione.getInstance().getTutteLePromozioni();
    }

    public Tratta getTratta(String id)
    {
        return GestoreViaggi.getInstance().getTratta(id);
    }

    public NotificheClienteDTO getNotificheClienteDTO(String id)
    {
        RecuperaNotificheCommand command = new RecuperaNotificheCommand(id, true);
        eseguiComandoCliente(command);
        return command.getRisultato();
    }
}


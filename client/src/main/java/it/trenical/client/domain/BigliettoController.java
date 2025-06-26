package it.trenical.client.domain;

import it.trenical.client.grpc.ServerProxy;
import it.trenical.client.singleton.SessioneCliente;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoBiglietto;
import it.trenical.server.dto.BigliettoDTO;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/*
 Controller per la gestione dei biglietti dell'utente.
 Permette di visualizzare, modificare e cancellare i biglietti esistenti.
 */
public class BigliettoController
{

    public List<BigliettoDTO> getMieiBiglietti()
    {
        try
        {
            if (!Loggato())
            {
                return new ArrayList<>();
            }

            String idCliente = SessioneCliente.getInstance().getIdClienteLoggato();
            List<BigliettoDTO> biglietti = ServerProxy.getBigliettiCliente(idCliente);

            if (biglietti.isEmpty())
            {
                System.out.println("Non hai ancora acquistato biglietti");
                System.out.println("Usa la ricerca viaggi per trovare e prenotare il tuo primo viaggio!");
            }
            else
            {
                System.out.println("Trovati " + biglietti.size() + " biglietti");
                mostraRiepilogoBiglietti(biglietti);
            }

            return biglietti;

        }
        catch (Exception e)
        {
            System.err.println("Errore nel recupero biglietti: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    public List<BigliettoDTO> getBigliettiAttivi()
    {
        try
        {
            List<BigliettoDTO> tuttiBiglietti = getMieiBiglietti();
            Calendar ora = Calendar.getInstance();

            List<BigliettoDTO> bigliettiAttivi = new ArrayList<>();
            for(BigliettoDTO b : tuttiBiglietti)
            {
                if(b.getStatoBiglietto() == StatoBiglietto.PAGATO && b.getDataAcquisto().before(ora))
                    bigliettiAttivi.add(b);
            }

            System.out.println("Biglietti attivi: " + bigliettiAttivi.size());
            return bigliettiAttivi;

        }
        catch (Exception e)
        {
            System.err.println("Errore nel recupero biglietti attivi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    //prende biglietto specifico in base a id
    public BigliettoDTO getBiglietto(String idBiglietto)
    {
        try
        {
            if (!Loggato())
            {
                return null;
            }

            if (idBiglietto == null || idBiglietto.trim().isEmpty())
            {
                System.err.println("ID biglietto non valido");
                return null;
            }

            BigliettoDTO biglietto = ServerProxy.getBiglietto(idBiglietto.trim());

            //verifico che il biglietto appartenga all'utente loggato
            String idClienteLoggato = SessioneCliente.getInstance().getIdClienteLoggato();
            if (!biglietto.getIDCliente().equals(idClienteLoggato))
            {
                System.err.println("Non hai i permessi per visualizzare questo biglietto");
                return null;
            }

            mostraDettagliBiglietto(biglietto);
            return biglietto;

        }
        catch (Exception e)
        {
            System.err.println("Errore nel recupero biglietto: " + e.getMessage());
            return null;
        }
    }


    public boolean modificaClasseBiglietto(String idBiglietto, ClasseServizio nuovaClasse)
    {
        try
        {
            if (!Loggato())
            {
                return false;
            }

            if (idBiglietto == null || idBiglietto.trim().isEmpty())
            {
                System.err.println("ID biglietto non valido");
                return false;
            }

            if (nuovaClasse == null)
            {
                System.err.println("Classe di servizio non valida");
                return false;
            }

            //verifico che il biglietto appartenga all'utente
            BigliettoDTO biglietto = getBiglietto(idBiglietto);
            if (biglietto == null)
            {
                return false;
            }

            //verifico che il biglietto sia modificabile
            if (!isBigliettoModificabile(biglietto))
            {
                return false;
            }

            if (biglietto.getClasseServizio() == nuovaClasse)
            {
                System.out.println("ℹIl biglietto ha già la classe " + nuovaClasse);
                return true;
            }

            System.out.println("Modifica in corso...");
            System.out.println("Da: " + biglietto.getClasseServizio() + " -> A: " + nuovaClasse);

            ServerProxy.modificaBiglietto(idBiglietto.trim(), nuovaClasse);

            System.out.println("Biglietto modificato con successo!");
            System.out.println("Controlla la tua email per eventuali addebiti o rimborsi");

            return true;

        }
        catch (Exception e)
        {
            System.err.println("Errore nella modifica del biglietto: " + e.getMessage());
            return false;
        }
    }

    public boolean cancellaBiglietto(String idBiglietto)
    {
        try
        {
            if (!Loggato())
            {
                return false;
            }

            if (idBiglietto == null || idBiglietto.trim().isEmpty())
            {
                System.err.println("ID biglietto non valido");
                return false;
            }

            //verifico che il biglietto appartenga all'utente
            BigliettoDTO biglietto = getBiglietto(idBiglietto);
            if (biglietto == null)
            {
                return false;
            }

            //verifico che il biglietto sia cancellabile
            if (!isBigliettoCancellabile(biglietto))
            {
                return false;
            }

            System.out.println("Cancellazione biglietto in corso...");

            ServerProxy.cancellaBiglietto(idBiglietto.trim());

            System.out.println("Biglietto cancellato con successo!");
            System.out.println("Eventuali rimborsi saranno accreditati sul tuo conto");

            return true;

        }
        catch (Exception e)
        {
            System.err.println("Errore nella cancellazione del biglietto: " + e.getMessage());
            return false;
        }
    }

    public void mostraDettagliBiglietto(BigliettoDTO biglietto)
    {
        if (biglietto == null)
        {
            System.err.println("Biglietto non valido");
            return;
        }

        System.out.println("\nDETTAGLI BIGLIETTO");
        System.out.println("-----------------------");
        System.out.println("ID: " + biglietto.getID());
        System.out.println("Viaggio: " + biglietto.getIDViaggio());
        System.out.println("Classe: " + biglietto.getClasseServizio());
        System.out.println("Prezzo: €" + String.format("%.2f", biglietto.getPrezzo()));
        System.out.println("Stato: " + getStatoBiglietto(biglietto.getStatoBiglietto()));
        System.out.println("Acquistato: " + formatCalendar(biglietto.getDataAcquisto()));

        // Indicazioni specifiche per stato
        switch (biglietto.getStatoBiglietto())
        {
            case NON_PAGATO:
                System.out.println("AZIONE RICHIESTA: Completa il pagamento per confermare il biglietto");
                break;
            case PAGATO:
                System.out.println("Biglietto valido e utilizzabile");
                break;
            case ANNULLATO:
                System.out.println("Biglietto annullato");
                break;
        }

        System.out.println("-----------------------");
    }


    private void mostraRiepilogoBiglietti(List<BigliettoDTO> biglietti)
    {
        System.out.println("\nI TUOI BIGLIETTI");
        System.out.println("--------------------------------------");

        // Raggruppa per stato
        int pagati = 0;
        int nonPagati = 0;
        int annullati = 0;

        for(BigliettoDTO bigliettoDTO : biglietti)
        {
            switch (bigliettoDTO.getStatoBiglietto().name())
            {
                case "PAGATO" : pagati+=1;
                break;
                case "ANNULLATO": annullati += 1;
                break;
                case "NON_PAGATO": nonPagati += 1;
                break;
            }
        }

        System.out.println("Biglietti validi: "+ pagati);
        if (nonPagati > 0)
        {
            System.out.println("In attesa di pagamento: "+ nonPagati);
        }
        if (annullati > 0)
        {
            System.out.println("Annullati: "+ annullati);
        }

        //mostro tutti i bigliretti
        System.out.println("\nBIGLIETTI:");
        StringBuilder sb = new StringBuilder();
        for(BigliettoDTO bigliettoDTO : biglietti)
        {
            sb.append(bigliettoDTO.toString());
            sb.append("\n");
        }

        System.out.println(sb.toString());
    }


    private boolean Loggato()
    {
        if (!SessioneCliente.getInstance().isLoggato())
        {
            System.err.println("Devi effettuare il login per gestire i biglietti!");
            System.out.println("Usa il menu 'Accedi' per autenticarti");
            return false;
        }
        return true;
    }


    private boolean isBigliettoModificabile(BigliettoDTO biglietto)
    {
        if (biglietto.getStatoBiglietto() != StatoBiglietto.PAGATO)
        {
            System.err.println("Solo i biglietti pagati possono essere modificati");
            if (biglietto.getStatoBiglietto() == StatoBiglietto.NON_PAGATO)
            {
                System.out.println("Completa prima il pagamento del biglietto");
            }
            return false;
        }

        return true;
    }


    private boolean isBigliettoCancellabile(BigliettoDTO biglietto)
    {
        if (biglietto.getStatoBiglietto() == StatoBiglietto.ANNULLATO)
        {
            System.err.println("Il biglietto è già stato annullato");
            return false;
        }
        return true;
    }

    private String getStatoBiglietto(StatoBiglietto stato)
    {
        switch (stato)
        {
            case PAGATO: return "Pagato";
            case NON_PAGATO: return "Da pagare";
            case ANNULLATO: return "Annullato";
            default: return "Non disponibile";
        }
    }

    private String formatCalendar(Calendar cal)
    {
        if (cal == null) return "N/A";

        return cal.get(Calendar.DAY_OF_MONTH)+"/"+cal.get(Calendar.MONTH)+"/"+cal.get(Calendar.YEAR)
                +" "+cal.get(Calendar.HOUR)+":"+cal.get(Calendar.MINUTE);
    }
}

package it.trenical.server.admin;

import it.trenical.server.domain.*;
import it.trenical.server.domain.enumerations.StatoViaggio;
import it.trenical.server.domain.enumerations.TipoPromozione;
import it.trenical.server.domain.enumerations.TipoTreno;
import it.trenical.server.domain.gestore.GestoreViaggi;
import it.trenical.server.grpc.ControllerGRPC;

import java.text.SimpleDateFormat;
import java.util.*;

public class AdminCLI
{
    private final ControllerGRPC controllerGRPC;
    private final Scanner scanner;
    private boolean running = true;

    //credenziali admin ( che in produzione andrebbero nel DB)
    private static final String ADMIN_USER = "Rosario";
    private static final String ADMIN_PASS = "trenical!";

    public AdminCLI()
    {
        this.controllerGRPC = ControllerGRPC.getInstance();
        this.scanner = new Scanner(System.in);
        this.scanner.useLocale(Locale.US); //i numeri decimali li leggiamo col .
    }

    public void avvia()
    {
        if (!effettuaLoginAdmin())
        {
            System.out.println("Accesso negato!");
            return;
        }

        mostraBenvenuto();

        while (running)
        {
            try
            {
                mostraMenuPrincipale();
                int scelta = leggiScelta();
                gestisciScelta(scelta);
            }
            catch (Exception e)
            {
                System.err.println("Errore: " + e.getMessage());
            }
        }

        System.out.println("\nArrivederci amministratore!");
    }

    private boolean effettuaLoginAdmin()
    {
        System.out.println("\nLOGIN AMMINISTRATORE");
        System.out.println("=======================");

        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        return ADMIN_USER.equals(username) && ADMIN_PASS.equals(password);
    }

    private void mostraBenvenuto()
    {
        System.out.println("\nSISTEMA AMMINISTRATIVO TRENICAL");
        System.out.println("=====================================");
        System.out.println("Benvenuto amministratore!");
        Calendar oggi = Calendar.getInstance();
        int mese = oggi.get(Calendar.MONTH) + 1;
        System.out.println("Data: " + oggi.get(Calendar.DAY_OF_MONTH) +"/"+mese+"/"+oggi.get(Calendar.YEAR));
        System.out.println("ore: "+ oggi.get(Calendar.HOUR_OF_DAY)+":"+oggi.get(Calendar.MINUTE));
        System.out.println("=====================================");
    }

    private void mostraMenuPrincipale() {
        System.out.println("\nMENU PRINCIPALE");
        System.out.println("========================");
        System.out.println("1.Gestione Treni");
        System.out.println("2.Gestione Tratte");
        System.out.println("3.Gestione Viaggi");
        System.out.println("4.Gestione Promozioni");
        System.out.println("5.Gestione Clienti");
        System.out.println("6.Gestione Stazioni");
        System.out.println("7.Utilità Database");
        System.out.println("0.Esci");
        System.out.print("\nScelta: ");
    }


    private void gestisciScelta(int scelta)
    {
        switch (scelta) {
            case 1: menuTreni(); break;
            case 2: menuTratte(); break;
            case 3: menuViaggi(); break;
            case 4: menuPromozioni(); break;
            case 5: menuClienti(); break;
            case 6 : menuStazioni(); break;
            case 7: menuDatabase(); break;
            case 0: running = false; break;
            default: System.out.println("Scelta non valida");
        }
    }

    private void menuTreni()
    {
        while (true)
        {
            System.out.println("\nGESTIONE TRENI");
            System.out.println("==================");
            System.out.println("1. Aggiungi nuovo treno");
            System.out.println("2. Visualizza tutti i treni");
            System.out.println("3. Cerca treno per ID");
            System.out.println("4. Elimina treno");
            System.out.println("0. Torna indietro");
            System.out.print("\nScelta: ");

            int scelta = leggiScelta();

            switch (scelta)
            {
                case 1: aggiungiTreno(); break;
                case 2: visualizzaTuttiITreni(); break;
                case 3: cercaTreno(); break;
                case 4: rimuoviTreno(); break;
                case 0: return;
                default: System.out.println("Scelta non valida");
            }
        }
    }

    private void aggiungiTreno()
    {
        System.out.println("\nAGGIUNGI NUOVO TRENO");

        System.out.println("Inserisci un id: ");
        String id = scanner.nextLine().trim().toLowerCase();

        System.out.println("Tipo treno:");
        System.out.println("1. ITALO");
        System.out.println("2. COMFORT");
        System.out.println("3. INTERCITY");
        System.out.print("Scelta: ");

        int tipoScelta = leggiScelta();
        TipoTreno tipo = null;

        switch (tipoScelta)
        {
            case 1: tipo = TipoTreno.ITALO; break;
            case 2: tipo = TipoTreno.COMFORT; break;
            case 3: tipo = TipoTreno.INTERCITY; break;
            default:
                System.out.println("Tipo non valido");
                return;
        }

        try
        {
            System.out.println("Recap specifiche: id="+id+", tipo="+tipo.name());
            controllerGRPC.aggiungiTreno(id, tipo);
            System.out.println("Treno " + id + " aggiunto con successo!");
        }
        catch (Exception e)
        {
            System.err.println("Errore: " + e.getMessage());
        }
    }

    private void rimuoviTreno()
    {
        System.out.println("\nRIMUOVI UN TRENO");
        System.out.println("Vuoi vedere i treni disponibili? Premi 1 per farlo");
        int scelta = leggiScelta();
        if(scelta == 1)
        {
            visualizzaTuttiITreni();
        }
        System.out.println("Inserisci l'id del treno da rimuovere: ");
        String id = scanner.nextLine().trim().toLowerCase();

        try
        {
            System.out.println("Recap specifiche\n: id treno da eliminare="+id);
            controllerGRPC.rimuoviTreno(id);
            System.out.println("Treno " + id + " aggiunto con successo!");
        }
        catch (Exception e)
        {
            System.err.println("Errore: " + e.getMessage());
        }
    }

    private void visualizzaTuttiITreni()
    {
        System.out.println("\nELENCO TRENI REGISTRATI");
        System.out.println("==========================");

        List<Treno> lista = controllerGRPC.getTuttiITreni();
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for(Treno t : lista)
        {
            sb.append(t);
        }
        sb.append("\n]");
        System.out.println(sb.toString());
    }

    private void cercaTreno()
    {
        System.out.print("\nInserisci ID treno: ");
        String id = scanner.nextLine().trim();

        Treno treno = controllerGRPC.getTreno(id);
        if(treno == null)
            System.out.println("Il treno "+ id+" e' non è presente");
        else
            System.out.println("Trovato treno: \n"+treno.toString());
    }

    private void menuTratte()
    {
        while (true)
        {
            System.out.println("\nGESTIONE TRATTE");
            System.out.println("==================");
            System.out.println("1. Aggiungi nuova tratta");
            System.out.println("2. Visualizza tutte le tratte");
            System.out.println("0. Torna indietro");
            System.out.print("\nScelta: ");

            int scelta = leggiScelta();

            switch (scelta) {
                case 1: aggiungiTratta(); break;
                case 2: visualizzaTratte(); break;
                case 0: return;
                default: System.out.println("Scelta non valida");
            }
        }
    }

    private void aggiungiTratta()
    {
        System.out.println("\nAGGIUNGI NUOVA TRATTA");

        System.out.println("Vuoi vedere le stazioni disponibili? Premi 1 per farlo");
        int scelta = leggiScelta();
        if(scelta == 1)
        {
            visualizzaStazioni();
        }
        // Stazione partenza
        System.out.println("\nSTAZIONE DI PARTENZA");
        System.out.println("ID Stazione di Partenza: ");
        String idStazionePartenza = scanner.nextLine();

        Stazione partenza = controllerGRPC.getStazione(idStazionePartenza);
        while(partenza == null)
        {
            System.out.println("Spiacente, stazione "+ idStazionePartenza+" non trovata.\nPer riprovare prema 1");
            scelta = leggiScelta();
            if(scelta == 1)
            {
                idStazionePartenza = scanner.nextLine();
                partenza = controllerGRPC.getStazione(idStazionePartenza);
            }
            else
            {
                System.out.println("Tornando indietro..."); return;
            }
        }

        // Stazione arrivo
        System.out.println("\nSTAZIONE DI ARRIVO");
        System.out.print("ID Stazione di Arrivo: ");
        String idStazioneArrivo = scanner.nextLine();

        Stazione arrivo = controllerGRPC.getStazione(idStazioneArrivo);
        while(arrivo == null)
        {
            System.out.println("Spiacente, stazione "+ idStazioneArrivo+" non trovata.\nPer riprovare prema 1");
            scelta = leggiScelta();
            if(scelta == 1)
            {
                idStazioneArrivo = scanner.nextLine();
                arrivo = controllerGRPC.getStazione(idStazioneArrivo);
            }
            else
            {
                System.out.println("Tornando indietro..."); return;
            }
        }
        try
        {
            Tratta tratta = new Tratta(partenza, arrivo);
            controllerGRPC.aggiungiTratta(tratta);
            System.out.println("Tratta " + tratta.getId() + " aggiunta con successo!");
            System.out.println("Dettagli tratta:\n"+ tratta.toString());
        }
        catch (Exception e)
        {
            System.err.println("Errore: " + e.getMessage());
        }
    }

    private void visualizzaTratte()
    {
        System.out.println("\nELENCO TRATTE DISPONIBILI");
        System.out.println("============================");

        List<Tratta> tratte = controllerGRPC.getTutteLeTratte();
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for(Tratta t : tratte)
        {
            String stringa = t+"\n";
            sb.append(stringa);
        }
        sb.append("\n]");
        System.out.println(sb.toString());
    }

    private void menuStazioni()
    {
        while(true)
        {
            System.out.println("\nGESTIONE STAZIONI");
            System.out.println("==================");
            System.out.println("1. Aggiungi una nuova stazione");
            System.out.println("2. Rimuovi una stazione");
            System.out.println("3. Prendi tutte le stazioni");
            System.out.println("0. Torna indietro");
            System.out.print("\nScelta: ");

            int scelta = leggiScelta();

            switch (scelta)
            {
                case 1: aggiungiStazione(); break;
                case 2: rimuoviStazione(); break;
                case 3: visualizzaStazioni(); break;
                case 0: return;
                default: System.out.println("Scelta non valida");
            }
        }
    }

    private void rimuoviStazione()
    {
        System.out.println("\nRIMUOVI UNA STAZIONE");
        System.out.println("Vuoi vedere le stazioni disponibili? Premi 1 per farlo");
        int scelta = leggiScelta();
        if(scelta == 1)
        {
            visualizzaStazioni();
        }
        System.out.println("Inserisci l'id della stazione da rimuovere: ");
        String id = scanner.nextLine().trim().toLowerCase();

        try
        {
            System.out.println("Recap specifiche\n: id stazione da eliminare="+id);
            controllerGRPC.rimuoviStazione(id);
            System.out.println("Stazione " + id + " rimossa con successo!");
        }
        catch (Exception e)
        {
            System.err.println("Errore: " + e.getMessage());
        }
    }

    private void aggiungiStazione()
    {
        System.out.println("\nAGGIUNGI NUOVA STAZIONE");

        System.out.println("Nome della citta': ");
        String città = scanner.nextLine();
        System.out.println("Nome della stazione: ");
        String nome = scanner.nextLine();
        System.out.println("Latitudine: ");
        double latitudine = Double.parseDouble(scanner.nextLine());
        System.out.println("Longitudine: ");
        double longitudine = Double.parseDouble(scanner.nextLine());
        while(città == null || nome == null)
        {
            System.out.println("Dati inseriti in un formato non applicabile.\nPer riprovare prema 1");
            int scelta = leggiScelta();
            if(scelta == 1)
            {
                System.out.println("Nome della citta': ");
                città = scanner.nextLine();
                System.out.println("Nome della stazione: ");
                nome = scanner.nextLine();
            }
            else
            {
                System.out.println("Tornando indietro...");
                return;
            }
        }

        System.out.println("Elenca i binari da aggiungere: ");
        ArrayList<Integer> binari = promptBinari();
        while(binari == null || binari.isEmpty())
        {
            System.out.println("La lista di binari non può essere vuota, se desidera annullare l'intera operazione, prema 0");
            int scelta = leggiScelta();
            if(scelta == 1)
            {
                System.out.println("Tornando indietro...");
                return;
            }
            else
            {
                binari = promptBinari();
            }
        }
        try
        {
            Stazione nuovaStazione = new Stazione(città, nome, binari, latitudine, longitudine);
            controllerGRPC.aggiungiStazione(nuovaStazione);
            System.out.println("Stazione " + nuovaStazione.getId() + " aggiunta con successo!");
            System.out.println("Dettagli stazione:\n"+ nuovaStazione.toString());
        }
        catch (Exception e)
        {
            System.err.println("Errore: " + e.getMessage());
        }
    }

    private ArrayList<Integer> promptBinari() {
        ArrayList<Integer> binari = new ArrayList<>();
        System.out.println("Digita il numero di ciascun binario e premi INVIO.");
        System.out.println("Quando hai finito, premi INVIO su riga vuota per terminare.");
        System.out.println("Per annullare l’operazione, premi INVIO senza aver inserito alcun binario.");

        while (true)
        {
            System.out.print("Binario #" + (binari.size()+1) + ": ");
            String line = scanner.nextLine().trim();
            // Se è vuoto al primo prompt → annulliamo
            if (line.isEmpty() && binari.isEmpty())
            {
                return null;
            }
            // Se è vuoto dopo aver almeno inserito un binario → finisco
            if (line.isEmpty())
            {
                break;
            }
            // Provo a fare il parse
            try
            {
                int numero = Integer.parseInt(line);
                binari.add(numero);
            }
            catch (NumberFormatException ex)
            {
                System.out.println("Input non valido, devi inserire un numero intero per il binario");
            }
        }
        return binari;
    }

    private void visualizzaStazioni()
    {
        System.out.println("\nELENCO STAZIONI DISPONIBILI");
        System.out.println("============================");

        List<Stazione> stazioni = controllerGRPC.getTutteLeStazioni();
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for(Stazione s : stazioni)
        {
            String stringa = s+"\n";
            sb.append(stringa);
        }
        sb.append("\n]");
        System.out.println(sb.toString());
    }

    private void menuViaggi()
    {
        while (true)
        {
            System.out.println("\nGESTIONE VIAGGI");
            System.out.println("==================");
            System.out.println("1. Programma nuovo viaggio");
            System.out.println("2. Aggiorna stato viaggio");
            System.out.println("3. Aggiungi ritardo");
            System.out.println("4. Visualizza viaggi del giorno");
            System.out.println("0. Torna indietro");
            System.out.print("\nScelta: ");

            int scelta = leggiScelta();

            switch (scelta)
            {
                case 1: programmaViaggio(); break;
                case 2: aggiornaStatoViaggio(); break;
                case 3: aggiungiRitardo(); break;
                case 4: visualizzaViaggi(); break;
                case 0: return;
                default: System.out.println("Scelta non valida");
            }
        }
    }

    private void programmaViaggio() {
        System.out.println("\nPROGRAMMA NUOVO VIAGGIO");

        System.out.println("Premi 1 per vedere tutti i treni, 0 altrimenti");
        if(leggiScelta() == 1)
        {
            visualizzaTuttiITreni();
        }
        System.out.print("ID Treno: ");
        String idTreno = scanner.nextLine().trim();

        System.out.println("Premi 1 per vedere tutte le tratte, 0 altrimenti");
        if(leggiScelta() == 1)
        {
            visualizzaTratte();
        }
        System.out.print("ID Tratta: ");
        String idTratta = scanner.nextLine().trim();

        System.out.print("Data partenza (dd/mm/yyyy): ");
        String dataStr = scanner.nextLine();

        System.out.print("Ora partenza (HH:mm): ");
        String oraPartenzaStr = scanner.nextLine();

        System.out.println("Data arrivo (dd/mm/yyyy):");
        String dataArr = scanner.nextLine();

        System.out.print("Ora arrivo (HH:mm): ");
        String oraArrivoStr = scanner.nextLine();

        try {
            Calendar partenza = parseDataOra(dataStr, oraPartenzaStr);
            Calendar arrivo = parseDataOra(dataArr, oraArrivoStr);

            Viaggio v = controllerGRPC.programmaViaggio(idTreno, idTratta, partenza, arrivo);
            System.out.println("Viaggio programmato con successo!");
            System.out.println("Dettagli viaggio\n"+ v.toString());
        }
        catch (Exception e)
        {
            System.err.println("Errore: " + e.getMessage());
        }
    }

    private void aggiornaStatoViaggio() {
        System.out.println("\nAGGIORNA STATO VIAGGIO");

        System.out.print("ID Viaggio: ");
        String idViaggio = scanner.nextLine().trim();

        System.out.println("Nuovo stato:");
        System.out.println("1. PROGRAMMATO");
        System.out.println("2. IN_CORSO");
        System.out.println("3. TERMINATO");
        System.out.println("4. IN_RITARDO");
        System.out.print("Scelta: ");

        int statoScelta = leggiScelta();
        StatoViaggio nuovoStato = null;

        switch (statoScelta)
        {
            case 1: nuovoStato = StatoViaggio.PROGRAMMATO; break;
            case 2: nuovoStato = StatoViaggio.IN_CORSO; break;
            case 3: nuovoStato = StatoViaggio.TERMINATO; break;
            case 4: nuovoStato = StatoViaggio.IN_RITARDO; aggiungiRitardo(); break;
            default:
                System.out.println("Stato non valido");
                return;
        }

        try
        {
            controllerGRPC.aggiornaStatoViaggio(idViaggio, nuovoStato);
            System.out.println("Stato aggiornato con successo!");
        } catch (Exception e) {
            System.err.println("Errore: " + e.getMessage());
        }
    }

    private void aggiungiRitardo()
    {
        try
        {
            System.out.println("\nAGGIUNGI RITARDO");

            System.out.print("ID Viaggio: ");
            String idViaggio = scanner.nextLine().trim();

            System.out.print("Minuti di ritardo: ");
            int minuti = leggiIntero();

            ControllerGRPC.getInstance().aggiornaRitardoViaggio(idViaggio, minuti);
        }catch(Exception e)
        {
            System.err.println("Errore: "+e.getMessage());
        }
    }

    private void visualizzaViaggi()
    {
        System.out.println("\nVIAGGI");
        System.out.println("==================");

        System.out.println("Quali viaggi ti piacerebbe visualizzare?");
        System.out.println("1. Viaggi programmati");
        System.out.println("2. Viaggi in ritardo");
        System.out.println("3. Viaggi in corso");
        System.out.println("4. Viaggi terminati");
        System.out.println("5. Viaggi per data");
        System.out.println("0. Torna indietro");

        int scelta = leggiScelta();
        switch (scelta)
        {
            case 1: cercaViaggi(StatoViaggio.PROGRAMMATO); break;
            case 2: cercaViaggi(StatoViaggio.IN_RITARDO); break;
            case 3: cercaViaggi(StatoViaggio.IN_CORSO); break;
            case 4: cercaViaggi(StatoViaggio.TERMINATO); break;
            case 5: cercaViaggiPerData();break;
            case 0: System.out.println("Tornando indietro..."); break;
            default : System.out.println("Scelta non valida");
                return;
        }
    }

    private void cercaViaggi(StatoViaggio statoViaggio)
    {
        System.out.println("Ricercando viaggi... di tipo "+ statoViaggio.name());
        try
        {
            List<Viaggio> viaggi = controllerGRPC.getViaggiPerStato(statoViaggio);
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for(Viaggio v : viaggi)
            {
                sb.append(v);
            }
            sb.append("\n]");
            System.out.println(sb.toString());
        }
        catch(Exception e)
        {
            System.err.println("Errore: "+ e.getMessage());
        }
    }

    private void cercaViaggiPerData()
    {
        System.out.println("Inserisci una data nel formato dd/MM/yyyy");
        System.out.println("Da: ");
        String da = scanner.nextLine();
        System.out.println("A: ");
        String a = scanner.nextLine();
        while(da == null || a == null)
        {
            System.out.println("Le date inserite non sono corrette. Premere 1 per riprovare (0 per tornare indietro)");
            int scelta = leggiScelta();
            if(scelta == 1)
            {
                System.out.println("Da: ");
                da = scanner.nextLine();
                System.out.println("A: ");
                a = scanner.nextLine();
            }
            else
            {
                System.out.println("Tornando indietro...");
                return;
            }
        }

        Calendar dataDa = parseDataOra(da, "00:00");
        Calendar dataA = parseDataOra(a, "23:59");
        try
        {
            List<Viaggio> viaggi = controllerGRPC.getViaggiPerData(dataDa, dataA);
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for(Viaggio v : viaggi)
            {
                sb.append(v);
            }
            sb.append("\n]");
            System.out.println(sb.toString());
        }
        catch(Exception e)
        {
            System.err.println("Errore: "+ e.getMessage());
        }
    }

    private void menuPromozioni()
    {
        System.out.println("\nGESTIONE PROMOZIONI");
        System.out.println("1. Crea Promozione");
        System.out.println("2. Visualizza tutte le promozioni");
        System.out.println("3. Visualizza promo per categoria");
        System.out.println("0. torna indietro");

        int scelta = leggiScelta();
        switch (scelta)
        {
            case 1: creaPromozione(); break;
            case 2: visualizzaPromozioni(); break;
            case 3: visualizzaPromoPerCategoria(); break;
            case 0: System.out.println("Tornando indietro..."); return;
            default: System.out.println("Scelta non valida"); return;
        }
        pausa();
    }

    private void visualizzaPromoPerCategoria()
    {
        System.out.println("\nELENCO PROMOZIONI PER CATEGORIA");
        try
        {
            System.out.println("Inserisci Categoria: ");
            TipoPromozione tipo = TipoPromozione.valueOf(scanner.nextLine());

            List<Promozione> promozioni = controllerGRPC.getPromoPerTipo(tipo);
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for(Promozione p : promozioni)
            {
                sb.append(p);
            }
            sb.append("\n]");
            System.out.println(sb.toString());

        }
        catch (Exception e)
        {
            System.err.println("Errore: "+e.getMessage());
        }
    }

    private void visualizzaPromoPerCategoria(TipoPromozione tipo)
    {
        System.out.println("\nELENCO PROMOZIONI PER CATEGORIA "+ tipo.name());
        try
        {
            List<Promozione> promozioni = controllerGRPC.getPromoPerTipo(tipo);
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for(Promozione p : promozioni)
            {
                sb.append(p);
            }
            sb.append("\n]");
            System.out.println(sb.toString());

        }
        catch (Exception e)
        {
            System.err.println("Errore: "+e.getMessage());
        }
    }

    private void visualizzaPromozioni()
    {
        System.out.println("\nELENCO PROMOZIONI");
        try
        {
            List<Promozione> promozioni = controllerGRPC.getPromo();
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for(Promozione p : promozioni)
            {
                sb.append(p).append("\n");
            }
            sb.append("\n]");
            System.out.println(sb.toString());
            menuPromozioni();
        }
        catch (Exception e)
        {
            System.err.println("Errore: "+e.getMessage());
        }
    }

    private void creaPromozione()
    {
        try
        {
            System.out.println("\nCREA PROMOZIONE");
            System.out.println("Scegli il tipo: ");
            System.out.println("1. Treno");
            System.out.println("2. Fedelta");
            System.out.println("3. Tratta");
            System.out.println("0. torna indietro");
            int scelta = leggiScelta();
            switch (scelta)
            {
                case 1: creaPromozioneTreno(); break;
                case 2: creaPromozioneFedelta(); break;
                case 3: creaPromozioneTratta(); break;
                default: System.out.println("Tornando indietro..."); return;
            }
        }catch(Exception e)
        {
            System.err.println("Errore: "+ e.getMessage());
        }
    }

    private void creaPromozioneTreno() throws Exception {
        System.out.println("Hai scelto di creare una promozione basata su un treno");
        System.out.println("Premi 1 per vedere tutti i treni disponibili, 0 se hai già un id in mente: ");
        int vedere = leggiIntero();
        if(vedere == 1)
        {
            visualizzaTuttiITreni();
        }
        System.out.println("Vuoi anche vedere tutte le promozioni di questo tipo? In tal caso premi 1");
        vedere = leggiIntero();
        if(vedere == 1)
        {
            visualizzaPromoPerCategoria(TipoPromozione.TRENO);
        }
        System.out.println("Inserisci l'id del treno: ");
        String idTreno = scanner.nextLine();
        Treno treno = controllerGRPC.getTreno(idTreno);
        while(treno == null)
        {
            System.out.println("Spiacente, la scelta non è valida, per riprovare prema 1");
            int scelta = leggiIntero();
            if(scelta == 1)
            {
                System.out.println("Inserisci l'id del treno: ");
                idTreno = scanner.nextLine();
                treno = controllerGRPC.getTreno(idTreno);
            }
            else
            {
                System.out.println("Tornando indietro...");
                return;
            }
        }
        Calendar inizio = null;
        Calendar fine = null;
        boolean dateValide = false;

        while (!dateValide)
        {
            System.out.println("Digita la data di inizio promozione nel formato dd/MM/yyyy: ");
            String dataInizio = scanner.nextLine();
            System.out.println("Digita la data di fine promozione nel formato dd/MM/yyyy: ");
            String dataFine = scanner.nextLine();

            inizio = parseDataOra(dataInizio, "00:00");
            fine = parseDataOra(dataFine, "23:59");

            Calendar oggi = Calendar.getInstance();

            if (inizio.before(oggi))
            {
                System.err.println("Errore: La data di inizio non può essere nel passato!");
                System.out.println("Premi 1 per reinserire le date, 0 per annullare: ");
                int scelta = leggiIntero();
                if (scelta != 1)
                {
                    System.out.println("Creazione promozione annullata.");
                    return;
                }
            }
            else if (inizio.after(fine))
            {
                System.err.println("Errore: La data di inizio non può essere dopo la data di fine!");
                System.out.println("Premi 1 per reinserire le date, 0 per annullare: ");
                int scelta = leggiIntero();
                if (scelta != 1)
                {
                    System.out.println("Creazione promozione annullata.");
                    return;
                }
            }
            else
            {
                dateValide = true;
            }
        }
        System.out.println("Inserisci lo sconto da applicare: ");
        double sconto = scanner.nextDouble();
        scanner.nextLine();

        controllerGRPC.creaPromozione(TipoPromozione.TRENO, inizio, fine, sconto, null, treno);
        System.out.println("Promozione creata con successo!");
    }

    private void creaPromozioneFedelta()
    {
        try
        {
            System.out.println("Hai scelto di creare una promozione fedelta");
            System.out.println("Premi 1 per vedere tutte le promozioni fedelta disponibili, 0 altrimenti");
            int vedere = leggiIntero();
            if(vedere == 1)
            {
                visualizzaPromoPerCategoria(TipoPromozione.FEDELTA);
            }
            System.out.println("Digita la data di inizio promozione nel formato dd/MM/yyyy: ");
            String dataInizio = scanner.nextLine();
            System.out.println("Digita la data di fine promozione nel formato dd/MM/yyyy: ");
            String dataFine= scanner.nextLine();
            System.out.println("Inserisci lo sconto da applicare: ");
            double sconto = scanner.nextDouble();
            scanner.nextLine();

            Calendar inizio = parseDataOra(dataInizio, "00:00");
            Calendar fine = parseDataOra(dataFine, "23:59");

            controllerGRPC.creaPromozione(TipoPromozione.FEDELTA, inizio, fine, sconto, null, null);
            System.out.println("Promozione creata con successo!");
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void creaPromozioneTratta() throws Exception {
        System.out.println("Hai scelto di creare una promozione basata su una tratta");
        System.out.println("Premi 1 per vedere tutti le tratte disponibili, 0 se hai già un id in mente: ");
        int vedere = leggiIntero();
        if(vedere == 1)
        {
            visualizzaTratte();
        }
        System.out.println("Vuoi anche vedere tutte le promozioni di questo tipo? In tal caso premi 1");
        vedere = leggiIntero();
        if(vedere == 1)
        {
            visualizzaPromoPerCategoria(TipoPromozione.TRATTA);
        }
        System.out.println("Inserisci l'id del tratta: ");
        String idTratta = scanner.nextLine();
        Tratta tratta = controllerGRPC.getTratta(idTratta);
        while(tratta == null)
        {
            System.out.println("Spiacente, la scelta non è valida, per riprovare prema 1");
            int scelta = leggiIntero();
            if(scelta == 1)
            {
                System.out.println("Inserisci l'id del tratta: ");
                idTratta = scanner.nextLine();
                tratta = controllerGRPC.getTratta(idTratta);
            }
            else
            {
                System.out.println("Tornando indietro...");
                return;
            }
        }
        System.out.println("Adesso digita la data di inizio promozione nel formato dd/MM/yyyy: ");
        String dataInizio = scanner.nextLine();
        System.out.println("Adesso digita la data di fine promozione nel formato dd/MM/yyyy: ");
        String dataFine= scanner.nextLine();
        System.out.println("Inserisci lo sconto da applicare: ");
        double sconto = scanner.nextDouble();
        scanner.nextLine();

        Calendar inizio = parseDataOra(dataInizio, "00:00");
        Calendar fine = parseDataOra(dataFine, "23:59");

        controllerGRPC.creaPromozione(TipoPromozione.TRATTA, inizio, fine, sconto, tratta, null);
        System.out.println("Promozione creata con successo!");
    }

    private void menuClienti() {
        System.out.println("\n👥 GESTIONE CLIENTI");
        System.out.println("In sviluppo...");
        pausa();
    }

    private void menuDatabase() {
        System.out.println("\n🔧 UTILITÀ DATABASE");
        System.out.println("1. Backup database");
        System.out.println("2. Ripristina database");
        System.out.println("3. Pulisci dati vecchi");
        System.out.println("In sviluppo...");
        pausa();
    }

   private int leggiScelta() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int leggiIntero() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Calendar parseDataOra(String data, String ora)
    {
        try
        {
            //utilizzo SimpleDateFormat
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            sdf.setLenient(false); //questo per evitare problemi di time zone

            //un'unica stringa costruita come dd/MM/yyyy HH:mm
            String dataCompleta = data.trim() + " " + ora.trim();

            Date dataRifatta = sdf.parse(dataCompleta);

            Calendar cal = Calendar.getInstance();
            cal.setTime(dataRifatta);
            return cal;
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Formato data/ora non valido");
        }
    }

    private void pausa() {
        System.out.print("\nPremi INVIO per continuare...");
        scanner.nextLine();
    }

    public static void main(String[] args) {
        AdminCLI admin = new AdminCLI();
        admin.avvia();
    }
}

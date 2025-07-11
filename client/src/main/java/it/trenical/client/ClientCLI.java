package it.trenical.client;

import it.trenical.client.domain.AuthController;
import it.trenical.client.domain.BigliettoController;
import it.trenical.client.domain.ProfiloController;
import it.trenical.client.domain.ViaggioController;
import it.trenical.client.singleton.SessioneCliente;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.TipoTreno;
import it.trenical.server.domain.gestore.GestoreClienti;
import it.trenical.server.dto.BigliettoDTO;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.dto.ModificaClienteDTO;
import it.trenical.server.dto.ViaggioDTO;

import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

/*
 Interfaccia a riga di comando principale per TreniCal.
 Gestisce la navigazione tra i diversi menu e coordina i controller.
 */
public class ClientCLI
{

    //Controllers per fare cose
    private final AuthController authController;
    private final ViaggioController viaggioController;
    private final BigliettoController bigliettoController;
    private final ProfiloController profiloController;

    //Input scanner
    private final Scanner scanner;

    //Flag per controllo loop principale
    private boolean running = true;

    public ClientCLI()
    {
        this.authController = new AuthController();
        this.viaggioController = new ViaggioController();
        this.bigliettoController = new BigliettoController();
        this.profiloController = new ProfiloController();
        this.scanner = new Scanner(System.in);
    }


    public void avvia()
    {
        mostraBenvenuto();

        while (running)
        {
            try
            {
                mostraMenuPrincipale();
                int scelta = leggiScelta();
                gestisciSceltaPrincipale(scelta);
            }
            catch (Exception e)
            {
                System.err.println("Errore imprevisto: " + e.getMessage());
                System.out.println("Ritorno al menu principale...\n");
            }
        }

        mostraArrivederci();
    }

    private void mostraMenuPrincipale()
    {
        System.out.println("\n" + "==================================================================");
        System.out.println("TRENICAL - SISTEMA BIGLIETTI FERROVIARI");
        System.out.println("==================================================================");

        if (SessioneCliente.getInstance().isLoggato())
        {
            mostraMenuLoggato();
        }
        else
        {
            mostraMenuNonLoggato();
        }
    }

    private void mostraMenuNonLoggato()
    {
        System.out.println("Benvenuto! Effettua l'accesso per continuare\n");
        System.out.println("1. Accedi");
        System.out.println("2. Registrati");
        System.out.println("3. Cerca viaggi");
        System.out.println("0. Esci");
        System.out.print("\nScegli un'opzione: ");
    }

    private void mostraMenuLoggato()
    {
        String nomeUtente = SessioneCliente.getInstance().getNomeClienteCorrente();
        System.out.println("Benvenut*, " + nomeUtente + "!");

        //mostro notifiche e promozioni se disponibili
        mostraNotificheRapide();

        System.out.println("\nMENU PRINCIPALE");
        System.out.println("1. Cerca e prenota viaggi");
        System.out.println("2. I miei biglietti");
        System.out.println("3. Il mio profilo");
        System.out.println("4. Notifiche");
        System.out.println("5. Promozioni attive");
        System.out.println("6. Treni Seguiti");
        System.out.println("7. Logout");
        System.out.println("0. Esci");
        System.out.print("\nScegli un'opzione: ");
    }

    private void gestisciSceltaPrincipale(int scelta)
    {
        if (SessioneCliente.getInstance().isLoggato())
        {
            gestisciSceltaLoggato(scelta);
        }
        else
        {
            gestisciSceltaNonLoggato(scelta);
        }
    }

    private void gestisciSceltaNonLoggato(int scelta)
    {
        switch (scelta)
        {
            case 1:
                menuLogin();
                break;
            case 2:
                menuRegistrazione();
                break;
            case 3:
                menuRicercaViaggiSolaLettura();
                break;
            case 0:
                running = false;
                break;
            default:
                System.out.println("Opzione non valida, riprova.");
        }
    }

    private void gestisciSceltaLoggato(int scelta)
    {
        switch (scelta)
        {
            case 1:
                menuViaggi();
                break;
            case 2:
                menuBiglietti();
                break;
            case 3:
                menuProfilo();
                break;
            case 4:
                menuNotifiche();
                break;
            case 5:
                menuPromozioni();
                break;
            case 6:
                menuTreniSeguiti();
                break;
            case 7:
                logout();
                break;
            case 0:
                running = false;
                break;
            default:
                System.out.println("Opzione non valida, riprova.");
        }
    }

    private void menuLogin()
    {
        System.out.println("\nACCESSO");
        System.out.println("=============================================");

        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        if (authController.login(email, password))
        {
            System.out.println("Accesso effettuato con successo!");
            pausa();
        }
        else
        {
            System.out.println("Credenziali non valide.");
            pausa();
        }
    }

    private void menuRegistrazione()
    {
        System.out.println("\nREGISTRAZIONE");
        System.out.println("=============================================");

        System.out.print("Nome: ");
        String nome = scanner.nextLine().trim();

        System.out.print("Cognome: ");
        String cognome = scanner.nextLine().trim();

        System.out.println("Sono Ammessi per la mail: ");
        System.out.println("- Lettere maiuscole (A-Z)");
        System.out.println("- Lettere minuscole (a-z)");
        System.out.println("- Cifre (0-9)");
        System.out.println("- Punto (.)");
        System.out.println("- Underscore (_)");
        System.out.println("- Percentuale (%)");
        System.out.println("- Più (+)");
        System.out.println("- Trattino (-)\n");
        System.out.println("Non sono Ammessi per la mail");
        System.out.println("- Spazi o tabulazioni");
        System.out.println("- Accenti o lettere straniere (es: à, è, ñ, ü)");
        System.out.println("- Simboli non standard come: ! # & * = ? : ; / \\ \" ' < > ~ ^ | { } [ ]");
        System.out.println("- Domini diversi da @gmail.com (es: @yahoo.it, @hotmail.com ecc.)");
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        System.out.println("""
        La password deve rispettare queste regole:
        
        1. Deve iniziare con una **lettera** (a-z, A-Z) oppure uno dei simboli: @ # $ % ^ & + =
        2. Deve contenere almeno:
           - Una **lettera maiuscola** (es: A-Z)
           - Una **lettera minuscola** (es: a-z)
           - Un **numero** (es: 0-9)
           - Un **simbolo speciale** tra: @ # $ % ^ & + =
        3. Deve essere lunga almeno **8 caratteri**
        4. Deve **finire** con una **lettera o un numero** (non un simbolo)
        """);

        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        System.out.print("Numero carta (16 cifre): ");
        String numeroCarta = scanner.nextLine().trim();

        System.out.print("Vuoi aderire al programma FedeltàTreno? (s/n): ");
        boolean wantsFedelta = scanner.nextLine().trim().toLowerCase().startsWith("s");

        boolean wantsNotifiche = true;
        boolean wantsPromozioni = false;

        if (wantsFedelta)
        {
            System.out.print("Vuoi ricevere notifiche promozionali FedeltàTreno? (s/n): ");
            wantsPromozioni = scanner.nextLine().trim().toLowerCase().startsWith("s");
        }

        if (authController.registrati(nome, cognome, email, password, numeroCarta,
                wantsFedelta, wantsNotifiche, wantsPromozioni))
        {
            System.out.println("Registrazione completata! Ora puoi effettuare l'accesso.");
            System.out.println("I tuoi dati bancari sono stati salvati e verranno utilizzati per i pagamenti.");
        }
        else
        {
            System.out.println("Registrazione fallita.");
        }
        pausa();
    }

    private void logout()
    {
        authController.logout();
        System.out.println("Logout effettuato con successo!");
        pausa();
    }

    private void menuViaggi()
    {
        while (true)
        {
            System.out.println("\nGESTIONE VIAGGI");
            System.out.println("=============================================");
            System.out.println("1. Cerca viaggi");
            System.out.println("2. Acquista biglietto");
            System.out.println("0. Torna al menu principale");
            System.out.print("\nScegli un'opzione: ");

            int scelta = leggiScelta();

            switch (scelta)
            {
                case 1:
                    cercaViaggi();
                    break;
                case 2:
                    acquistaBiglietto();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Opzione non valida.");
            }
        }
    }

    private void menuRicercaViaggiSolaLettura()
    {
        System.out.println("\nRICERCA VIAGGI (SOLO VISUALIZZAZIONE)");
        System.out.println("=============================================");
        System.out.println("Effettua l'accesso per prenotare biglietti");

        cercaViaggi();
    }

    private void cercaViaggi()
    {
        System.out.println("\nRICERCA VIAGGI");
        System.out.println("=============================================");

        System.out.print("Città di partenza: ");
        String partenza = scanner.nextLine().trim();

        System.out.print("Città di arrivo: ");
        String arrivo = scanner.nextLine().trim();

        System.out.print("Data viaggio (dd/mm/yyyy): ");
        String dataStr = scanner.nextLine().trim();
        Calendar dataViaggio = parseData(dataStr);

        if (dataViaggio == null)
        {
            System.out.println("Formato data non valido.");
            return;
        }

        System.out.print("Numero passeggeri: ");
        int passeggeri = leggiIntero();

        System.out.println("\nClasse di servizio preferita:");
        System.out.println("1. Economy  2. Business  3. Low Cost  4. Fedeltà");
        System.out.print("Scelta (default Economy): ");
        int classeChoice = leggiScelta();
        ClasseServizio classe = scegliClasse(classeChoice);

        System.out.println("\nTipo treno preferito:");
        System.out.println("1. Italo  2. Comfort  3. Intercity");
        System.out.print("Scelta (default Comfort): ");
        int trenoChoice = leggiScelta();
        TipoTreno tipoTreno = scegliTipoTreno(trenoChoice);

        //Solo andata per ora
        List<ViaggioDTO> risultati = viaggioController.cercaViaggio(
                partenza, arrivo, dataViaggio, null, passeggeri, classe, tipoTreno
        );

        if (!risultati.isEmpty() && SessioneCliente.getInstance().isLoggato())
        {
            System.out.print("\nVuoi acquistare un biglietto? (s/n): ");
            if (scanner.nextLine().trim().toLowerCase().startsWith("s"))
            {
                mostraAcquistoRapido(risultati);
            }
        }
    }

    private void acquistaBiglietto()
    {
        System.out.println("\nACQUISTO BIGLIETTO");
        System.out.println("=============================================");

        System.out.print("ID Viaggio: ");
        String idViaggio = scanner.nextLine().trim();

        System.out.println("\nScegli classe di servizio:");
        System.out.println("1. Economy  2. Business  3. Low Cost  4. Fedeltà");
        System.out.print("Scelta: ");
        int classeChoice = leggiScelta();
        ClasseServizio classe = scegliClasse(classeChoice);

        System.out.print("Numero biglietti: ");
        int numeroBiglietti = leggiIntero();

        if (viaggioController.acquistaBiglietto(idViaggio, classe, numeroBiglietti))
        {
            System.out.println("Acquisto completato!");
        }
        else
        {
            System.out.println("Acquisto fallito.");
        }

        pausa();
    }

    private void mostraAcquistoRapido(List<ViaggioDTO> viaggi)
    {
        System.out.println("\nSELEZIONE VIAGGIO PER ACQUISTO");

        for (int i = 0; i < Math.min(viaggi.size(), 5); i++)
        {
            ViaggioDTO v = viaggi.get(i);
            System.out.printf("%d. %s → %s | %s | €%.2f di aumento prezzo stimato in base al tipo di treno%n",
                    i + 1,
                    v.getCittaPartenza(),
                    v.getCittaArrivo(),
                    formatCalendar(v.getInizio()),
                    v.getTipo().getAumentoPrezzo() // Stima prezzo
            );
        }

        System.out.print("\nSelezione viaggio (0 per annullare): ");
        int scelta = leggiScelta();

        if (scelta > 0 && scelta <= viaggi.size())
        {
            ViaggioDTO viaggioSelezionato = viaggi.get(scelta - 1);
            double kilometri = viaggioSelezionato.getKilometri();
            double aggiuntaTipo = viaggioSelezionato.getTipo().getAumentoPrezzo();
            double aggiuntaServizio1 = ClasseServizio.ECONOMY.getCoefficienteAumentoPrezzo();
            double aggiuntaServizio2 = ClasseServizio.BUSINESS.getCoefficienteAumentoPrezzo();
            double aggiuntaServizio3 = ClasseServizio.LOW_COST.getCoefficienteAumentoPrezzo();
            double aggiuntaServizio4 = ClasseServizio.FEDELTA.getCoefficienteAumentoPrezzo();

            double prezzo1 = kilometri * aggiuntaServizio1 * aggiuntaTipo;
            double prezzo2 = kilometri * aggiuntaServizio2 * aggiuntaTipo;
            double prezzo3 = kilometri * aggiuntaServizio3 * aggiuntaTipo;
            double prezzo4 = kilometri * aggiuntaServizio4 * aggiuntaTipo;
            System.out.println("\nClasse di servizio:");
            System.out.printf("1. Economy: %.2f%n \n2. Business %.2f%n \n3. Low Cost %.2f%n \n4. Fedeltà %.2f%n"
            , prezzo1, prezzo2, prezzo3, prezzo4);

            System.out.print("Scelta: ");
            int classeChoice = leggiScelta();
            ClasseServizio classe = scegliClasse(classeChoice);

            viaggioController.acquistaBiglietto(viaggioSelezionato.getID(), classe);
        }
    }

    private void menuBiglietti()
    {
        while (true)
        {
            System.out.println("\nI MIEI BIGLIETTI");
            System.out.println("=============================================");
            System.out.println("1. Visualizza tutti i biglietti");
            System.out.println("2. Dettagli biglietto");
            System.out.println("3. Modifica biglietto");
            System.out.println("4. Cancella biglietto");
            System.out.println("0. Torna al menu principale");
            System.out.print("\nScegli un'opzione: ");

            int scelta = leggiScelta();

            switch (scelta)
            {
                case 1:
                    bigliettoController.getMieiBiglietti();
                    pausa();
                    break;
                case 2:
                    dettagliBiglietto();
                    break;
                case 3:
                    modificaBiglietto();
                    break;
                case 4:
                    cancellaBiglietto();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Opzione non valida.");
            }
        }
    }

    private void dettagliBiglietto()
    {
        System.out.print("\nID Biglietto: ");
        String id = scanner.nextLine().trim();

        BigliettoDTO biglietto = bigliettoController.getBiglietto(id);
        if (biglietto != null)
        {
            pausa();
        }
    }

    private void modificaBiglietto()
    {
        System.out.print("\nID Biglietto da modificare: ");
        String id = scanner.nextLine().trim();

        System.out.println("\nNuova classe di servizio:");
        System.out.println("1. Economy  2. Business  3. Low Cost  4. Fedeltà");
        System.out.print("Scelta: ");
        int classeChoice = leggiScelta();
        ClasseServizio nuovaClasse = scegliClasse(classeChoice);

        if (bigliettoController.modificaClasseBiglietto(id, nuovaClasse))
        {
            System.out.println("Biglietto modificato!");
        }
        else
        {
            System.out.println("Modifica fallita.");
        }

        pausa();
    }

    private void cancellaBiglietto()
    {
        System.out.print("\nID Biglietto da cancellare: ");
        String id = scanner.nextLine().trim();

        if (bigliettoController.cancellaBiglietto(id))
        {
            System.out.println("Biglietto cancellato!");
        }
        else
        {
            System.out.println("Cancellazione fallita.");
        }

        pausa();
    }

    private void menuProfilo()
    {
        while (true)
        {
            System.out.println("\nIL MIO PROFILO");
            System.out.println("=============================================");
            System.out.println("1. Visualizza profilo");
            System.out.println("2. Modifica profilo");
            System.out.println("3. Gestisci FedeltàTreno");
            System.out.println("0. Torna al menu principale");
            System.out.print("\nScegli un'opzione: ");

            int scelta = leggiScelta();

            switch (scelta)
            {
                case 1:
                    profiloController.visualizzaProfilo();
                    pausa();
                    break;
                case 2:
                    modificaProfilo();
                    break;
                case 3:
                    gestisciFedelta();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Opzione non valida.");
            }
        }
    }

    private void modificaProfilo()
    {
        System.out.println("\nMODIFICA PROFILO");
        System.out.println("=============================================");

        ClienteDTO clienteCorrente = SessioneCliente.getInstance().getClienteCorrente();

        System.out.print("Nuovo nome (attuale: " + clienteCorrente.getNome() + "): ");
        String nome = scanner.nextLine().trim();
        if (nome.isEmpty())
            nome = clienteCorrente.getNome();

        System.out.print("Nuovo cognome (attuale: " + clienteCorrente.getCognome() + "): ");
        String cognome = scanner.nextLine().trim();
        if (cognome.isEmpty())
            cognome = clienteCorrente.getCognome();

        System.out.print("Nuova password: ");
        String password = scanner.nextLine().trim();
        if (password.isEmpty())
            password = clienteCorrente.getPassword();

        ModificaClienteDTO dto = new ModificaClienteDTO(
                clienteCorrente.getId(), nome, cognome, password, clienteCorrente.isFedelta(), clienteCorrente.isRiceviNotifiche()
        );

        if (profiloController.modificaProfilo(dto))
        {
            System.out.println("Profilo modificato!");
        }
        else
        {
            System.out.println("Modifica fallita.");
        }

        pausa();
    }

    private void gestisciFedelta()
    {
        ClienteDTO cliente = SessioneCliente.getInstance().getClienteCorrente();

        if (cliente.isFedelta())
        {
            System.out.println("\nGESTIONE FEDELTÀTRENO");
            System.out.println("=============================================");
            System.out.println("Sei già iscritto al programma FedeltàTreno!");
            System.out.print("Vuoi rimuovere l'adesione? (s/n): ");

            if (scanner.nextLine().trim().toLowerCase().startsWith("s"))
            {
                if (authController.rimuoviFedelta())
                {
                    System.out.println("Rimosso da FedeltàTreno.");
                }
            }
        }
        else
        {
            System.out.println("\nADESIONE FEDELTÀTRENO");
            System.out.println("=============================================");
            System.out.println("Vantaggi del programma FedeltàTreno:");
            System.out.println("   • Sconti esclusivi sui biglietti");
            System.out.println("   • Promozioni speciali dedicate");

            System.out.print("\nVuoi aderire? (s/n): ");
            if (scanner.nextLine().trim().toLowerCase().startsWith("s"))
            {
                System.out.print("Vuoi ricevere notifiche promozionali? (s/n): ");
                boolean notifiche = scanner.nextLine().trim().toLowerCase().startsWith("s");

                if (authController.aderisciAFedelta(notifiche))
                {
                    System.out.println("Benvenuto in FedeltàTreno!");
                }
            }
        }

        pausa();
    }

    private void menuNotifiche()
    {
        while (true)
        {
            System.out.println("\nNOTIFICHE");
            System.out.println("=============================================");
            System.out.println("1. Nuove notifiche");
            System.out.println("2. Storico completo");
            System.out.println("0. Torna al menu principale");
            System.out.print("\nScegli un'opzione: ");

            int scelta = leggiScelta();

            switch (scelta)
            {
                case 1:
                    profiloController.getNotifiche(true);
                    pausa();
                    break;
                case 2:
                    profiloController.visualizzaStoricoNotifiche();
                    pausa();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Opzione non valida.");
            }
        }
    }

    private void menuPromozioni()
    {
        System.out.println("\nPROMOZIONI ATTIVE");
        System.out.println("=============================================");

        profiloController.visualizzaPromozioni();
        pausa();
    }

    private void mostraBenvenuto()
    {
        System.out.println("\n" + "===================================================================================");
        System.out.println("BENVENUTO IN TRENICAL!");
        System.out.println("===================================================================================");
    }

    private void mostraArrivederci()
    {
        System.out.println("\n" + "===================================================================================");
        System.out.println("Grazie per aver usato TreniCal!");
        System.out.println("A presto!");
        System.out.println("===================================================================================");
        scanner.close();
    }

    private void mostraNotificheRapide()
    {
        try
        {
            int notifiche = profiloController.contaNotificheNonLette();
            if (notifiche > 0)
            {
                System.out.println(notifiche + " nuove notifiche");
            }

            if (profiloController.hasPromozioniDisponibili())
            {
                System.out.println("Promozioni attive disponibili!");
            }
        } catch (Exception e)
        {
            //meglio se non blocco il menu...
        }
    }

    private void menuTreniSeguiti() {
        while (true) {
            System.out.println("\n=== TRENI SEGUITI ===");
            System.out.println("1. Segui un nuovo treno");
            System.out.println("2. Visualizza treni seguiti");
            System.out.println("3. Smetti di seguire un treno");
            System.out.println("0. Torna al menu principale");
            System.out.print("\nScegli un'opzione: ");

            int scelta = leggiScelta();

            switch (scelta) {
                case 1:
                    seguiNuovoTreno();
                    break;
                case 2:
                    visualizzaTreniSeguiti();
                    break;
                case 3:
                    smettiDiSeguireTreno();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Opzione non valida.");
            }
        }
    }

    private void seguiNuovoTreno() {
        System.out.println("\n=== SEGUI UN NUOVO TRENO ===");
        System.out.println("Riceverai notifiche per tutti i viaggi di questo treno.");

        ClienteDTO cliente = SessioneCliente.getInstance().getClienteCorrente();
        if (!cliente.isRiceviNotifiche()) {
            System.out.println("\nLe tue notifiche viaggi sono disattivate.");
            System.out.println("Vuoi attivarle?" +
                    "\n1. Attiva" +
                    "\n0. Torna indietro");
            if (leggiScelta() == 1) {
                System.out.println("\nAttivazione notifiche in corso...");
                if (profiloController.aggiornaPreferenze(true, cliente.isRiceviPromozioni())) {
                    System.out.println("Notifiche attivate con successo!");
                } else {
                    System.err.println("Errore nell'attivazione delle notifiche.");
                    pausa();
                    return;
                }
            } else {
                System.out.println("Operazione annullata.");
                return;
            }
        }

        System.out.println("\nRecupero treni disponibili...");
        List<ViaggioController.TrenoSeguitoInfo> treniDisponibili = viaggioController.getTreniDisponibili();

        if (treniDisponibili.isEmpty()) {
            System.err.println("Spiacenti, nessun treno disponibile nel sistema.");
            pausa();
            return;
        }

        System.out.println("\nTreni disponibili nel sistema:");
        System.out.println("----------------------------------------");
        for (int i = 0; i < treniDisponibili.size(); i++) {
            ViaggioController.TrenoSeguitoInfo treno = treniDisponibili.get(i);
            System.out.printf("%d. %s (ID: %s)\n", i + 1, treno.getTipo(), treno.getId());
        }
        System.out.println("----------------------------------------");

        System.out.println("\nOpzioni:");
        System.out.println("- Inserisci il numero del treno da seguire (1-" + treniDisponibili.size() + ")");
        System.out.println("- Inserisci 0 per annullare");

        System.out.print("\nScelta: ");
        String input = scanner.nextLine().trim();

        if (input.equals("0")) {
            System.out.println("Operazione annullata.");
            return;
        }

        String trenoId = null;

        // Verifica se è un numero (selezione dalla lista)
        try {
            int scelta = Integer.parseInt(input);
            if (scelta >= 1 && scelta <= treniDisponibili.size())
            {
                trenoId = treniDisponibili.get(scelta - 1).getId();
            }
            else
            {
                System.err.println("Numero non valido.");
                pausa();
                return;
            }
        }
        catch (NumberFormatException e)
        {
            System.err.println("Numero non valido.");
            pausa();
            return;
        }

        System.out.print("Sicuro di voler seguire il treno " + trenoId +" ? (s/n): ");
        String conferma = scanner.nextLine().trim().toLowerCase();

        if (conferma.startsWith("s"))
        {
            if (viaggioController.seguiTreno(trenoId))
            {
                System.out.println("Operazione effettuata con successo!");
            }
        }
        else
        {
            System.out.println("Operazione annullata.");
        }
        pausa();
    }

    private void visualizzaTreniSeguiti()
    {
        System.out.println("\n=== I TUOI TRENI SEGUITI ===");

        List<ViaggioController.TrenoSeguitoInfo> treniSeguiti = viaggioController.getTreniSeguiti();

        if (treniSeguiti.isEmpty())
        {
            System.out.println("Non stai seguendo nessun treno.");
            System.out.println("Usa l'opzione 'Segui un nuovo treno' per iniziare!");
        }
        else
        {
            if(treniSeguiti.size() == 1)
            {
                System.out.println("Stai seguendo " + treniSeguiti.size() + " treno:\n");
            }
            else
            {
                System.out.println("Stai seguendo " + treniSeguiti.size() + " treni:\n");
            }
            for (int i = 0; i < treniSeguiti.size(); i++)
            {
                System.out.println((i + 1) + ". " + treniSeguiti.get(i));
            }
            System.out.println();
        }
        pausa();
    }

    private void smettiDiSeguireTreno()
    {
        System.out.println("\n=== SMETTI DI SEGUIRE UN TRENO ===");

        List<ViaggioController.TrenoSeguitoInfo> treniSeguiti = viaggioController.getTreniSeguiti();

        if (treniSeguiti.isEmpty())
        {
            System.out.println("Non stai seguendo nessun treno.");
            pausa();
            return;
        }

        System.out.println("Quale treno vuoi smettere di seguire?\n");

        visualizzaTreniSeguiti();
        System.out.println("0. Annulla");

        System.out.print("\nScegli: ");
        int scelta = leggiScelta();

        if (scelta == 0)
        {
            return;
        }

        if (scelta < 1 || scelta > treniSeguiti.size())
        {
            System.err.println("Scelta non valida.");
            pausa();
            return;
        }

        ViaggioController.TrenoSeguitoInfo trenoScelto = treniSeguiti.get(scelta - 1);

        System.out.print("Vuoi davvero smettere di seguire " + trenoScelto + "? (s/n): ");
        String conferma = scanner.nextLine().trim().toLowerCase();

        if (conferma.startsWith("s"))
        {
            if (viaggioController.smettiDiSeguireTreno(trenoScelto.getId()))
            {
                System.out.println("Non segui più il treno " + trenoScelto.getTipo() + ".");
            }
        }
        else
        {
            System.out.println("Operazione annullata.");
        }
        pausa();
    }


    private int leggiScelta()
    {
        try
        {
            return Integer.parseInt(scanner.nextLine().trim());
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    private int leggiIntero()
    {
        try
        {
            return Integer.parseInt(scanner.nextLine().trim());
        }
        catch (NumberFormatException e)
        {
            return 1; // Default
        }
    }

    private ClasseServizio scegliClasse(int scelta)
    {
        switch (scelta)
        {
            case 1: return ClasseServizio.ECONOMY;
            case 2: return ClasseServizio.BUSINESS;
            case 3: return ClasseServizio.LOW_COST;
            case 4: return ClasseServizio.FEDELTA;
            default: return ClasseServizio.ECONOMY;
        }
    }

    private TipoTreno scegliTipoTreno(int scelta)
    {
        switch (scelta)
        {
            case 1: return TipoTreno.ITALO;
            case 2: return TipoTreno.COMFORT;
            case 3: return TipoTreno.INTERCITY;
            default: return TipoTreno.COMFORT;
        }
    }

    private Calendar parseData(String dataStr)
    {
        try {
            String[] parti = dataStr.split("/");
            if (parti.length != 3) return null;

            int giorno = Integer.parseInt(parti[0]);
            int mese = Integer.parseInt(parti[1]) - 1; //Calendar usa 0-11 per mesi
            int anno = Integer.parseInt(parti[2]);

            Calendar cal = Calendar.getInstance();
            cal.set(anno, mese, giorno, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);

            return cal;
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatCalendar(Calendar cal)
    {
        if (cal == null) return "N/A";

        return cal.get(Calendar.DAY_OF_MONTH)+"/"+cal.get(Calendar.MONTH)+"/"+cal.get(Calendar.YEAR)
                +" "+cal.get(Calendar.HOUR)+":"+cal.get(Calendar.MINUTE);
    }

    private void pausa()
    {
        System.out.print("\nPremi INVIO per continuare...");
        scanner.nextLine();
    }

    public static void main(String[] args)
    {
        ClientCLI cli = new ClientCLI();
        cli.avvia();
    }
}

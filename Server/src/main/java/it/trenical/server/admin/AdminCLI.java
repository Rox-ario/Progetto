package it.trenical.server.admin;

import it.trenical.server.domain.Treno;
import it.trenical.server.domain.enumerations.TipoTreno;
import it.trenical.server.grpc.ControllerGRPC;
import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

public class AdminCLI
{
    private final ControllerGRPC controllerGRPC;
    private final Scanner scanner;
    private boolean running = true;

    //credenziali admin ( che in produzione andrebbero nel DB)
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "TreniCal2024!";

    public AdminCLI()
    {
        this.controllerGRPC = ControllerGRPC.getInstance();
        this.scanner = new Scanner(System.in);
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
        System.out.println("ore: "+ oggi.get(Calendar.HOUR)+":"+oggi.get(Calendar.MINUTE));
        System.out.println("=====================================");
    }

    private void mostraMenuPrincipale() {
        System.out.println("\nMENU PRINCIPALE");
        System.out.println("========================");
        System.out.println("1.Gestione Treni");
        System.out.println("2.Gestione Tratte");
        System.out.println("3.Gestione Viaggi");
        System.out.println("4.Gestione Biglietti");
        System.out.println("5.Gestione Promozioni");
        System.out.println("6.Gestione Clienti");
        System.out.println("7.Statistiche Sistema");
        System.out.println("8.Utilità Database");
        System.out.println("0.Esci");
        System.out.print("\nScelta: ");
    }


    private void gestisciScelta(int scelta)
    {
        switch (scelta) {
            case 1: menuTreni(); break;
            case 2: menuTratte(); break;
            case 3: menuViaggi(); break;
            case 4: menuBiglietti(); break;
            case 5: menuPromozioni(); break;
            case 6: menuClienti(); break;
            case 7: mostraStatistiche(); break;
            case 8: menuDatabase(); break;
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
            System.out.println("0. Torna indietro");
            System.out.print("\nScelta: ");

            int scelta = leggiScelta();

            switch (scelta)
            {
                case 1: aggiungiTreno(); break;
                case 2: visualizzaTuttiITreni(); break;
                case 3: cercaTreno(); break;
                case 0: return;
                default: System.out.println("Scelta non valida");
            }
        }
    }

    private void aggiungiTreno()
    {
        System.out.println("\nAGGIUNGI NUOVO TRENO");

        String id = scanner.nextLine().trim().toUpperCase();

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

        System.out.print("ID Tratta (es. MI-RM): ");
        String id = scanner.nextLine().trim();

        // Stazione partenza
        System.out.println("\nSTAZIONE DI PARTENZA");
        String idPartenza = scanner.nextLine();
        System.out.print("Città: ");
        String cittaPartenza = scanner.nextLine();
        System.out.print("Nome stazione: ");
        String nomePartenza = scanner.nextLine();
        System.out.print("Latitudine: ");
        double latPartenza = leggiDouble();
        System.out.print("Longitudine: ");
        double lonPartenza = leggiDouble();

        ArrayList<Integer> binariPartenza = new ArrayList<>();
        binariPartenza.add(1);
        binariPartenza.add(2);

        Stazione partenza = new Stazione(idPartenza, cittaPartenza, nomePartenza,
                binariPartenza, latPartenza, lonPartenza);

        // Stazione arrivo
        System.out.println("\n📍 STAZIONE DI ARRIVO");
        System.out.print("ID Stazione (es. RM01): ");
        String idArrivo = scanner.nextLine();
        System.out.print("Città: ");
        String cittaArrivo = scanner.nextLine();
        System.out.print("Nome stazione: ");
        String nomeArrivo = scanner.nextLine();
        System.out.print("Latitudine: ");
        double latArrivo = leggiDouble();
        System.out.print("Longitudine: ");
        double lonArrivo = leggiDouble();

        ArrayList<Integer> binariArrivo = new ArrayList<>();
        binariArrivo.add(1);
        binariArrivo.add(2);
        binariArrivo.add(3);

        Stazione arrivo = new Stazione(idArrivo, cittaArrivo, nomeArrivo,
                binariArrivo, latArrivo, lonArrivo);

        try {
            Tratta tratta = new Tratta(id, partenza, arrivo);
            controllerGRPC.aggiungiTratta(tratta);
            System.out.println("✅ Tratta " + id + " aggiunta con successo!");
        } catch (Exception e) {
            System.err.println("❌ Errore: " + e.getMessage());
        }
    }

    private void visualizzaTratte() {
        System.out.println("\n🛤️ ELENCO TRATTE DISPONIBILI");
        System.out.println("============================");

        // TODO: Aggiungere metodo getTratte() in ServerOperations
        System.out.println("Funzionalità in sviluppo...");
    }

    // ==================== GESTIONE VIAGGI ====================

    private void menuViaggi() {
        while (true) {
            System.out.println("\n📅 GESTIONE VIAGGI");
            System.out.println("==================");
            System.out.println("1. Programma nuovo viaggio");
            System.out.println("2. Aggiorna stato viaggio");
            System.out.println("3. Aggiungi ritardo");
            System.out.println("4. Visualizza viaggi del giorno");
            System.out.println("0. Torna indietro");
            System.out.print("\nScelta: ");

            int scelta = leggiScelta();

            switch (scelta) {
                case 1: programmaViaggio(); break;
                case 2: aggiornaStatoViaggio(); break;
                case 3: aggiungiRitardo(); break;
                case 4: visualizzaViaggiOggi(); break;
                case 0: return;
                default: System.out.println("⚠️ Scelta non valida");
            }
        }
    }

    private void programmaViaggio() {
        System.out.println("\n➕ PROGRAMMA NUOVO VIAGGIO");

        System.out.print("ID Treno: ");
        String idTreno = scanner.nextLine().trim();

        System.out.print("ID Tratta: ");
        String idTratta = scanner.nextLine().trim();

        System.out.print("Data partenza (dd/mm/yyyy): ");
        String dataStr = scanner.nextLine();

        System.out.print("Ora partenza (HH:mm): ");
        String oraPartenzaStr = scanner.nextLine();

        System.out.print("Ora arrivo (HH:mm): ");
        String oraArrivoStr = scanner.nextLine();

        try {
            Calendar partenza = parseDataOra(dataStr, oraPartenzaStr);
            Calendar arrivo = parseDataOra(dataStr, oraArrivoStr);

            controllerGRPC.programmaViaggio(idTreno, idTratta, partenza, arrivo);
            System.out.println("✅ Viaggio programmato con successo!");

        } catch (Exception e) {
            System.err.println("❌ Errore: " + e.getMessage());
        }
    }

    private void aggiornaStatoViaggio() {
        System.out.println("\n🔄 AGGIORNA STATO VIAGGIO");

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

        switch (statoScelta) {
            case 1: nuovoStato = StatoViaggio.PROGRAMMATO; break;
            case 2: nuovoStato = StatoViaggio.IN_CORSO; break;
            case 3: nuovoStato = StatoViaggio.TERMINATO; break;
            case 4: nuovoStato = StatoViaggio.IN_RITARDO; break;
            default:
                System.out.println("❌ Stato non valido");
                return;
        }

        try {
            controllerGRPC.aggiornaStatoViaggio(idViaggio, nuovoStato);
            System.out.println("✅ Stato aggiornato con successo!");
        } catch (Exception e) {
            System.err.println("❌ Errore: " + e.getMessage());
        }
    }

    private void aggiungiRitardo() {
        System.out.println("\n⏰ AGGIUNGI RITARDO");

        System.out.print("ID Viaggio: ");
        String idViaggio = scanner.nextLine().trim();

        System.out.print("Minuti di ritardo: ");
        int minuti = leggiIntero();

        // TODO: Aggiungere metodo aggiungiRitardo() in ServerOperations
        System.out.println("Funzionalità in sviluppo...");
    }

    private void visualizzaViaggiOggi() {
        System.out.println("\n📅 VIAGGI DI OGGI");
        System.out.println("==================");

        // TODO: Aggiungere metodo getViaggiOggi() in ServerOperations
        System.out.println("Funzionalità in sviluppo...");
    }

    // ==================== STATISTICHE ====================

    private void mostraStatistiche() {
        System.out.println(controllerGRPC.getStatisticheServizio());

        System.out.print("\nPremi INVIO per continuare...");
        scanner.nextLine();
    }

    // ==================== ALTRI MENU (PLACEHOLDER) ====================

    private void menuBiglietti() {
        System.out.println("\n🎫 GESTIONE BIGLIETTI");
        System.out.println("In sviluppo...");
        pausa();
    }

    private void menuPromozioni() {
        System.out.println("\n🎯 GESTIONE PROMOZIONI");
        System.out.println("In sviluppo...");
        pausa();
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

    // ==================== METODI UTILITY ====================

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

    private double leggiDouble() {
        try {
            return Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Calendar parseDataOra(String data, String ora) {
        try {
            String[] partiData = data.split("/");
            String[] partiOra = ora.split(":");

            int giorno = Integer.parseInt(partiData[0]);
            int mese = Integer.parseInt(partiData[1]) - 1;
            int anno = Integer.parseInt(partiData[2]);
            int ore = Integer.parseInt(partiOra[0]);
            int minuti = Integer.parseInt(partiOra[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(anno, mese, giorno, ore, minuti, 0);
            cal.set(Calendar.MILLISECOND, 0);

            return cal;
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato data/ora non valido");
        }
    }

    private void pausa() {
        System.out.print("\nPremi INVIO per continuare...");
        scanner.nextLine();
    }

    /**
     * Main per avviare la GUI admin
     */
    public static void main(String[] args) {
        AdminCLI admin = new AdminCLI();
        admin.avvia();
    }
}

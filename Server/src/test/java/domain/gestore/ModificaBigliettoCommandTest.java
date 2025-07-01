package domain.gestore;

import it.trenical.server.command.biglietto.AssegnaBiglietto;
import it.trenical.server.command.biglietto.ModificaBigliettoCommand;
import it.trenical.server.command.biglietto.PagaBiglietto;
import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.*;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.cliente.ClienteBanca;
import it.trenical.server.domain.enumerations.*;
import it.trenical.server.domain.gestore.*;
import it.trenical.server.dto.DatiBancariDTO;
import it.trenical.server.dto.ModificaBigliettoDTO;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ModificaBigliettoCommandTest {

    private GestoreBiglietti gestoreBiglietti;
    private GestoreViaggi gestoreViaggi;
    private GestoreClienti gestoreClienti;
    private GestoreBanca gestoreBanca;

    private Cliente clienteTest;
    private Viaggio viaggioTest;
    private String idTreno;
    private Tratta trattaTest;
    private Stazione stazionePartenza;
    private Stazione stazioneArrivo;

    private List<String> bigliettiDaPulire = new ArrayList<>();
    private List<String> viaggiDaPulire = new ArrayList<>();
    private List<String> clientiDaPulire = new ArrayList<>();

    @BeforeAll
    void setupAll() throws Exception {
        System.out.println("\n=== INIZIO TEST MODIFICA BIGLIETTO ===");

        pulisciDatabaseCompleto();

        gestoreBiglietti = GestoreBiglietti.getInstance();
        gestoreViaggi = GestoreViaggi.getInstance();
        gestoreClienti = GestoreClienti.getInstance();
        gestoreBanca = GestoreBanca.getInstance();

        //creo le stazioni di test
        ArrayList<Integer> binari = new ArrayList<>();
        binari.add(1);
        binari.add(2);
        binari.add(3);
        binari.add(4);
        binari.add(5);

        stazionePartenza = new Stazione("Roma", "Roma Termini TEST", binari, 41.9028, 12.4964);
        stazioneArrivo = new Stazione("Milano", "Milano Centrale TEST", binari, 45.4642, 9.1900);

        gestoreViaggi.aggiungiStazione(stazionePartenza);
        gestoreViaggi.aggiungiStazione(stazioneArrivo);

        trattaTest = new Tratta(stazionePartenza, stazioneArrivo);
        gestoreViaggi.aggiungiTratta(trattaTest);

        gestoreViaggi.aggiungiTreno("Treno1", TipoTreno.ITALO);

    }

    private void pulisciDatabaseCompleto() throws SQLException {
        Connection conn = null;
        try {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();

            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            stmt.execute("DELETE FROM biglietti");
            stmt.execute("DELETE FROM clienti_banca");
            stmt.execute("DELETE FROM viaggi");
            stmt.execute("DELETE FROM clienti");
            stmt.execute("DELETE FROM treni");
            stmt.execute("DELETE FROM tratte");
            stmt.execute("DELETE FROM stazioni");

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            System.out.println("Database pulito dai dati di test");

        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    @BeforeEach
    void setupEach()
    {
        long timestamp = System.currentTimeMillis();
        idTreno = "TR_TEST_" + timestamp;

        try {
            gestoreViaggi.aggiungiTreno(idTreno, TipoTreno.ITALO); //avrà quindi 100 LowCost, 70 economy, 50 business, 70 fedeltà
        } catch (Exception e) {
            idTreno = "TR_TEST_" + timestamp + "_ALT";
            gestoreViaggi.aggiungiTreno(idTreno, TipoTreno.ITALO);
        }

        String idCliente = "TEST_" + timestamp;
        clienteTest = new Cliente.Builder()
                .ID(idCliente)
                .Nome("Mario")
                .Cognome("Rossi")
                .Email("marioRossi" + timestamp + "@test.it")
                .Password("password123")
                .build();


        DatiBancariDTO datiBancari = new DatiBancariDTO(
                clienteTest.getId(),
                "Mario",
                "Rossi",
                "1234-5678-9012-3456"
        );
        gestoreClienti.aggiungiCliente(clienteTest, datiBancari);
        clientiDaPulire.add(clienteTest.getId());

        ClienteBanca clienteBanca = gestoreBanca.getClienteBanca(clienteTest.getId());
        System.out.println(clienteBanca);
        assertNotNull(clienteBanca, "Il cliente dovrebbe essere presente anche in clienti_banca");
        System.out.println("Cliente test creato con carta: " + clienteBanca.getNumeroCarta());

        //creo un viaggio di test per domani alle 14:00
        Calendar partenza = Calendar.getInstance();
        partenza.add(Calendar.DAY_OF_MONTH, 1);
        partenza.set(Calendar.HOUR_OF_DAY, 14);
        partenza.set(Calendar.MINUTE, 0);

        Calendar arrivo = Calendar.getInstance();
        arrivo.add(Calendar.DAY_OF_MONTH, 1);
        arrivo.set(Calendar.HOUR_OF_DAY, 17);
        arrivo.set(Calendar.MINUTE, 0);

        //programmo il viaggio
        viaggioTest = gestoreViaggi.programmaViaggio(idTreno, trattaTest.getId(), partenza, arrivo);
        viaggioTest.setStato(StatoViaggio.PROGRAMMATO);

        viaggiDaPulire.add(viaggioTest.getId());
    }

    @AfterEach
    void tearDown() throws SQLException
    {
        pulisciBigliettiDiTest();
        pulisciViaggiDiTest();
        pulisciBigliettiDiTest();

        bigliettiDaPulire.clear();
        viaggiDaPulire.clear();
        clientiDaPulire.clear();
    }

    private void rimuoviBigliettoDaDB(String idBiglietto) throws SQLException {
        String sql = "DELETE FROM biglietti WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, idBiglietto);
            pstmt.executeUpdate();
        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void rimuoviViaggioDaDB(String idViaggio) throws SQLException {
        String sql = "DELETE FROM viaggi WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, idViaggio);
            pstmt.executeUpdate();
        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Modifica senza penale - Upgrade dopo 7 giorni")
    void testModificaSenzaPenale() throws Exception
    {
        String idCliente = clienteTest.getId();
        String idViaggio = viaggioTest.getId();
        ClasseServizio classe = ClasseServizio.ECONOMY;

        Biglietto biglietto = gestoreBiglietti.creaBiglietto(idViaggio, idCliente, classe);
        bigliettiDaPulire.add(biglietto.getID());
        double prezzoOriginale = biglietto.getPrezzo();

        PagaBiglietto pagaBiglietto = new PagaBiglietto(biglietto.getID());
        pagaBiglietto.esegui();

        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.BUSINESS
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);

        command.esegui();

        //verifico nel database
        verificaClasseServizioNelDB(biglietto.getID(), ClasseServizio.BUSINESS);

        //verifico il prezzo
        Biglietto bigliettoModificato = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        assertTrue(bigliettoModificato.getPrezzo() > prezzoOriginale,
                "Il prezzo dovrebbe aumentare con l'upgrade");

        //verifico i posti disponibili
        assertEquals(70, viaggioTest.getPostiDisponibiliPerClasse(ClasseServizio.ECONOMY),
                "I posti Economy dovrebbero essere tornati disponibili");
        assertEquals(49, viaggioTest.getPostiDisponibiliPerClasse(ClasseServizio.BUSINESS),
                "I posti Business dovrebbero essere ridotti");

        System.out.println("Test modifica senza penale completato");
    }

    @Test
    @Order(2)
    @DisplayName("Modifica con penale 10% - Downgrade tra 3-7 giorni")
    void testModificaConPenale10Percento() throws Exception
    {
        Calendar partenza5Giorni = Calendar.getInstance();
        partenza5Giorni.add(Calendar.DAY_OF_MONTH, 5);
        creaViaggioCustom(partenza5Giorni);

        Biglietto biglietto = creaBigliettoPerTest(ClasseServizio.BUSINESS);
        double prezzoOriginale = biglietto.getPrezzoBiglietto();
        double saldoIniziale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        System.out.println("Prezzo pagato del biglietto= "+prezzoOriginale+"\n"+
                "Saldo cliente dopo pagamento= "+ saldoIniziale);

        //modifica a Economy (downgrade)
        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.ECONOMY
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);
        command.esegui();

        //verifica del rimborso
        Biglietto bigliettoModificato = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        double differenzaTariffaria = bigliettoModificato.getPrezzoBiglietto() - prezzoOriginale;
        double penale = CalcolatorePenali.calcolaPenale(Calendar.getInstance(), gestoreViaggi.getViaggio(bigliettoModificato.getIDViaggio()).getInizioReale(), differenzaTariffaria);
        double importo = 0;
        if (differenzaTariffaria < 0)  //il prezzo originale è maggiore del prezzo nuovo, quindi passo da una classe superiore a una inferiore il prezzo deve diminuire
        {
            double moltiplicatorePenale = CalcolatorePenali.calcolaPenaleDowngrade(ClasseServizio.BUSINESS, ClasseServizio.ECONOMY);
            penale *= moltiplicatorePenale;
        }

        if (differenzaTariffaria > 0)
        {
            //Il nuovo biglietto costa di più: addebita la differenza
            importo = differenzaTariffaria;
        }
        else if (differenzaTariffaria < 0)
        {
            //Il nuovo biglietto costa meno: calcola il rimborso al netto della penale
            double rimborsoLordo = Math.abs(differenzaTariffaria);
            double rimborsoNetto = rimborsoLordo - penale;
            if (rimborsoNetto > 0)
            {
                importo = rimborsoNetto;
            }
            else
            {
                importo = Math.abs(rimborsoNetto);
            }
        }


        double saldoFinale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        assertEquals(saldoIniziale + importo, saldoFinale, 0.01,
                "Il rimborso dovrebbe essere la differenza meno il 10% di penale");

        System.out.println("Test modifica con penale 10% completato");
    }

    @Test
    @Order(3)
    @DisplayName("Modifica con penale 25% - Downgrade tra 1-3 giorni")
    void testModificaConPenale25Percento() throws Exception
    {
        Calendar partenza2Giorni = Calendar.getInstance();
        partenza2Giorni.add(Calendar.DAY_OF_MONTH, 2);
        creaViaggioCustom(partenza2Giorni);

        Biglietto biglietto = creaBigliettoPerTest(ClasseServizio.BUSINESS);
        double prezzoOriginale = biglietto.getPrezzoBiglietto();
        double saldoIniziale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        System.out.println("Prezzo pagato del biglietto= "+prezzoOriginale+"\n"+
                "Saldo cliente dopo pagamento= "+ saldoIniziale);

        //modifica a Economy (downgrade)
        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.ECONOMY
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);
        command.esegui();

        //verifica del rimborso
        Biglietto bigliettoModificato = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        double differenzaTariffaria = bigliettoModificato.getPrezzoBiglietto() - prezzoOriginale;
        double penale = CalcolatorePenali.calcolaPenale(Calendar.getInstance(), gestoreViaggi.getViaggio(bigliettoModificato.getIDViaggio()).getInizioReale(), differenzaTariffaria);
        double importo = 0;
        if (differenzaTariffaria < 0)  //il prezzo originale è maggiore del prezzo nuovo, quindi passo da una classe superiore a una inferiore il prezzo deve diminuire
        {
            double moltiplicatorePenale = CalcolatorePenali.calcolaPenaleDowngrade(ClasseServizio.BUSINESS, ClasseServizio.ECONOMY);
            penale *= moltiplicatorePenale;
        }

        if (differenzaTariffaria > 0)
        {
            //Il nuovo biglietto costa di più: addebita la differenza
            importo = differenzaTariffaria;
        }
        else if (differenzaTariffaria < 0)
        {
            //Il nuovo biglietto costa meno: calcola il rimborso al netto della penale
            double rimborsoLordo = Math.abs(differenzaTariffaria);
            double rimborsoNetto = rimborsoLordo - penale;
            if (rimborsoNetto > 0)
            {
                importo = rimborsoNetto;
            }
            else
            {
                importo = Math.abs(rimborsoNetto);
            }
        }


        double saldoFinale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        assertEquals(saldoIniziale + importo, saldoFinale, 0.01,
                "Il rimborso dovrebbe essere la differenza meno il 25% di penale");

        System.out.println("Test modifica con penale 25% completato");
    }

    @Test
    @Order(4)
    @DisplayName("Modifica con penale 50% - Downgrade meno di 24 ore")
    void testModificaConPenale50Percento() throws Exception
    {
        Calendar partenza12Ore = Calendar.getInstance();
        partenza12Ore.add(Calendar.HOUR, 12);
        creaViaggioCustom(partenza12Ore);

        Biglietto biglietto = creaBigliettoPerTest(ClasseServizio.BUSINESS);
        double prezzoOriginale = biglietto.getPrezzoBiglietto();
        double saldoIniziale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        System.out.println("Prezzo pagato del biglietto= "+prezzoOriginale+"\n"+
                "Saldo cliente dopo pagamento= "+ saldoIniziale);

        //modifica a Economy (downgrade)
        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.ECONOMY
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);
        command.esegui();

        //verifica del rimborso
        Biglietto bigliettoModificato = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        double differenzaTariffaria = bigliettoModificato.getPrezzoBiglietto() - prezzoOriginale;
        double penale = CalcolatorePenali.calcolaPenale(Calendar.getInstance(), gestoreViaggi.getViaggio(bigliettoModificato.getIDViaggio()).getInizioReale(), differenzaTariffaria);
        double importo = 0;
        if (differenzaTariffaria < 0)  //il prezzo originale è maggiore del prezzo nuovo, quindi passo da una classe superiore a una inferiore il prezzo deve diminuire
        {
            double moltiplicatorePenale = CalcolatorePenali.calcolaPenaleDowngrade(ClasseServizio.BUSINESS, ClasseServizio.ECONOMY);
            penale *= moltiplicatorePenale;
        }

        if (differenzaTariffaria > 0)
        {
            //Il nuovo biglietto costa di più: addebita la differenza
            importo = differenzaTariffaria;
        }
        else if (differenzaTariffaria < 0)
        {
            //Il nuovo biglietto costa meno: calcola il rimborso al netto della penale
            double rimborsoLordo = Math.abs(differenzaTariffaria);
            double rimborsoNetto = rimborsoLordo - penale;
            if (rimborsoNetto > 0)
            {
                importo = rimborsoNetto;
            }
            else
            {
                importo = Math.abs(rimborsoNetto);
            }
        }


        double saldoFinale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        assertEquals(saldoIniziale + importo, saldoFinale, 0.01,
                "Il rimborso dovrebbe essere la differenza meno il 50% di penale");

        System.out.println("Test modifica con penale 50% completato");
    }

    @Test
    @Order(5)
    @DisplayName("Modifica con penale extra per downgrade Business->LowCost")
    void testModificaConPenaleDowngradeExtra() throws Exception
    {
        Calendar partenza5Giorni = Calendar.getInstance();
        partenza5Giorni.add(Calendar.DAY_OF_MONTH, 5);
        creaViaggioCustom(partenza5Giorni);

        Biglietto biglietto = creaBigliettoPerTest(ClasseServizio.BUSINESS);
        double prezzoOriginale = biglietto.getPrezzoBiglietto();
        double saldoIniziale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        System.out.println("Prezzo pagato del biglietto= "+prezzoOriginale+"\n"+
                "Saldo cliente dopo pagamento= "+ saldoIniziale);

        //modifica a Economy (downgrade)
        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.ECONOMY
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);
        command.esegui();

        //verifica del rimborso
        Biglietto bigliettoModificato = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        double differenzaTariffaria = bigliettoModificato.getPrezzoBiglietto() - prezzoOriginale;
        double penale = CalcolatorePenali.calcolaPenale(Calendar.getInstance(), gestoreViaggi.getViaggio(bigliettoModificato.getIDViaggio()).getInizioReale(), differenzaTariffaria);
        double importo = 0;
        if (differenzaTariffaria < 0)  //il prezzo originale è maggiore del prezzo nuovo, quindi passo da una classe superiore a una inferiore il prezzo deve diminuire
        {
            double moltiplicatorePenale = CalcolatorePenali.calcolaPenaleDowngrade(ClasseServizio.BUSINESS, ClasseServizio.ECONOMY);
            penale *= moltiplicatorePenale;
        }

        if (differenzaTariffaria > 0)
        {
            //Il nuovo biglietto costa di più: addebita la differenza
            importo = differenzaTariffaria;
        }
        else if (differenzaTariffaria < 0)
        {
            //Il nuovo biglietto costa meno: calcola il rimborso al netto della penale
            double rimborsoLordo = Math.abs(differenzaTariffaria);
            double rimborsoNetto = rimborsoLordo - penale;
            if (rimborsoNetto > 0)
            {
                importo = rimborsoNetto;
            }
            else
            {
                importo = Math.abs(rimborsoNetto);
            }
        }


        double saldoFinale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        assertEquals(saldoIniziale + importo, saldoFinale, 0.01,
                "Il rimborso dovrebbe includere la penale extra per downgrade drastico");
    }

    @Test
    @Order(6)
    @DisplayName("Modifica biglietto non esistente")
    void testModificaBigliettoNonEsistente ()
    {
        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                "BIGLIETTO_INESISTENTE",
                ClasseServizio.BUSINESS
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);

        Exception exception = assertThrows(IllegalArgumentException.class, command::esegui);

        assertTrue(exception.getMessage().contains("non esiste"),
                "Dovrebbe lanciare eccezione per biglietto non esistente");

        System.out.println("✓ Test biglietto non esistente completato");
    }

    @Test
    @Order(7)
    @DisplayName("Modifica con posti non disponibili")
    void testModificaPostiNonDisponibili () throws Exception
    {
        // Occupa tutti i posti Business
        for (int i = 0; i < 50; i++) {
            AssegnaBiglietto cmd = new AssegnaBiglietto(
                    viaggioTest.getId(),
                    clienteTest.getId(),
                    ClasseServizio.BUSINESS
            );
            cmd.esegui();
            bigliettiDaPulire.add(cmd.getBiglietto().getID());
        }

        Biglietto biglietto = creaBigliettoPerTest(ClasseServizio.ECONOMY);

        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.BUSINESS
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                command::esegui,
                "Dovrebbe impedire la modifica se non ci sono più posti"
        );
        assertTrue(
                exception.getMessage().toLowerCase().contains("non ci sono posti disponibili"),
                "Il messaggio d'errore deve dire che i posti non sono disponibili");

        System.out.println("Test posti non disponibili completato");
    }

    @Test
    @Order(8)
    @DisplayName("Modifica con pagamento fallito e rollback")
    void testModificaPagamentoFallitoRollback() throws Exception
    {
        //svuoto il conto del cliente
        GestoreBanca bancaManager = GestoreBanca.getInstance();
        System.out.println("Saldo cliente prima: "+ bancaManager.getClienteBanca(clienteTest.getId()).getSaldo());
        double saldoOriginale = bancaManager.getClienteBanca(clienteTest.getId()).getSaldo();
        boolean pagato = bancaManager.eseguiPagamento(clienteTest.getId(), saldoOriginale);
        System.out.println("Saldo cliente dopo: "+ bancaManager.getClienteBanca(clienteTest.getId()).getSaldo());
        assertTrue(pagato, "Non sono riuscito a svuotare il conto in DB");

        Biglietto biglietto = creaBigliettoPerTest(ClasseServizio.ECONOMY);
        System.out.println("Il prezzo del biglietto è: "+ biglietto.getPrezzoBiglietto());
        ClasseServizio classeOriginale = biglietto.getClasseServizio();
        int postiEconomyPrima = viaggioTest.getPostiDisponibiliPerClasse(ClasseServizio.ECONOMY);
        System.out.println("Posti disponibili Economy dopo aver assegnato il biglietto= "+ postiEconomyPrima);
        int postiBusinessPrima = viaggioTest.getPostiDisponibiliPerClasse(ClasseServizio.BUSINESS);
        System.out.println("Posti disponibili Business = "+ postiBusinessPrima);

        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.BUSINESS
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                command::esegui,
                "Dovrebbe fallire per pagamento non riuscito"
        );

        assertTrue(
                exception.getMessage().contains(
                        "Pagamento della differenza tariffaria fallito. Modifica annullata."
                ),
                "Messaggio d'errore sbagliato"
        );

        Biglietto dopo = GestoreBiglietti.getInstance().getBigliettoPerID(biglietto.getID());
        assertEquals(classeOriginale, dopo.getClasseServizio(),
                "La classe dovrebbe essere rimasta la stessa");

        assertEquals(postiEconomyPrima,
                viaggioTest.getPostiDisponibiliPerClasse(ClasseServizio.ECONOMY),
                "Posti Economy non ripristinati");
        assertEquals(postiBusinessPrima,
                viaggioTest.getPostiDisponibiliPerClasse(ClasseServizio.BUSINESS),
                "Posti Business non ripristinati");

        System.out.println("Test rollback per pagamento fallito completato");
    }

    @Test
    @Order(9)
    @DisplayName("Modifica con penale che supera il rimborso")
    void testModificaPenaleSuperaRimborso () throws Exception
    {
        Calendar partenza12Ore = Calendar.getInstance();
        partenza12Ore.add(Calendar.HOUR, 12);
        creaViaggioCustom(partenza12Ore);

        Biglietto biglietto = creaBigliettoPerTest(ClasseServizio.BUSINESS);

        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.ECONOMY
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);

        assertDoesNotThrow(command::esegui);
    }

    private Biglietto creaBigliettoPerTest (ClasseServizio classe) throws Exception
    {
        AssegnaBiglietto comando = new AssegnaBiglietto(
                viaggioTest.getId(),
                clienteTest.getId(),
                classe
        );
        comando.esegui();

        String idBiglietto = comando.getBiglietto().getID();
        bigliettiDaPulire.add(idBiglietto);

        //simulo solo il pagamento
        Biglietto biglietto = gestoreBiglietti.getBigliettoPerID(idBiglietto);
        double prezzoPagato = biglietto.getPrezzoBiglietto();
        biglietto.setStatoBiglietto(StatoBiglietto.PAGATO);
        GestoreBanca.getInstance().eseguiPagamento(biglietto.getIDCliente(), prezzoPagato);

        return biglietto;
    }

    private void creaViaggioCustom (Calendar partenza)
    {
        if (viaggioTest != null)
        {
            viaggiDaPulire.remove(viaggioTest.getId());
            try
            {
                gestoreViaggi.rimuoviViaggio(viaggioTest.getId());
            }
            catch (Exception e)
            {
                // Ignora se non esiste
            }
        }

        Calendar arrivo = (Calendar) partenza.clone();
        arrivo.add(Calendar.HOUR, 2);

        viaggioTest = gestoreViaggi.programmaViaggio(idTreno, trattaTest.getId(), partenza, arrivo);
        viaggiDaPulire.add(viaggioTest.getId());
    }

    private void pulisciBigliettiDiTest ()
    {
        if (bigliettiDaPulire.isEmpty()) return;

        String sql = "DELETE FROM biglietti WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            for (String id : bigliettiDaPulire) {
                try {
                    pstmt.setString(1, id);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    // Ignora errori singoli
                }
            }

            System.out.println("Puliti " + bigliettiDaPulire.size() + " biglietti di test");

        } catch (SQLException e) {
            System.err.println("Errore pulizia biglietti: " + e.getMessage());
        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void pulisciViaggiDiTest ()
    {
        if (viaggiDaPulire.isEmpty()) return;

        String sql = "DELETE FROM viaggi WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            for (String id : viaggiDaPulire) {
                try {
                    pstmt.setString(1, id);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    // Ignora errori singoli
                }
            }

        } catch (SQLException e) {
            System.err.println("Errore pulizia viaggi: " + e.getMessage());
        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void pulisciClientiDiTest ()
    {
        if (clientiDaPulire.isEmpty()) return;

        // Prima pulisci clienti_banca
        String deleteClientiBanca = "DELETE FROM clienti_banca WHERE cliente_id = ?";
        String deleteClienti = "DELETE FROM clienti WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();

            // Pulisci clienti_banca
            PreparedStatement pstmtBanca = conn.prepareStatement(deleteClientiBanca);
            for (String id : clientiDaPulire) {
                pstmtBanca.setString(1, id);
                pstmtBanca.executeUpdate();
            }

            // Poi pulisci clienti
            PreparedStatement pstmtClienti = conn.prepareStatement(deleteClienti);
            for (String id : clientiDaPulire) {
                pstmtClienti.setString(1, id);
                pstmtClienti.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("Errore pulizia clienti: " + e.getMessage());
        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void verificaClasseServizioNelDB(String idBiglietto, ClasseServizio classeAttesa) throws SQLException
    {
        String sql = "SELECT classe_servizio FROM biglietti WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, idBiglietto);

            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "Il biglietto dovrebbe esistere nel DB");
            assertEquals(classeAttesa.name(), rs.getString("classe_servizio"),
                    "La classe di servizio nel DB dovrebbe essere aggiornata");

        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }
}
package domain.gestore;

import it.trenical.server.command.biglietto.AssegnaBiglietto;
import it.trenical.server.command.biglietto.ModificaBigliettoCommand;
import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.*;
import it.trenical.server.domain.cliente.Cliente;
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
    private Treno trenoTest;
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

        stazionePartenza = new Stazione("Roma", "Roma Termini TEST", binari, 41.9028, 12.4964);
        stazioneArrivo = new Stazione("Milano", "Milano Centrale TEST", binari, 45.4642, 9.1900);

        gestoreViaggi.aggiungiStazione(stazionePartenza);
        gestoreViaggi.aggiungiStazione(stazioneArrivo);

        // Crea la tratta
        trattaTest = new Tratta(stazionePartenza, stazioneArrivo);
        gestoreViaggi.aggiungiTratta(trattaTest);

        gestoreViaggi.aggiungiTreno("T_MOD_001", TipoTreno.ITALO);

        //il cliente di test con saldo sufficiente
        String idCliente = "CLI_MOD_" + System.currentTimeMillis();
        clienteTest = new Cliente.Builder()
                .ID(idCliente)
                .Nome("Mario")
                .Cognome("Rossi")
                .Email("mario.test@test.it")
                .Password("password123")
                .isFedelta(true)
                .build();

        DatiBancariDTO datiBancari = new DatiBancariDTO(
                idCliente,
                "Mario",
                "Rossi",
                "1234-5678-9012-3456"
        );

        gestoreClienti.aggiungiCliente(clienteTest, datiBancari);
        clientiDaPulire.add(idCliente);
    }

    private void pulisciDatabaseCompleto() throws SQLException {
        Connection conn = null;
        try {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();

            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            stmt.execute("DELETE FROM biglietti WHERE cliente_id LIKE 'TEST_%' OR viaggio_id IN " +
                    "(SELECT id FROM viaggi WHERE treno_id LIKE 'TR_TEST_%')");
            stmt.execute("DELETE FROM viaggi WHERE treno_id LIKE 'TR_TEST_%'");
            stmt.execute("DELETE FROM clienti_banca WHERE cliente_id LIKE 'TEST_%'");
            stmt.execute("DELETE FROM clienti WHERE id LIKE 'TEST_%'");
            stmt.execute("DELETE FROM treni WHERE id LIKE 'TR_TEST_%'");
            stmt.execute("DELETE FROM tratte WHERE stazione_partenza_id IN " +
                    "(SELECT id FROM stazioni WHERE nome LIKE '%TEST%')");
            stmt.execute("DELETE FROM stazioni WHERE nome LIKE '%TEST%'");

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            System.out.println("Database pulito dai dati di test");

        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    @BeforeEach
    void setupEach()
    {
        //creo un nuovo viaggio per ogni test
        Calendar partenza = Calendar.getInstance();
        partenza.add(Calendar.DAY_OF_MONTH, 10); //Viaggio tra 10 giorni

        Calendar arrivo = (Calendar) partenza.clone();
        arrivo.add(Calendar.HOUR, 2); //Durata 2 ore

        Viaggio v = gestoreViaggi.programmaViaggio(trenoTest.getID(), trattaTest.getId(), partenza, arrivo);
        viaggiDaPulire.add(v.getId());
    }

    @AfterEach
    void tearDown() throws SQLException {
        for (String idBiglietto : bigliettiDaPulire) {
            rimuoviBigliettoDaDB(idBiglietto);
        }

        for (String idViaggio : viaggiDaPulire) {
            rimuoviViaggioDaDB(idViaggio);
        }

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
        assertEquals(299, viaggioTest.getPostiDisponibiliPerClasse(ClasseServizio.ECONOMY),
                "I posti Economy dovrebbero essere tornati disponibili");
        assertEquals(299, viaggioTest.getPostiDisponibiliPerClasse(ClasseServizio.BUSINESS),
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
        double prezzoOriginale = biglietto.getPrezzo();
        double saldoIniziale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();

        //modifica a Economy (downgrade)
        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.ECONOMY
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);
        command.esegui();

        //verifica del rimborso
        Biglietto bigliettoModificato = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        double differenza = prezzoOriginale - bigliettoModificato.getPrezzo();
        double penaleAttesa = differenza * 0.10;
        double rimborsoAtteso = differenza - penaleAttesa;

        double saldoFinale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        assertEquals(saldoIniziale + rimborsoAtteso, saldoFinale, 0.01,
                "Il rimborso dovrebbe essere la differenza meno il 10% di penale");

        System.out.println("Test modifica con penale 10% completato");
        System.out.println("  - Differenza: €" + String.format("%.2f", differenza));
        System.out.println("  - Penale: €" + String.format("%.2f", penaleAttesa));
        System.out.println("  - Rimborso netto: €" + String.format("%.2f", rimborsoAtteso));
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
        double prezzoOriginale = biglietto.getPrezzo();
        double saldoIniziale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();

        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.ECONOMY
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);
        command.esegui();

        //verifico il rimborso con penale 25%
        Biglietto bigliettoModificato = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        double differenza = prezzoOriginale - bigliettoModificato.getPrezzo();
        double penaleAttesa = differenza * 0.25;
        double rimborsoAtteso = differenza - penaleAttesa;

        double saldoFinale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        assertEquals(saldoIniziale + rimborsoAtteso, saldoFinale, 0.01,
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
        double prezzoOriginale = biglietto.getPrezzo();
        double saldoIniziale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();

        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.ECONOMY
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);
        command.esegui();

        //verifico il rimborso con penale 50%
        Biglietto bigliettoModificato = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        double differenza = prezzoOriginale - bigliettoModificato.getPrezzo();
        double penaleAttesa = differenza * 0.50;
        double rimborsoAtteso = differenza - penaleAttesa;

        double saldoFinale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        assertEquals(saldoIniziale + rimborsoAtteso, saldoFinale, 0.01,
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
        double prezzoOriginale = biglietto.getPrezzo();
        double saldoIniziale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();

        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.LOW_COST
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);
        command.esegui();

        //verifico il rimborso con penale 10% * moltiplicatore downgrade (che è 1.2)
        Biglietto bigliettoModificato = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        double differenza = prezzoOriginale - bigliettoModificato.getPrezzo();
        double penaleBase = differenza * 0.10;
        double penaleConMoltiplicatore = penaleBase * 1.2;
        double rimborsoAtteso = differenza - penaleConMoltiplicatore;

        double saldoFinale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        assertEquals(saldoIniziale + rimborsoAtteso, saldoFinale, 0.01,
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
    @DisplayName("Modifica viaggio già partito")
    void testModificaViaggioGiaPartito () throws Exception
    {
        Calendar partenzaPassata = Calendar.getInstance();
        partenzaPassata.add(Calendar.HOUR, -1); // 1 ora fa
        creaViaggioCustom(partenzaPassata);

        Biglietto biglietto = creaBigliettoPerTest(ClasseServizio.ECONOMY);

        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.BUSINESS
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);

        Exception exception = assertThrows(IllegalArgumentException.class, command::esegui);

            assertTrue(exception.getMessage().contains("già partito"),
                    "Dovrebbe impedire la modifica se il treno è già partito");

            System.out.println("Test viaggio già partito completato");
        }

    @Test
    @Order(8)
    @DisplayName("Modifica con posti non disponibili")
    void testModificaPostiNonDisponibili () throws Exception
    {
        // Occupa tutti i posti Business
        for (int i = 0; i < 300; i++) {
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

        Exception exception = assertThrows(IllegalArgumentException.class, command::esegui);

        assertTrue(exception.getMessage().contains("non ci sono posti disponibili"),
                "Dovrebbe impedire la modifica se non ci sono posti");

        System.out.println("Test posti non disponibili completato");
    }

    @Test
    @Order(9)
    @DisplayName("Modifica con pagamento fallito e rollback")
    void testModificaPagamentoFallitoRollback () throws Exception
    {
        //riduco il saldo del cliente per far fallire il pagamento
        double saldoOriginale = gestoreBanca.getClienteBanca(clienteTest.getId()).getSaldo();
        gestoreBanca.getClienteBanca(clienteTest.getId()).addebita(saldoOriginale);

        Biglietto biglietto = creaBigliettoPerTest(ClasseServizio.ECONOMY);
        ClasseServizio classeOriginale = biglietto.getClasseServizio();
        int postiEconomyPrima = viaggioTest.getPostiDisponibiliPerClasse(ClasseServizio.ECONOMY);
        int postiBusinessPrima = viaggioTest.getPostiDisponibiliPerClasse(ClasseServizio.BUSINESS);

        ModificaBigliettoDTO dto = new ModificaBigliettoDTO(
                biglietto.getID(),
                ClasseServizio.BUSINESS
        );
        ModificaBigliettoCommand command = new ModificaBigliettoCommand(dto);

        Exception exception = assertThrows(IllegalStateException.class, command::esegui);

        assertTrue(exception.getMessage().contains("Pagamento"),
                "Dovrebbe fallire per pagamento non riuscito");

        Biglietto bigliettoPost = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        assertEquals(classeOriginale, bigliettoPost.getClasseServizio(),
                "La classe dovrebbe tornare quella originale");

        assertEquals(postiEconomyPrima, viaggioTest.getPostiDisponibiliPerClasse(ClasseServizio.ECONOMY),
                "I posti Economy dovrebbero essere ripristinati");
        assertEquals(postiBusinessPrima, viaggioTest.getPostiDisponibiliPerClasse(ClasseServizio.BUSINESS),
                "I posti Business dovrebbero essere ripristinati");

        System.out.println("Test rollback per pagamento fallito completato");
    }

    @Test
    @Order(10)
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
        biglietto.setStatoBiglietto(StatoBiglietto.PAGATO);

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

        Viaggio v = gestoreViaggi.programmaViaggio(trenoTest.getID(), trattaTest.getId(), partenza, arrivo);
        viaggiDaPulire.add(v.getId());
    }


    // ==================== METODI DI PULIZIA ====================

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
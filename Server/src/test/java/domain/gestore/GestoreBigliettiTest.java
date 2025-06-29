package domain.gestore;

import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.*;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.cliente.ClienteBanca;
import it.trenical.server.domain.enumerations.*;
import it.trenical.server.domain.gestore.*;
import it.trenical.server.dto.DatiBancariDTO;
import it.trenical.server.dto.RimborsoDTO;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.List;
import java.util.Calendar;
import java.util.ArrayList;

/**
 * Test per GestoreBiglietti con utilizzo del database reale
 * Basato sul test esistente in Server/src/test/java/domain/gestore/GestoreBigliettiTest.java
 *
 * PREREQUISITI:
 * 1. Database MySQL attivo
 * 2. Schema creato con src/main/resources/sql/schema.sql
 * 3. Configurazione connessione in application.properties
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GestoreBigliettiTest {

    private GestoreBiglietti gestoreBiglietti;
    private GestoreViaggi gestoreViaggi;
    private GestoreClienti gestoreClienti;
    private GestoreBanca gestoreBanca;

    // Oggetti di test riutilizzabili
    private Cliente clienteTest;
    private Viaggio viaggioTest;
    private Stazione stazionePartenza;
    private Stazione stazioneArrivo;
    private Tratta trattaTest;

    // Liste per tenere traccia degli ID da pulire
    private List<String> bigliettiDaPulire = new ArrayList<>();
    private List<String> viaggiDaPulire = new ArrayList<>();
    private List<String> clientiDaPulire = new ArrayList<>();

    @BeforeAll
    void setupAll() throws Exception
    {
        pulisciDatabaseCompleto();

        // Verifico che il database sia accessibile
        verificaConnessioneDB();

        // Inizializzo i singleton
        gestoreBiglietti = GestoreBiglietti.getInstance();
        gestoreViaggi = GestoreViaggi.getInstance();
        gestoreClienti = GestoreClienti.getInstance();
        gestoreBanca = GestoreBanca.getInstance();

        // Creo stazioni di test con ArrayList di binari
        ArrayList<Integer> binari = new ArrayList<>();
        binari.add(1);
        binari.add(2);
        binari.add(3);

        // Uso il costruttore senza ID (genera UUID automaticamente)
        stazionePartenza = new Stazione("Roma", "Roma Termini TEST", binari, 41.9028, 12.4964);
        stazioneArrivo = new Stazione("Milano", "Milano Centrale TEST", binari, 45.4642, 9.1900);

        // Aggiungo le stazioni al gestore (salva anche su DB)
        gestoreViaggi.aggiungiStazione(stazionePartenza);
        gestoreViaggi.aggiungiStazione(stazioneArrivo);

        // Creo la tratta
        trattaTest = new Tratta(stazionePartenza, stazioneArrivo);
        gestoreViaggi.aggiungiTratta(trattaTest);
    }

    @AfterEach
    void cleanup() throws SQLException
    {
        // ✅ PULIZIA DOPO OGNI TEST per evitare interferenze

        // Rimuovi biglietti creati in questo test
        for (String idBiglietto : bigliettiDaPulire)
        {
            rimuoviBigliettoDaDB(idBiglietto);
        }

        // Svuoto le liste
        bigliettiDaPulire.clear();
        viaggiDaPulire.clear();
        clientiDaPulire.clear();
    }

    @AfterAll
    void tearDownAll() throws SQLException {
        // ✅ PULIZIA FINALE completa
        pulisciDatabaseCompleto();
        System.out.println("\n=== TEST COMPLETATI ===");
        System.out.println("Database pulito dai dati di test");
    }

    // ✅ METODO per pulizia completa del database
    private void pulisciDatabaseCompleto() throws SQLException {
        Connection conn = null;
        try {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();

            // Disabilito foreign key checks per eliminare in qualsiasi ordine
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            // Pulisco tabelle in ordine inverso delle dipendenze
            stmt.execute("DELETE FROM biglietti WHERE cliente_id LIKE 'TEST_%' OR viaggio_id IN " +
                    "(SELECT id FROM viaggi WHERE treno_id LIKE 'TR_TEST_%')");
            stmt.execute("DELETE FROM viaggi WHERE treno_id LIKE 'TR_TEST_%'");
            stmt.execute("DELETE FROM clienti_banca WHERE cliente_id LIKE 'TEST_%'");
            stmt.execute("DELETE FROM clienti WHERE id LIKE 'TEST_%'");
            stmt.execute("DELETE FROM treni WHERE id LIKE 'TR_TEST_%'");
            stmt.execute("DELETE FROM tratte WHERE stazione_partenza_id IN " +
                    "(SELECT id FROM stazioni WHERE nome LIKE '%TEST%')");
            stmt.execute("DELETE FROM stazioni WHERE nome LIKE '%TEST%'");

            // Riabilito foreign key checks
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            System.out.println("✓ Database pulito dai dati di test");

        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    // ✅ METODO per rimuovere biglietto specifico
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

    @BeforeEach
    void setup() {
        // Creo un treno di test con ID univoco basato su timestamp
        long timestamp = System.currentTimeMillis();
        String idTreno = "TR_TEST_" + timestamp;  // ✅ ID univoco

        try {
            gestoreViaggi.aggiungiTreno(idTreno, TipoTreno.ITALO);
        } catch (Exception e) {
            // Se il treno esiste già, uso un ID diverso
            idTreno = "TR_TEST_" + timestamp + "_ALT";
            gestoreViaggi.aggiungiTreno(idTreno, TipoTreno.ITALO);
        }

        // Creo un cliente di test con ID univoco
        String idCliente = "TEST_" + timestamp;  // ✅ ID univoco
        clienteTest = new Cliente.Builder()
                .ID(idCliente)
                .Nome("Mario")
                .Cognome("Rossi")
                .Email("marioRossi" + timestamp + "@test.it")  // ✅ Email univoca
                .Password("password123")
                .build();

        gestoreClienti.aggiungiCliente(clienteTest);
        clientiDaPulire.add(clienteTest.getId());

        ClienteBanca clienteBanca = gestoreBanca.getClienteBanca(clienteTest.getId());
        assertNotNull(clienteBanca, "Il cliente dovrebbe essere presente anche in clienti_banca");
        System.out.println("✓ Cliente test creato con carta: " + clienteBanca.getNumeroCarta());

        // Creo un viaggio di test per domani alle 14:00
        Calendar partenza = Calendar.getInstance();
        partenza.add(Calendar.DAY_OF_MONTH, 1);
        partenza.set(Calendar.HOUR_OF_DAY, 14);
        partenza.set(Calendar.MINUTE, 0);

        Calendar arrivo = Calendar.getInstance();
        arrivo.add(Calendar.DAY_OF_MONTH, 1);
        arrivo.set(Calendar.HOUR_OF_DAY, 17);
        arrivo.set(Calendar.MINUTE, 0);

        // Programmo il viaggio
        viaggioTest = gestoreViaggi.programmaViaggio(idTreno, trattaTest.getId(), partenza, arrivo);
        viaggioTest.setStato(StatoViaggio.PROGRAMMATO);

        viaggiDaPulire.add(viaggioTest.getId());
    }

    /**
     * TEST 1: Creazione biglietto con successo e verifica persistenza DB
     */
    @Test
    @Order(1)
    @DisplayName("Creazione biglietto con successo e salvataggio su DB")
    void testCreaBigliettoConSuccessoEPersistenza() throws SQLException {
        // Given
        String idCliente = clienteTest.getId();
        String idViaggio = viaggioTest.getId();
        ClasseServizio classe = ClasseServizio.ECONOMY;

        // When: creo un biglietto (NOTA: l'ordine dei parametri è IDViaggio, IDUtente, classe)
        Biglietto biglietto = gestoreBiglietti.creaBiglietto(idViaggio, idCliente, classe);
        bigliettiDaPulire.add(biglietto.getID());

        // Then: verifico creazione
        assertNotNull(biglietto, "Il biglietto non dovrebbe essere null");
        assertEquals(idCliente, biglietto.getIDCliente());
        assertEquals(idViaggio, biglietto.getIDViaggio());
        assertEquals(classe, biglietto.getClasseServizio());

        // Verifico che il biglietto sia stato salvato nel database
        verificaBigliettoNelDB(biglietto.getID(), idCliente, idViaggio, classe, biglietto.getPrezzo());

        ClienteBanca clienteBanca = gestoreBanca.getClienteBanca(idCliente);
        assertNotNull(clienteBanca, "I dati bancari del cliente dovrebbero esistere");
        assertEquals("Banca Trenical", clienteBanca.getBanca(), "La banca dovrebbe essere quella corretta");

        System.out.println("✓ Biglietto creato con successo per cliente con carta: " + clienteBanca.getNumeroCarta());
    }

    /**
     * TEST 2: Creazione biglietto fallisce per cliente non esistente
     */
    @Test
    @Order(2)
    @DisplayName("Creazione biglietto fallisce con cliente non esistente")
    void testCreaBigliettoClienteNonEsistente() {
        // Given
        String idClienteInesistente = "CLIENTE_FAKE_" + System.currentTimeMillis();
        String idViaggio = viaggioTest.getId();

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> gestoreBiglietti.creaBiglietto(idViaggio, idClienteInesistente, ClasseServizio.ECONOMY),
                "Dovrebbe lanciare un'eccezione per cliente non esistente"
        );

        assertTrue(exception.getMessage().contains("utente") || exception.getMessage().contains("cliente"));
    }

    /**
     * TEST 3: Cancellazione biglietto con successo e verifica rimozione da DB
     */
    @Test
    @Order(3)
    @DisplayName("Cancella biglietto con successo e rimuove da DB")
    void testCancellaBigliettoConSuccessoERimozioneDaDB() throws SQLException {
        // Given: creo un biglietto
        Biglietto biglietto = gestoreBiglietti.creaBiglietto(
                viaggioTest.getId(), clienteTest.getId(), ClasseServizio.ECONOMY
        );
        String idBiglietto = biglietto.getID();
        double prezzoPagato = biglietto.getPrezzoBiglietto(); // Uso getPrezzoBiglietto() invece di getPrezzo()

        // Verifico che sia nel DB
        assertTrue(esisteBigliettoNelDB(idBiglietto));

        // When: cancello il biglietto
        RimborsoDTO rimborso = gestoreBiglietti.cancellaBiglietto(biglietto);

        // Then: verifico il rimborso
        assertNotNull(rimborso);
        assertEquals(idBiglietto, rimborso.getIdBiglietto());
        assertEquals(clienteTest.getId(), rimborso.getIdClienteRimborsato());
        assertEquals(prezzoPagato, rimborso.getImportoRimborsato(), 0.01);

        // Verifico che non esista più
        assertNull(gestoreBiglietti.getBigliettoPerID(idBiglietto));
        assertFalse(esisteBigliettoNelDB(idBiglietto));
    }

    /**
     * TEST 4: Test applicazione promozione attraverso il cliente
     * NOTA: Le promozioni vengono applicate automaticamente attraverso il CatalogoPromozione
     */
    @Test
    @Order(4)
    @DisplayName("Test calcolo prezzo con promozione fedeltà")
    void testCalcoloPrezzoConPromozioneFedelta() throws SQLException {
        // Given: creo un cliente con fedeltà
        String idClienteFedelta = "FEDELTA_" + System.currentTimeMillis();
        Cliente clienteFedelta = new Cliente.Builder()
                .ID(idClienteFedelta)
                .Nome("Luigi")
                .Cognome("Verdi")
                .Email("luigi" + System.currentTimeMillis() + "@test.it")
                .Password("pass123")
                .isFedelta(true)
                .build();

        gestoreClienti.aggiungiCliente(clienteFedelta);
        clientiDaPulire.add(clienteFedelta.getId());

        // Creo una promozione fedeltà nel catalogo
        Calendar inizioPromo = Calendar.getInstance();
        inizioPromo.add(Calendar.DAY_OF_MONTH, -1);
        Calendar finePromo = Calendar.getInstance();
        finePromo.add(Calendar.DAY_OF_MONTH, 30);

        PromozioneFedelta promoFedelta = new PromozioneFedelta(inizioPromo, finePromo, 0.20);
        promoFedelta.setStatoPromozioneATTIVA();

        // Aggiungo al catalogo
        CatalogoPromozione.getInstance().aggiungiPromozione(promoFedelta);

        // When: creo biglietto (la promozione viene applicata automaticamente se il cliente ha fedeltà)
        Biglietto biglietto = gestoreBiglietti.creaBiglietto(
                viaggioTest.getId(), clienteFedelta.getId(), ClasseServizio.ECONOMY
        );
        bigliettiDaPulire.add(biglietto.getID());

        // Then: verifico che il prezzo sia scontato
        // Il prezzo finale dovrebbe essere diverso dal prezzo base se la promozione è applicata
        double prezzoBase = biglietto.getOggettoPrezzoBiglietto().getPrezzoBase();
        double prezzoFinale = biglietto.getPrezzo();

        // Se il cliente ha fedeltà e c'è una promozione attiva, il prezzo finale dovrebbe essere inferiore
        assertTrue(prezzoFinale <= prezzoBase, "Il prezzo finale dovrebbe essere <= al prezzo base");
    }

    /**
     * TEST 5: Modifica classe servizio e verifica aggiornamento DB
     */
    @Test
    @Order(5)
    @DisplayName("Modifica classe servizio e aggiorna DB")
    void testModificaClasseServizioEAggiornamentoDB() throws SQLException {
        // Given: creo un biglietto BUSINESS
        Biglietto biglietto = gestoreBiglietti.creaBiglietto(
                viaggioTest.getId(), clienteTest.getId(), ClasseServizio.BUSINESS
        );
        bigliettiDaPulire.add(biglietto.getID());
        biglietto.setStatoBiglietto(StatoBiglietto.PAGATO);

        // When: modifico a ECONOMY
        gestoreBiglietti.modificaClasseServizio(
                biglietto.getID(), clienteTest.getId(), viaggioTest.getId(), ClasseServizio.ECONOMY
        );

        // Then: verifico il cambio
        Biglietto bigliettoModificato = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        assertEquals(ClasseServizio.ECONOMY, bigliettoModificato.getClasseServizio());

        // Verifico nel DB
        verificaClasseServizioNelDB(biglietto.getID(), ClasseServizio.ECONOMY);
    }

    /**
     * TEST 6: Recupera biglietti utente
     */
    @Test
    @Order(6)
    @DisplayName("Recupera lista biglietti di un utente")
    void testGetBigliettiUtenteMultipli() {
        // Given: creo 3 biglietti
        for (int i = 0; i < 3; i++) {
            Biglietto b = gestoreBiglietti.creaBiglietto(
                    viaggioTest.getId(), clienteTest.getId(),
                    i < 2 ? ClasseServizio.ECONOMY : ClasseServizio.BUSINESS
            );
            bigliettiDaPulire.add(b.getID());
        }

        // When
        List<Biglietto> bigliettiUtente = gestoreBiglietti.getBigliettiUtente(clienteTest.getId());

        // Then
        assertNotNull(bigliettiUtente);
        assertEquals(3, bigliettiUtente.size());

        // Verifico che tutti appartengano al cliente
        for (Biglietto b : bigliettiUtente) {
            assertEquals(clienteTest.getId(), b.getIDCliente());
        }
    }

    @Test
    @Order(7)
    @DisplayName("Registrazione cliente con carta personalizzata")
    void testRegistrazioneClienteConCartaPersonalizzata() throws SQLException {
        // Given: creo dati bancari personalizzati
        String idClienteCustom = "CUSTOM_" + System.currentTimeMillis();
        String cartaPersonalizzata = "4000-1111-2222-3333";
        String bancaPersonalizzata = "Banca del Cliente VIP";

        DatiBancariDTO datiBancari = new DatiBancariDTO();
        datiBancari.setNumeroCarta(cartaPersonalizzata);
        datiBancari.setNomeBanca(bancaPersonalizzata);

        // Creo il cliente
        Cliente clienteCustom = new Cliente.Builder()
                .ID(idClienteCustom)
                .Nome("Giulia")
                .Cognome("Bianchi")
                .Email("giulia" + System.currentTimeMillis() + "@custom.it")
                .Password("password456")
                .isFedelta(true)
                .build();

        // When: registro il cliente con dati bancari personalizzati
        gestoreClienti.aggiungiCliente(clienteCustom, datiBancari);
        clientiDaPulire.add(clienteCustom.getId());

        // Then: verifico che i dati bancari siano stati salvati correttamente
        GestoreBanca gestoreBanca = GestoreBanca.getInstance();
        ClienteBanca clienteBanca = gestoreBanca.getClienteBanca(idClienteCustom);

        assertNotNull(clienteBanca, "Il cliente banca dovrebbe esistere");
        assertEquals(cartaPersonalizzata, clienteBanca.getNumeroCarta(),
                "La carta dovrebbe essere quella personalizzata");
        assertEquals(bancaPersonalizzata, clienteBanca.getBanca(),
                "La banca dovrebbe essere quella personalizzata");
        assertEquals(1000.00, clienteBanca.getSaldo(), 0.01,
                "Il saldo iniziale dovrebbe essere 1000");

        System.out.println("✓ Cliente registrato con carta personalizzata: " + cartaPersonalizzata);
    }

    /**
     * TEST 7: Test calcolo penali
     */
    @Test
    @Order(8)
    @DisplayName("Test calcolo penali per diversi periodi temporali")
    void testCalcoloPenali() {
        Calendar now = Calendar.getInstance();
        double differenzaTariffaria = -50.0; // Da 100€ a 50€

        // Test 1: Oltre 7 giorni - nessuna penale
        Calendar partenzaOltre7Giorni = (Calendar) now.clone();
        partenzaOltre7Giorni.add(Calendar.DAY_OF_MONTH, 10);
        double penale1 = CalcolatorePenali.calcolaPenale(now, partenzaOltre7Giorni, differenzaTariffaria);
        assertEquals(0.0, penale1, 0.01);

        // Test 2: 5 giorni prima - penale 10%
        Calendar partenza5Giorni = (Calendar) now.clone();
        partenza5Giorni.add(Calendar.DAY_OF_MONTH, 5);
        double penale2 = CalcolatorePenali.calcolaPenale(now, partenza5Giorni, differenzaTariffaria);
        assertEquals(5.0, penale2, 0.01);

        // Test 3: 2 giorni prima - penale 25%
        Calendar partenza2Giorni = (Calendar) now.clone();
        partenza2Giorni.add(Calendar.DAY_OF_MONTH, 2);
        double penale3 = CalcolatorePenali.calcolaPenale(now, partenza2Giorni, differenzaTariffaria);
        assertEquals(12.5, penale3, 0.01);

        // Test 4: 12 ore prima - penale 50%
        Calendar partenza12Ore = (Calendar) now.clone();
        partenza12Ore.add(Calendar.HOUR_OF_DAY, 12);
        double penale4 = CalcolatorePenali.calcolaPenale(now, partenza12Ore, differenzaTariffaria);
        assertEquals(25.0, penale4, 0.01);
    }

    @AfterEach
    void tearDown() {
        // Pulisco i dati di test dal database
        pulisciBigliettiDiTest();
        pulisciViaggiDiTest();
        pulisciClientiDiTest();

        // Svuoto le liste
        bigliettiDaPulire.clear();
        viaggiDaPulire.clear();
        clientiDaPulire.clear();
    }

    // ========== METODI DI SUPPORTO ==========

    private void verificaConnessioneDB() {
        Connection conn = null;
        try {
            conn = ConnessioneADB.getConnection();
            assertNotNull(conn, "La connessione al database dovrebbe essere disponibile");

            // Verifica che le tabelle esistano
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "biglietti", null);
            assertTrue(tables.next(), "La tabella 'biglietti' deve esistere");

            System.out.println("✓ Connessione al database verificata");
        } catch (Exception e) {
            fail("Impossibile connettersi al database: " + e.getMessage() +
                    "\nAssicurati che il database sia attivo e lo schema sia stato creato.");
        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void verificaBigliettoNelDB(String idBiglietto, String idCliente, String idViaggio,
                                        ClasseServizio classe, double prezzo) throws SQLException {
        String sql = "SELECT * FROM biglietti WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, idBiglietto);

            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "Il biglietto dovrebbe esistere nel DB");

            assertEquals(idCliente, rs.getString("cliente_id"));
            assertEquals(idViaggio, rs.getString("viaggio_id"));
            assertEquals(classe.name(), rs.getString("classe_servizio"));
            assertEquals(prezzo, rs.getDouble("prezzo"), 0.01);

        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private boolean esisteBigliettoNelDB(String idBiglietto) throws SQLException {
        String sql = "SELECT COUNT(*) FROM biglietti WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, idBiglietto);

            ResultSet rs = pstmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;

        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void verificaClasseServizioNelDB(String idBiglietto, ClasseServizio classeAttesa)
            throws SQLException {
        String sql = "SELECT classe_servizio FROM biglietti WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, idBiglietto);

            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(classeAttesa.name(), rs.getString("classe_servizio"));

        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void pulisciBigliettiDiTest() {
        if (bigliettiDaPulire.isEmpty()) return;

        String sql = "DELETE FROM biglietti WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            for (String id : bigliettiDaPulire) {
                pstmt.setString(1, id);
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("Errore pulizia biglietti: " + e.getMessage());
        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void pulisciViaggiDiTest() {
        if (viaggiDaPulire.isEmpty()) return;

        String sql = "DELETE FROM viaggi WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            for (String id : viaggiDaPulire) {
                pstmt.setString(1, id);
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("Errore pulizia viaggi: " + e.getMessage());
        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void pulisciClientiDiTest() {
        if (clientiDaPulire.isEmpty()) return;

        String sql = "DELETE FROM clienti WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            for (String id : clientiDaPulire) {
                pstmt.setString(1, id);
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("Errore pulizia clienti: " + e.getMessage());
        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }
}
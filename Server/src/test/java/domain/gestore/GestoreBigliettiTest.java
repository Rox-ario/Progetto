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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GestoreBigliettiTest {

    private GestoreBiglietti gestoreBiglietti;
    private GestoreViaggi gestoreViaggi;
    private GestoreClienti gestoreClienti;
    private GestoreBanca gestoreBanca;

    private Cliente clienteTest;
    private Viaggio viaggioTest;
    private Stazione stazionePartenza;
    private Stazione stazioneArrivo;
    private Tratta trattaTest;

    private List<String> bigliettiDaPulire = new ArrayList<>();
    private List<String> viaggiDaPulire = new ArrayList<>();
    private List<String> clientiDaPulire = new ArrayList<>();

    @BeforeAll
    void setupAll() throws Exception {
        pulisciDatabaseCompleto();

        verificaConnessioneDB();

        gestoreBiglietti = GestoreBiglietti.getInstance();
        gestoreViaggi = GestoreViaggi.getInstance();
        gestoreClienti = GestoreClienti.getInstance();
        gestoreBanca = GestoreBanca.getInstance();

        ArrayList<Integer> binari = new ArrayList<>();
        binari.add(1);
        binari.add(2);
        binari.add(3);

        stazionePartenza = new Stazione("Roma", "Roma Termini TEST", binari, 41.9028, 12.4964);
        stazioneArrivo = new Stazione("Milano", "Milano Centrale TEST", binari, 45.4642, 9.1900);

        gestoreViaggi.aggiungiStazione(stazionePartenza);
        gestoreViaggi.aggiungiStazione(stazioneArrivo);

        trattaTest = new Tratta(stazionePartenza, stazioneArrivo);
        gestoreViaggi.aggiungiTratta(trattaTest);
    }

    @AfterEach
    void cleanup() throws SQLException {
        for (String idBiglietto : bigliettiDaPulire) {
            rimuoviBigliettoDaDB(idBiglietto);
        }

        bigliettiDaPulire.clear();
        viaggiDaPulire.clear();
        clientiDaPulire.clear();
    }

    @AfterAll
    void tearDownAll() throws SQLException {
        pulisciDatabaseCompleto();
        System.out.println("\n=== TEST COMPLETATI ===");
        System.out.println("Database pulito dai dati di test");
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
        long timestamp = System.currentTimeMillis();
        String idTreno = "TR_TEST_" + timestamp;

        try {
            gestoreViaggi.aggiungiTreno(idTreno, TipoTreno.ITALO);
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


    @Test
    @Order(1)
    @DisplayName("Creazione biglietto con successo e salvataggio su DB")
    void testCreaBigliettoConSuccessoEPersistenza() throws SQLException {

        String idCliente = clienteTest.getId();
        String idViaggio = viaggioTest.getId();
        ClasseServizio classe = ClasseServizio.ECONOMY;

        Biglietto biglietto = gestoreBiglietti.creaBiglietto(idViaggio, idCliente, classe);
        bigliettiDaPulire.add(biglietto.getID());

        assertNotNull(biglietto, "Il biglietto non dovrebbe essere null");
        assertEquals(idCliente, biglietto.getIDCliente());
        assertEquals(idViaggio, biglietto.getIDViaggio());
        assertEquals(classe, biglietto.getClasseServizio());

        verificaBigliettoNelDB(biglietto.getID(), idCliente, idViaggio, classe, biglietto.getPrezzo());

        ClienteBanca clienteBanca = gestoreBanca.getClienteBanca(idCliente);
        assertNotNull(clienteBanca, "I dati bancari del cliente dovrebbero esistere");
        assertEquals("Banca Trenical", clienteBanca.getBanca(), "La banca dovrebbe essere quella corretta");

        System.out.println("✓ Biglietto creato con successo per cliente con carta: " + clienteBanca.getNumeroCarta());
    }


    @Test
    @Order(2)
    @DisplayName("Creazione biglietto fallisce con cliente non esistente")
    void testCreaBigliettoClienteNonEsistente() {

        String idClienteInesistente = "CLIENTE_FAKE_" + System.currentTimeMillis();
        String idViaggio = viaggioTest.getId();


        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> gestoreBiglietti.creaBiglietto(idViaggio, idClienteInesistente, ClasseServizio.ECONOMY),
                "Dovrebbe lanciare un'eccezione per cliente non esistente"
        );

        assertTrue(exception.getMessage().contains("utente") || exception.getMessage().contains("cliente"));
    }


    @Test
    @Order(3)
    @DisplayName("Cancella biglietto con successo e rimuove da DB")
    void testCancellaBigliettoConSuccessoERimozioneDaDB() throws SQLException {

        Biglietto biglietto = gestoreBiglietti.creaBiglietto(
                viaggioTest.getId(), clienteTest.getId(), ClasseServizio.ECONOMY
        );
        String idBiglietto = biglietto.getID();
        double prezzoPagato = biglietto.getPrezzoBiglietto(); // Uso getPrezzoBiglietto() invece di getPrezzo()


        assertTrue(esisteBigliettoNelDB(idBiglietto));


        RimborsoDTO rimborso = gestoreBiglietti.cancellaBiglietto(biglietto);

        assertNotNull(rimborso);
        assertEquals(idBiglietto, rimborso.getIdBiglietto());
        assertEquals(clienteTest.getId(), rimborso.getIdClienteRimborsato());
        assertEquals(prezzoPagato, rimborso.getImportoRimborsato(), 0.01);

        assertNull(gestoreBiglietti.getBigliettoPerID(idBiglietto));
        assertFalse(esisteBigliettoNelDB(idBiglietto));
    }


    @Test
    @Order(4)
    @DisplayName("Test calcolo prezzo con promozione fedeltà")
    void testCalcoloPrezzoConPromozioneFedelta() throws SQLException {

        String idClienteFedelta = "FEDELTA_" + System.currentTimeMillis();
        Cliente clienteFedelta = new Cliente.Builder()
                .ID(idClienteFedelta)
                .Nome("Luigi")
                .Cognome("Verdi")
                .Email("luigi" + System.currentTimeMillis() + "@test.it")
                .Password("pass123")
                .isFedelta(true)
                .build();

        DatiBancariDTO datiBancariFedelta = new DatiBancariDTO
                (
                        clienteFedelta.getId(),
                        "Luigi",
                        "Verdi",
                        "9999-8888-7777-6666"
                );
        gestoreClienti.aggiungiCliente(clienteFedelta, datiBancariFedelta);
        clientiDaPulire.add(clienteFedelta.getId());


        Calendar inizioPromo = Calendar.getInstance();
        inizioPromo.add(Calendar.DAY_OF_MONTH, 1);
        Calendar finePromo = (Calendar) inizioPromo.clone();
        finePromo.add(Calendar.DAY_OF_MONTH, 1);

        PromozioneFedelta promoFedelta = new PromozioneFedelta(inizioPromo, finePromo, 0.20);
        promoFedelta.setStatoPromozioneATTIVA();

        CatalogoPromozione.getInstance().aggiungiPromozione(promoFedelta);

         Biglietto biglietto = gestoreBiglietti.creaBiglietto(
                viaggioTest.getId(), clienteFedelta.getId(), ClasseServizio.ECONOMY
        );
        bigliettiDaPulire.add(biglietto.getID());

        // Il prezzo finale dovrebbe essere diverso dal prezzo base se la promozione è applicata
        double prezzoBase = biglietto.getOggettoPrezzoBiglietto().getPrezzoBase();
        double prezzoFinale = biglietto.getPrezzo();

        // Se il cliente ha fedeltà e c'è una promozione attiva, il prezzo finale dovrebbe essere inferiore
        assertTrue(prezzoFinale <= prezzoBase, "Il prezzo finale dovrebbe essere <= al prezzo base");
    }

    @Test
    @Order(5)
    @DisplayName("Modifica classe servizio e aggiorna DB")
    void testModificaClasseServizioEAggiornamentoDB() throws SQLException {

        Biglietto biglietto = gestoreBiglietti.creaBiglietto(
                viaggioTest.getId(), clienteTest.getId(), ClasseServizio.BUSINESS
        );
        bigliettiDaPulire.add(biglietto.getID());
        biglietto.setStatoBiglietto(StatoBiglietto.PAGATO);


        gestoreBiglietti.modificaClasseServizio(
                biglietto.getID(), clienteTest.getId(), viaggioTest.getId(), ClasseServizio.ECONOMY
        );

        Biglietto bigliettoModificato = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        assertEquals(ClasseServizio.ECONOMY, bigliettoModificato.getClasseServizio());

        verificaClasseServizioNelDB(biglietto.getID(), ClasseServizio.ECONOMY);
    }

    @Test
    @Order(6)
    @DisplayName("Recupera lista biglietti di un utente")
    void testGetBigliettiUtenteMultipli() {

        for (int i = 0; i < 3; i++) {
            Biglietto b = gestoreBiglietti.creaBiglietto(
                    viaggioTest.getId(), clienteTest.getId(),
                    i < 2 ? ClasseServizio.ECONOMY : ClasseServizio.BUSINESS
            );
            bigliettiDaPulire.add(b.getID());
        }


        List<Biglietto> bigliettiUtente = gestoreBiglietti.getBigliettiUtente(clienteTest.getId());


        assertNotNull(bigliettiUtente);
        assertEquals(3, bigliettiUtente.size());

        for (Biglietto b : bigliettiUtente) {
            assertEquals(clienteTest.getId(), b.getIDCliente());
        }
    }

    @Test
    @Order(7)
    @DisplayName("Registrazione cliente con carta personalizzata")
    void testRegistrazioneClienteConCartaPersonalizzata() throws SQLException {

        String idClienteCustom = "CUSTOM_" + System.currentTimeMillis();
        String cartaPersonalizzata = "4000-1111-2222-3333";

        DatiBancariDTO datiBancari = new DatiBancariDTO(
                idClienteCustom,
                "Giulia",
                "Bianchi",
                cartaPersonalizzata
        );

        Cliente clienteCustom = new Cliente.Builder()
                .ID(idClienteCustom)
                .Nome("Giulia")
                .Cognome("Bianchi")
                .Email("giulia" + System.currentTimeMillis() + "@custom.it")
                .Password("password456")
                .isFedelta(true)
                .build();

        gestoreClienti.aggiungiCliente(clienteCustom, datiBancari);
        clientiDaPulire.add(clienteCustom.getId());

        GestoreBanca gestoreBanca = GestoreBanca.getInstance();
        ClienteBanca clienteBanca = gestoreBanca.getClienteBanca(idClienteCustom);

        assertNotNull(clienteBanca, "Il cliente banca dovrebbe esistere");
        assertEquals(cartaPersonalizzata, clienteBanca.getNumeroCarta(),
                "La carta dovrebbe essere quella personalizzata");
        assertEquals(1000.00, clienteBanca.getSaldo(), 0.01,
                "Il saldo iniziale dovrebbe essere 1000");

        System.out.println("✓ Cliente registrato con carta personalizzata: " + cartaPersonalizzata);
    }


    @Test
    @Order(8)
    @DisplayName("Test calcolo penali per diversi periodi temporali")
    void testCalcoloPenali() {
        Calendar now = Calendar.getInstance();
        double differenzaTariffaria = -50.0; // Da 100€ a 50€


        Calendar partenzaOltre7Giorni = (Calendar) now.clone();
        partenzaOltre7Giorni.add(Calendar.DAY_OF_MONTH, 10);
        double penale1 = CalcolatorePenali.calcolaPenale(now, partenzaOltre7Giorni, differenzaTariffaria);
        assertEquals(0.0, penale1, 0.01);


        Calendar partenza5Giorni = (Calendar) now.clone();
        partenza5Giorni.add(Calendar.DAY_OF_MONTH, 5);
        double penale2 = CalcolatorePenali.calcolaPenale(now, partenza5Giorni, differenzaTariffaria);
        assertEquals(5.0, penale2, 0.01);

        Calendar partenza2Giorni = (Calendar) now.clone();
        partenza2Giorni.add(Calendar.DAY_OF_MONTH, 2);
        double penale3 = CalcolatorePenali.calcolaPenale(now, partenza2Giorni, differenzaTariffaria);
        assertEquals(12.5, penale3, 0.01);

        Calendar partenza12Ore = (Calendar) now.clone();
        partenza12Ore.add(Calendar.HOUR_OF_DAY, 12);
        double penale4 = CalcolatorePenali.calcolaPenale(now, partenza12Ore, differenzaTariffaria);
        assertEquals(25.0, penale4, 0.01);
    }

    @AfterEach
    void tearDown() {
        pulisciBigliettiDiTest();
        pulisciViaggiDiTest();
        pulisciClientiDiTest();

        bigliettiDaPulire.clear();
        viaggiDaPulire.clear();
        clientiDaPulire.clear();
    }

    private void verificaConnessioneDB() {
        Connection conn = null;
        try {
            conn = ConnessioneADB.getConnection();
            assertNotNull(conn, "La connessione al database dovrebbe essere disponibile");


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

        String deleteClientiBancaSql = "DELETE FROM clienti_banca WHERE cliente_id = ?";
        String deleteClientiSql = "DELETE FROM clienti WHERE id = ?";

        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();

            PreparedStatement pstmtBanca = conn.prepareStatement(deleteClientiBancaSql);
            PreparedStatement pstmtClienti = conn.prepareStatement(deleteClientiSql);

            for (String id : clientiDaPulire) {
                pstmtBanca.setString(1, id);
                pstmtBanca.executeUpdate();

                pstmtClienti.setString(1, id);
                pstmtClienti.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("Errore pulizia clienti: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();  // Annulla tutto in caso di errore
                } catch (SQLException ex) {
                    System.err.println("Errore rollback: " + ex.getMessage());
                }
            }
        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }
}

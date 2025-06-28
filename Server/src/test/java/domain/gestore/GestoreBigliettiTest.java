package domain.gestore;

import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.*;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.*;
import it.trenical.server.domain.gestore.GestoreBiglietti;
import it.trenical.server.domain.gestore.GestoreClienti;
import it.trenical.server.domain.gestore.GestoreViaggi;
import it.trenical.server.dto.RimborsoDTO;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;
import java.util.Calendar;
import java.util.ArrayList;

//Test per GestoreBiglietti con utilizzo del database reale
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GestoreBigliettiTest
{

    private GestoreBiglietti gestoreBiglietti;
    private GestoreViaggi gestoreViaggi;
    private GestoreClienti gestoreClienti;

    private Cliente clienteTest;
    private Viaggio viaggioTest;
    private Stazione stazionePartenza;
    private Stazione stazioneArrivo;
    private Tratta trattaTest;

    //Liste per tenere traccia degli ID da pulire
    private List<String> bigliettiDaPulire = new ArrayList<>();
    private List<String> viaggiDaPulire = new ArrayList<>();
    private List<String> clientiDaPulire = new ArrayList<>();

    @BeforeAll
    void setupAll() throws Exception
    {
        //verifico che il database sia accessibile
        verificaConnessioneDB();

        //inizializzo i singleton
        gestoreBiglietti = GestoreBiglietti.getInstance();
        gestoreViaggi = GestoreViaggi.getInstance();
        gestoreClienti = GestoreClienti.getInstance();

        //creo stazioni di test
        ArrayList<Integer> binari1 = new ArrayList<>();
        binari1.add(1); binari1.add(2); binari1.add(3);
        stazionePartenza = new Stazione("Roma", "Roma Termini TEST", binari1, 41.9028, 12.4964);
        stazioneArrivo = new Stazione("Milano","Milano Centrale TEST", binari1, 45.4642, 9.1900);

        //aggiungo le stazioni al gestore
        gestoreViaggi.aggiungiStazione(stazionePartenza);
        gestoreViaggi.aggiungiStazione(stazioneArrivo);

        //creo la tratta
        trattaTest = new Tratta(stazionePartenza, stazioneArrivo);
        gestoreViaggi.aggiungiTratta(trattaTest);
    }

    @BeforeEach
    void setup()
    {
        String idTreno = "TR_TEST_" + 1;
        gestoreViaggi.aggiungiTreno(idTreno, TipoTreno.ITALO);

        //creo un cliente di test con ID univoco
        String idCliente = "TEST_" + System.currentTimeMillis();
        clienteTest = new Cliente.Builder().ID(idCliente)
                .Nome("Rosario").Cognome("Chiappetta")
                .Email("rosariochiappetta03@gmail.com").Password("password123")
                .isFedelta(true).build();
        gestoreClienti.aggiungiCliente(clienteTest);
        clientiDaPulire.add(clienteTest.getId());

        //creo un viaggio di test per domani alle 14:00
        Calendar partenza = Calendar.getInstance();
        partenza.add(Calendar.DAY_OF_MONTH, 1);
        partenza.set(Calendar.HOUR_OF_DAY, 14);
        partenza.set(Calendar.MINUTE, 0);

        Calendar arrivo = (Calendar) partenza.clone();
        arrivo.add(Calendar.HOUR_OF_DAY, 3);

        //programmo il viaggio usando il metodo di GestoreViaggi
        viaggioTest = gestoreViaggi.programmaViaggio(idTreno, trattaTest.getId(), partenza, arrivo);
        viaggioTest.setStato(StatoViaggio.PROGRAMMATO);

        viaggiDaPulire.add(viaggioTest.getId());
    }

      @Test
    @Order(1)
    @DisplayName("Creazione biglietto con successo e salvataggio su DB")
    void testCreaBigliettoConSuccessoEPersistenza() throws SQLException
      {
        //ho un cliente e un viaggio validi
        String idCliente = clienteTest.getId();
        String idViaggio = viaggioTest.getId();
        ClasseServizio classe = ClasseServizio.ECONOMY;

        //creo un biglietto
        Biglietto biglietto = gestoreBiglietti.creaBiglietto(idCliente, idViaggio, classe);
        bigliettiDaPulire.add(biglietto.getID());

        //verifico che il biglietto sia stato creato correttamente
        assertNotNull(biglietto, "Il biglietto non dovrebbe essere null");
        assertEquals(idCliente, biglietto.getIDCliente());
        assertEquals(idViaggio, biglietto.getIDViaggio());
        assertEquals(classe, biglietto.getClasseServizio());

        //Verifico che il biglietto sia stato salvato nel database
        verificaBigliettoNelDB(biglietto.getID(), idCliente, idViaggio, classe, biglietto.getPrezzo());
    }

    @Test
    @Order(2)
    @DisplayName("Creazione biglietto fallisce con cliente non esistente")
    void testCreaBigliettoClienteNonEsistente() {
        //un ID cliente che non esiste
        String idClienteInesistente = "CLIENTE_FAKE_" + System.currentTimeMillis();
        String idViaggio = viaggioTest.getId();

        //mi aspetto un'eccezione
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> gestoreBiglietti.creaBiglietto(idClienteInesistente, idViaggio, ClasseServizio.ECONOMY),
                "Dovrebbe lanciare un'eccezione per cliente non esistente"
        );

        assertTrue(exception.getMessage().contains("cliente"),
                "Il messaggio dovrebbe menzionare il cliente");
    }

    @Test
    @Order(3)
    @DisplayName("Cancella biglietto con successo e rimuove da DB")
    void testCancellaBigliettoConSuccessoERimozioneDaDB() throws SQLException {
        //creo un biglietto
        Biglietto biglietto = gestoreBiglietti.creaBiglietto(
                clienteTest.getId(), viaggioTest.getId(), ClasseServizio.ECONOMY
        );
        String idBiglietto = biglietto.getID();
        double prezzoPagato = biglietto.getPrezzo();

        //verifico che sia nel DB prima della cancellazione
        assertTrue(esisteBigliettoNelDB(idBiglietto),
                "Il biglietto dovrebbe esistere nel DB prima della cancellazione");

        //cancello il biglietto
        RimborsoDTO rimborso = gestoreBiglietti.cancellaBiglietto(biglietto);

        //verifico il rimborso
        assertNotNull(rimborso, "Il rimborso non dovrebbe essere null");
        assertEquals(idBiglietto, rimborso.getIdBiglietto());
        assertEquals(clienteTest.getId(), rimborso.getIdClienteRimborsato());
        assertEquals(prezzoPagato, rimborso.getImportoRimborsato(), 0.01);

        //verifico che il biglietto non esista più nel gestore
        assertNull(gestoreBiglietti.getBigliettoPerID(idBiglietto));

        //verifico che sia stato rimosso dal database
        assertFalse(esisteBigliettoNelDB(idBiglietto),
                "Il biglietto non dovrebbe più esistere nel DB");
    }


    @Test
    @Order(4)
    @DisplayName("Applica promozione fedeltà del 20% e aggiorna DB")
    void testApplicaPromozioneFedeltaEAggiornamentoDB() throws SQLException {
        //creo un biglietto
        Biglietto biglietto = gestoreBiglietti.creaBiglietto(
                clienteTest.getId(), viaggioTest.getId(), ClasseServizio.ECONOMY
        );
        bigliettiDaPulire.add(biglietto.getID());

        double kilometri = viaggioTest.getKilometri();
        double aggiuntaTipo = viaggioTest.getTreno().getTipo().getAumentoPrezzo();
        double aggiuntaServizio = biglietto.getClasseServizio().getCoefficienteAumentoPrezzo();

        double prezzoBase = kilometri * aggiuntaServizio * aggiuntaTipo;

        //creo una promozione fedeltà del 20%
        Calendar inizioPromo = Calendar.getInstance();
        inizioPromo.add(Calendar.DAY_OF_MONTH, -1); // Ieri
        Calendar finePromo = Calendar.getInstance();
        finePromo.add(Calendar.DAY_OF_MONTH, 30); // Tra 30 giorni

        PromozioneFedelta promoFedelta = new PromozioneFedelta(inizioPromo, finePromo, 0.20);
        promoFedelta.setStatoPromozioneATTIVA(); // La attivo

        //prendo il prezzo a cui abbiamo già applicato la promozione
        double prezzoScontato = biglietto.getPrezzo();

        assertEquals(prezzoScontato, prezzoBase, 0.01,
                "Il prezzo dovrebbe essere scontato del 20%");

        // Verifico che il prezzo sia aggiornato nel database
        verificaPrezzoNelDB(biglietto.getID(), prezzoScontato);
    }

    @Test
    @Order(5)
    @DisplayName("Modifica classe servizio e aggiorna DB")
    void testModificaClasseServizioEAggiornamentoDB() throws SQLException {
        // Given: creo un biglietto BUSINESS
        Biglietto biglietto = gestoreBiglietti.creaBiglietto(
                clienteTest.getId(), viaggioTest.getId(), ClasseServizio.BUSINESS
        );
        bigliettiDaPulire.add(biglietto.getID());
        biglietto.setStatoBiglietto(StatoBiglietto.PAGATO);

        //modifico a ECONOMY
        gestoreBiglietti.modificaClasseServizio(
                biglietto.getID(), clienteTest.getId(), viaggioTest.getId(), ClasseServizio.ECONOMY
        );

        //verifico il cambio nel gestore
        Biglietto bigliettoModificato = gestoreBiglietti.getBigliettoPerID(biglietto.getID());
        assertEquals(ClasseServizio.ECONOMY, bigliettoModificato.getClasseServizio());

        //verifico che sia aggiornato nel database
        verificaClasseServizioNelDB(biglietto.getID(), ClasseServizio.ECONOMY);
    }


    @Test
    @Order(6)
    @DisplayName("Recupera lista biglietti di un utente da DB")
    void testGetBigliettiUtenteMultipli() {
        //creo 3 biglietti per lo stesso utente
        Biglietto biglietto1 = gestoreBiglietti.creaBiglietto(
                clienteTest.getId(), viaggioTest.getId(), ClasseServizio.ECONOMY
        );
        Biglietto biglietto2 = gestoreBiglietti.creaBiglietto(
                clienteTest.getId(), viaggioTest.getId(), ClasseServizio.ECONOMY
        );
        Biglietto biglietto3 = gestoreBiglietti.creaBiglietto(
                clienteTest.getId(), viaggioTest.getId(), ClasseServizio.BUSINESS
        );

        bigliettiDaPulire.add(biglietto1.getID());
        bigliettiDaPulire.add(biglietto2.getID());
        bigliettiDaPulire.add(biglietto3.getID());

        //recupero i biglietti dell'utente
        List<Biglietto> bigliettiUtente = gestoreBiglietti.getBigliettiUtente(clienteTest.getId());

        //verifico che ci siano tutti e 3 i biglietti
        assertNotNull(bigliettiUtente);
        assertEquals(3, bigliettiUtente.size());

        //verifico che tutti appartengano all'utente giusto
        for (Biglietto b : bigliettiUtente)
        {
            assertEquals(clienteTest.getId(), b.getIDCliente());
        }
    }


    @Test
    @Order(7)
    @DisplayName("Test calcolo penali per diversi periodi temporali")
    void testCalcoloPenali()
    {
        Calendar now = Calendar.getInstance();
        double differenzaTariffaria = -50.0; // Da 100€ a 50€

        //Test 1: Oltre 7 giorni - nessuna penale
        Calendar partenzaOltre7Giorni = (Calendar) now.clone();
        partenzaOltre7Giorni.add(Calendar.DAY_OF_MONTH, 10);
        double penale1 = CalcolatorePenali.calcolaPenale(now, partenzaOltre7Giorni, differenzaTariffaria);
        assertEquals(0.0, penale1, 0.01, "Nessuna penale oltre 7 giorni");

        //Test 2: 5 giorni prima - penale 10%
        Calendar partenza5Giorni = (Calendar) now.clone();
        partenza5Giorni.add(Calendar.DAY_OF_MONTH, 5);
        double penale2 = CalcolatorePenali.calcolaPenale(now, partenza5Giorni, differenzaTariffaria);
        assertEquals(5.0, penale2, 0.01, "Penale 10% di 50€ = 5€");

        //Test 3: 2 giorni prima - penale 25%
        Calendar partenza2Giorni = (Calendar) now.clone();
        partenza2Giorni.add(Calendar.DAY_OF_MONTH, 2);
        double penale3 = CalcolatorePenali.calcolaPenale(now, partenza2Giorni, differenzaTariffaria);
        assertEquals(12.5, penale3, 0.01, "Penale 25% di 50€ = 12.5€");

        //Test 4: 12 ore prima - penale 50%
        Calendar partenza12Ore = (Calendar) now.clone();
        partenza12Ore.add(Calendar.HOUR_OF_DAY, 12);
        double penale4 = CalcolatorePenali.calcolaPenale(now, partenza12Ore, differenzaTariffaria);
        assertEquals(25.0, penale4, 0.01, "Penale 50% di 50€ = 25€");
    }

    @AfterEach
    void tearDown()
    {
        //pulisco i dati di test dal database in ordine inverso per evitare violazioni FK
        pulisciBigliettiDiTest();
        pulisciViaggiDiTest();
        pulisciClientiDiTest();

        //svuoto le liste per il prossimo test
        bigliettiDaPulire.clear();
        viaggiDaPulire.clear();
        clientiDaPulire.clear();
    }

    @AfterAll
    void tearDownAll()
    {
        System.out.println("\n========== TEST COMPLETATI ==========");
        System.out.println("Database pulito dai dati di test");
    }

   private void verificaConnessioneDB() throws Exception
   {
       ConnessioneADB.inizializzaDatabase("C:/Users/rosar/Desktop/Uni III anno/Secondo Semestre/Ingegneria del Software/Progetto/treniCal/src/main/resources/sql/schema.sql");
   }

    private void verificaBigliettoNelDB(String idBiglietto, String idCliente, String idViaggio,
                                        ClasseServizio classe, double prezzo) throws SQLException
    {
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

        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private boolean esisteBigliettoNelDB(String idBiglietto) throws SQLException
    {
        String sql = "SELECT COUNT(*) FROM biglietti WHERE id = ?";
        Connection conn = null;

        try
        {
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

    private void verificaPrezzoNelDB(String idBiglietto, double prezzoAtteso) throws SQLException
    {
        String sql = "SELECT prezzo FROM biglietti WHERE id = ?";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, idBiglietto);

            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "Il biglietto dovrebbe esistere nel DB");
            assertEquals(prezzoAtteso, rs.getDouble("prezzo"), 0.01);

        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void verificaClasseServizioNelDB(String idBiglietto, ClasseServizio classeAttesa)
            throws SQLException
    {
        String sql = "SELECT classe_servizio FROM biglietti WHERE id = ?";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, idBiglietto);

            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "Il biglietto dovrebbe esistere nel DB");
            assertEquals(classeAttesa.name(), rs.getString("classe_servizio"));

        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void pulisciBigliettiDiTest()
    {
        if (bigliettiDaPulire.isEmpty()) return;

        String sql = "DELETE FROM biglietti WHERE id = ?";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            for (String id : bigliettiDaPulire)
            {
                pstmt.setString(1, id);
                pstmt.executeUpdate();
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore durante la pulizia biglietti di test: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void pulisciViaggiDiTest()
    {
        if (viaggiDaPulire.isEmpty()) return;

        String sql = "DELETE FROM viaggi WHERE id = ?";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            for (String id : viaggiDaPulire)
            {
                pstmt.setString(1, id);
                pstmt.executeUpdate();
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore durante la pulizia viaggi di test: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void pulisciClientiDiTest()
    {
        if (clientiDaPulire.isEmpty()) return;

        String sql = "DELETE FROM clienti WHERE id = ?";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            for (String id : clientiDaPulire)
            {
                pstmt.setString(1, id);
                pstmt.executeUpdate();
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore durante la pulizia clienti di test: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }
}
package domain.gestore;

import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.*;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.*;
import it.trenical.server.domain.gestore.*;

import it.trenical.server.dto.DatiBancariDTO;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CatalogoPromozioniTest
{
    private CatalogoPromozione catalogoPromozione;
    private GestoreViaggi gestoreViaggi;
    private GestoreClienti gestoreClienti;

    private Tratta trattaTest;
    private Stazione stazionePartenza;
    private Stazione stazioneArrivo;
    private Cliente clienteFedeltaTest;

    private List<Promozione> promozioniDaPulire = new ArrayList<>();
    private List<String> clientiDaPulire = new ArrayList<>();


    @BeforeAll
    void setupAll() throws Exception
    {
        System.out.println("\n=== INIZIO TEST CATALOGO PROMOZIONI ===");

        pulisciDatabaseCompleto();

        catalogoPromozione = CatalogoPromozione.getInstance();
        gestoreViaggi = GestoreViaggi.getInstance();
        gestoreClienti = GestoreClienti.getInstance();

        ArrayList<Integer> binari = new ArrayList<>();
        for(int i = 1; i <= 5; i++)
        {
            binari.add(i);
        }

        stazionePartenza = new Stazione("Napoli", "Napoli Centrale TEST", binari, 40.8518, 14.2681);
        stazioneArrivo = new Stazione("Roma", "Roma Termini TEST", binari, 41.9028, 12.4964);

        gestoreViaggi.aggiungiStazione(stazionePartenza);
        gestoreViaggi.aggiungiStazione(stazioneArrivo);

        trattaTest = new Tratta(stazionePartenza, stazioneArrivo);
        gestoreViaggi.aggiungiTratta(trattaTest);

        clienteFedeltaTest = new Cliente.Builder()
                .ID("FEDELTA_TEST_" + System.currentTimeMillis())
                .Nome("Mario")
                .Cognome("Rossi")
                .Email("mario.rossi@test.it")
                .Password("password123")
                .isFedelta(true)
                .riceviPromozioni(true)
                .build();

        DatiBancariDTO datiBancari = new DatiBancariDTO(
                clienteFedeltaTest.getId(),
                "Mario",
                "Rossi",
                "1234-5678-9012-3456"
        );

        gestoreClienti.aggiungiCliente(clienteFedeltaTest, datiBancari);
        clientiDaPulire.add(clienteFedeltaTest.getId());
    }

    @AfterEach
    void cleanup() throws SQLException
    {
        for (Promozione promozione : promozioniDaPulire)
        {
            catalogoPromozione.rimuoviPromozione(promozione);
        }
        promozioniDaPulire.clear();
    }

    @AfterAll
    void tearDownAll() throws SQLException
    {
        pulisciDatabaseCompleto();
        System.out.println("=== TEST CATALOGO PROMOZIONI COMPLETATI ===\n");
    }

    @Test
    @Order(1)
    @DisplayName("Creazione promozione fedeltà con successo")
    void testCreaPromozioneFedeltaConSuccesso()
    {
        Calendar dataInizio = Calendar.getInstance();//Inizia oggi
        dataInizio.add(Calendar.SECOND, 1);

        Calendar dataFine = (Calendar) dataInizio.clone();
        dataFine.add(Calendar.MONTH, 1); //Dura un mese

        double percentualeSconto = 0.15; //15% di sconto

        PromozioneFedelta promozione = new PromozioneFedelta(dataInizio, dataFine, percentualeSconto);
        promozione.setStatoPromozioneATTIVA();

        assertDoesNotThrow(() -> catalogoPromozione.aggiungiPromozione(promozione),
                "Dovrebbe aggiungere la promozione senza errori");

        promozioniDaPulire.add(promozione);

        List<Promozione> promozioniFedelta = catalogoPromozione.getPromoPerTipo(TipoPromozione.FEDELTA);
        assertTrue(promozioniFedelta.stream()
                        .anyMatch(p -> p.getID().equals(promozione.getID())),
                "La promozione dovrebbe essere presente nel catalogo");
    }

    @Test
    @Order(2)
    @DisplayName("Creazione promozione tratta con successo")
    void testCreaPromozioneTrattaConSuccesso()
    {
        Calendar dataInizio = Calendar.getInstance();
        dataInizio.add(Calendar.SECOND, 2);

        Calendar dataFine = (Calendar) dataInizio.clone();
        dataFine.add(Calendar.WEEK_OF_YEAR, 2); //Due settimane

        double percentualeSconto = 0.20; //20% di sconto

        PromozioneTratta promozione = new PromozioneTratta(trattaTest, dataInizio, dataFine, percentualeSconto);
        promozione.setStatoPromozioneATTIVA();

        catalogoPromozione.aggiungiPromozione(promozione);
        promozioniDaPulire.add(promozione);

        PromozioneTratta promoAttiva = catalogoPromozione.getPromozioneAttivaTratta(trattaTest);
        assertNotNull(promoAttiva, "Dovrebbe esserci una promozione attiva per la tratta");
        assertEquals(promozione.getID(), promoAttiva.getID());
    }

    @Test
    @Order(3)
    @DisplayName("Creazione promozione treno con successo")
    void testCreaPromozioneTrenoConSuccesso()
    {
        Calendar dataInizio = Calendar.getInstance();
        dataInizio.add(Calendar.SECOND, 3);

        Calendar dataFine = (Calendar) dataInizio.clone();
        dataFine.add(Calendar.DAY_OF_MONTH, 10);

        double percentualeSconto = 0.25; //25% di sconto
        TipoTreno tipoTreno = TipoTreno.ITALO;

        PromozioneTreno promozione = new PromozioneTreno(dataInizio, dataFine, percentualeSconto, tipoTreno);
        promozione.setStatoPromozioneATTIVA();

        catalogoPromozione.aggiungiPromozione(promozione);
        promozioniDaPulire.add(promozione);

        PromozioneTreno promoAttiva = catalogoPromozione.getPromozioneAttivaPerTipoTreno(tipoTreno);
        assertNotNull(promoAttiva);
        assertEquals(tipoTreno, promoAttiva.getTipoTreno());
    }

    @Test
    @Order(4)
    @DisplayName("Verifica sovrapposizione promozioni fedeltà")
    void testSovrapposizionePromozioniFedelta()
    {
        Calendar dataInizio1 = Calendar.getInstance();
        dataInizio1.add(Calendar.SECOND, 5);
        Calendar dataFine1 = (Calendar) dataInizio1.clone();
        dataFine1.add(Calendar.DAY_OF_MONTH, 15);

        PromozioneFedelta promo1 = new PromozioneFedelta(dataInizio1, dataFine1, 0.10);
        promo1.setStatoPromozioneATTIVA();
        catalogoPromozione.aggiungiPromozione(promo1);
        promozioniDaPulire.add(promo1);
        //Seconda promozione sovrapposta (stesso periodo)
        Calendar dataInizio2 = Calendar.getInstance();
        dataInizio2.add(Calendar.SECOND, 6); //Si sovrappone
        Calendar dataFine2 = (Calendar) dataInizio2.clone();
        dataFine2.add(Calendar.DAY_OF_MONTH, 20);

        PromozioneFedelta promo2 = new PromozioneFedelta(dataInizio2, dataFine2, 0.15);
        promo2.setStatoPromozioneATTIVA();
        System.out.println("Id promo2 = "+promo2.getID());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalogoPromozione.aggiungiPromozione(promo2),
                "Dovrebbe impedire promozioni fedeltà sovrapposte"
        );

        assertTrue(exception.getMessage().contains("sovrappone"),
                "Il messaggio dovrebbe indicare la sovrapposizione");
    }

    @Test
    @Order(5)
    @DisplayName("Verifica sovrapposizione promozioni tratta diverse")
    void testPromozioniTratteDiverseNonSiSovrappongono()
    {
        Stazione stazione3 = new Stazione("Milano", "Milano Centrale TEST", new ArrayList<>(), 45.4642, 9.1900);
        gestoreViaggi.aggiungiStazione(stazione3);
        Tratta tratta2 = new Tratta(stazioneArrivo, stazione3);
        gestoreViaggi.aggiungiTratta(tratta2);

        Calendar dataInizio = Calendar.getInstance();
        dataInizio.add(Calendar.SECOND, 1);
        Calendar dataFine = (Calendar) dataInizio.clone();
        dataFine.add(Calendar.DAY_OF_MONTH, 10);

        PromozioneTratta promo1 = new PromozioneTratta(trattaTest, dataInizio, dataFine, 0.20);
        promo1.setStatoPromozioneATTIVA();
        catalogoPromozione.aggiungiPromozione(promo1);
        promozioniDaPulire.add(promo1);

        PromozioneTratta promo2 = new PromozioneTratta(tratta2, dataInizio, dataFine, 0.25);
        promo2.setStatoPromozioneATTIVA();

        assertDoesNotThrow(() -> catalogoPromozione.aggiungiPromozione(promo2),
                "Promozioni su tratte diverse non dovrebbero sovrapporsi");
        promozioniDaPulire.add(promo2);
    }

    @Test
    @Order(6)
    @DisplayName("Test recupero tutte le promozioni")
    void testGetTutteLePromozioni()
    {
        int numeroIniziale = catalogoPromozione.getTutteLePromozioni().size();

        Calendar dataInizio = Calendar.getInstance();
        dataInizio.add(Calendar.MONTH, 2); //Per evitare sovrapposizioni
        Calendar dataFine = (Calendar) dataInizio.clone();
        dataFine.add(Calendar.DAY_OF_MONTH, 5);

        PromozioneFedelta promoFedelta = new PromozioneFedelta(dataInizio, dataFine, 0.10);
        catalogoPromozione.aggiungiPromozione(promoFedelta);
        promozioniDaPulire.add(promoFedelta);

        PromozioneTratta promoTratta = new PromozioneTratta(trattaTest, dataInizio, dataFine, 0.15);
        catalogoPromozione.aggiungiPromozione(promoTratta);
        promozioniDaPulire.add(promoTratta);

        PromozioneTreno promoTreno = new PromozioneTreno(dataInizio, dataFine, 0.20, TipoTreno.ITALO);
        catalogoPromozione.aggiungiPromozione(promoTreno);
        promozioniDaPulire.add(promoTreno);

        List<Promozione> tuttePromozioni = catalogoPromozione.getTutteLePromozioni();
        assertEquals(numeroIniziale + 3, tuttePromozioni.size(),
                "Dovrebbero esserci 3 promozioni in più");
    }

    @Test
    @Order(7)
    @DisplayName("Test promozione con date invalide")
    void testPromozioneConDateInvalide()
    {
        Calendar dataInizio = Calendar.getInstance();
        dataInizio.add(Calendar.DAY_OF_MONTH, 10);

        Calendar dataFine = Calendar.getInstance();
        dataFine.add(Calendar.DAY_OF_MONTH, 5); // Fine prima dell'inizio!

        assertThrows(IllegalArgumentException.class,
                () -> new PromozioneFedelta(dataInizio, dataFine, 0.15),
                "Non dovrebbe permettere data fine prima di data inizio");
    }

    @Test
    @Order(8)
    @DisplayName("Test registrazione observer per promozioni fedeltà")
    void testRegistrazioneObserverPromozioniFedelta()
    {
        //Crea un secondo cliente fedeltà che vuole notifiche
        Cliente clienteFedelta2 = new Cliente.Builder()
                .ID("FEDELTA2_" + System.currentTimeMillis())
                .Nome("Luigi")
                .Cognome("Verdi")
                .Email("luigi.verdi@test.it")
                .Password("pass456")
                .isFedelta(true)
                .riceviPromozioni(true)
                .riceviNotifiche(false)
                .build();

        DatiBancariDTO datiBancari = new DatiBancariDTO(
                clienteFedelta2.getId(),
                "Luigi",
                "Verdi",
                "1224-5658-9012-3456"
        );
        gestoreClienti.aggiungiCliente(clienteFedelta2, datiBancari);
        clientiDaPulire.add(clienteFedelta2.getId());

        Calendar dataInizio = Calendar.getInstance();
        dataInizio.add(Calendar.SECOND, 3);
        Calendar dataFine = (Calendar) dataInizio.clone();
        dataFine.add(Calendar.DAY_OF_MONTH, 7);

        PromozioneFedelta promozione = new PromozioneFedelta(dataInizio, dataFine, 0.30);
        promozione.setStatoPromozioneATTIVA();

        //Quando aggiungo la promozione, dovrebbe registrare gli observer
        catalogoPromozione.aggiungiPromozione(promozione);
        promozioniDaPulire.add(promozione);

        assertFalse(promozione.getObservers().isEmpty(), "Dovrebbero esserci observer registrati per la promozione fedeltà");
    }

    @Test
    @Order(9)
    @DisplayName("Test recupero promozioni attive")
    void testGetPromozioniAttive()
    {
        Calendar dataInizio = Calendar.getInstance();
        dataInizio.add(Calendar.MILLISECOND, 1);
        Calendar dataFine = (Calendar) dataInizio.clone();
        dataFine.add(Calendar.DAY_OF_MONTH, 10); // Ancora valida

        PromozioneTreno promoAttiva = new PromozioneTreno(dataInizio, dataFine, 0.15, TipoTreno.INTERCITY);
        promoAttiva.setStatoPromozioneATTIVA();
        catalogoPromozione.aggiungiPromozione(promoAttiva);
        promozioniDaPulire.add(promoAttiva);

        //Creo una promozione non attiva
        Calendar dataInizio2 = Calendar.getInstance();
        dataInizio2.add(Calendar.DAY_OF_MONTH, 40);
        Calendar dataFine2 = (Calendar) dataInizio2.clone();
        dataFine2.add(Calendar.DAY_OF_MONTH, 5);

        PromozioneTreno promoNonAttiva = new PromozioneTreno(dataInizio2, dataFine2, 0.20, TipoTreno.ITALO);
        catalogoPromozione.aggiungiPromozione(promoNonAttiva);
        promozioniDaPulire.add(promoNonAttiva);

        List<Promozione> promozioniAttive = catalogoPromozione.getPromozioniAttive();
        assertTrue(promozioniAttive.stream().anyMatch(p -> p.getID().equals(promoAttiva.getID())),
                "La promozione attiva dovrebbe essere nella lista");
        assertFalse(promozioniAttive.stream().anyMatch(p -> p.getID().equals(promoNonAttiva.getID())),
                "La promozione non attiva non dovrebbe essere nella lista");
    }

    @Test
    @Order(10)
    @DisplayName("Test aggiunta promozione null")
    void testAggiungiPromozioneNull()
    {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalogoPromozione.aggiungiPromozione(null),
                "Non dovrebbe permettere l'aggiunta di promozioni null"
        );

        assertTrue(exception.getMessage().toLowerCase().contains("null"),
                "Il messaggio di errore dovrebbe menzionare null");
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
            stmt.execute("DELETE FROM promozioni");

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            System.out.println("Database pulito dai dati di test");

        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }
}

package domain.gestore.TestNotifiche;

import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.NotificheListener;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.gestore.*;
import it.trenical.server.dto.DatiBancariDTO;
import it.trenical.server.dto.NotificaDTO;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GestoreNotificheTest
{
    private GestoreNotifiche gestoreNotifiche;
    private GestoreClienti gestoreClienti;

    private Cliente clienteConNotifiche;      //Accetta tutte le notifiche
    private Cliente clienteSenzaNotifiche;    //Non vuole notifiche
    private Cliente clienteSoloViaggio;       //Solo notifiche treno e viaggio, no promo

    private List<String> clientiDaPulire = new ArrayList<>();
    private List<String> notificheDaPulire = new ArrayList<>();

    //Listener personalizzato per test
    private TestNotificheListener testListener;

    @BeforeAll
    void setupAll() throws Exception
    {
        System.out.println("\n=== INIZIO TEST GESTORE NOTIFICHE ===");

        pulisciDatabaseCompleto();

        gestoreNotifiche = GestoreNotifiche.getInstance();
        gestoreClienti = GestoreClienti.getInstance();

        setupClientiTest();

        testListener = new TestNotificheListener();
    }

    private void setupClientiTest()
    {
        clienteConNotifiche = new Cliente.Builder()
                .ID("NOTIF_ALL_" + System.currentTimeMillis())
                .Nome("Mario")
                .Cognome("Rossi")
                .Email("mario.notifiche@test.it")
                .Password("pass123")
                .isFedelta(true)
                .riceviNotifiche(true)
                .riceviPromozioni(true)
                .build();

        DatiBancariDTO datiBancari1 = new DatiBancariDTO(
                clienteConNotifiche.getId(),
                "Mario",
                "Rossi",
                "1234-5678-9012-3456"
        );

        clienteSenzaNotifiche = new Cliente.Builder()
                .ID("NOTIF_NONE_" + System.currentTimeMillis())
                .Nome("Luigi")
                .Cognome("Verdi")
                .Email("luigi.nonotif@test.it")
                .Password("pass456")
                .isFedelta(false)
                .riceviNotifiche(false)
                .riceviPromozioni(false)
                .build();

        DatiBancariDTO datiBancari2 = new DatiBancariDTO(
                clienteSenzaNotifiche.getId(),
                "Luigi",
                "Verdi",
                "1434-5678-9612-3456"
        );

        clienteSoloViaggio = new Cliente.Builder()
                .ID("NOTIF_VIAG_" + System.currentTimeMillis())
                .Nome("Anna")
                .Cognome("Bianchi")
                .Email("anna.viaggio@test.it")
                .Password("pass789")
                .isFedelta(true)
                .riceviNotifiche(true)
                .riceviPromozioni(false) // NO promozioni
                .build();

        DatiBancariDTO datiBancari3 = new DatiBancariDTO(
                clienteSoloViaggio.getId(),
                "Anna",
                "Bianchi",
                "3234-5678-9012-3456"
        );

        gestoreClienti.aggiungiCliente(clienteConNotifiche, datiBancari1);
        gestoreClienti.aggiungiCliente(clienteSenzaNotifiche, datiBancari2);
        gestoreClienti.aggiungiCliente(clienteSoloViaggio, datiBancari3);

        clientiDaPulire.add(clienteConNotifiche.getId());
        clientiDaPulire.add(clienteSenzaNotifiche.getId());
        clientiDaPulire.add(clienteSoloViaggio.getId());
    }

    @AfterEach
    void cleanup() throws SQLException
    {
        if (testListener != null)
        {
            testListener.reset();
        }

        for (String idNotifica : notificheDaPulire) {
            rimuoviNotificaDaDB(idNotifica);
        }
        notificheDaPulire.clear();
    }

    @AfterAll
    void tearDownAll() throws SQLException
    {
        pulisciDatabaseCompleto();
        System.out.println("=== TEST GESTORE NOTIFICHE COMPLETATI ===\n");
    }

    @Test
    @Order(1)
    @DisplayName("Invio notifica normale a cliente con preferenze attive")
    void testInvioNotificaNormale() {
        String messaggio = "Il tuo viaggio Roma-Milano è in partenza dal binario 5";
        NotificaDTO notifica = new NotificaDTO(messaggio);

        gestoreNotifiche.inviaNotifica(clienteConNotifiche.getId(), notifica);

        List<NotificaDTO> notificheNonLette =
                gestoreNotifiche.getNotificheNonLette(clienteConNotifiche.getId());

        assertEquals(1, notificheNonLette.size(),
                "Dovrebbe esserci 1 notifica non letta");
        assertEquals(messaggio, notificheNonLette.get(0).getMessaggio());

        List<NotificaDTO> secondaLettura =
                gestoreNotifiche.getNotificheNonLette(clienteConNotifiche.getId());
        assertTrue(secondaLettura.isEmpty(),
                "Dopo la lettura non ci dovrebbero essere notifiche non lette");
    }

    @Test
    @Order(2)
    @DisplayName("Notifica non inviata a cliente senza preferenze")
    void testNotificaNonInviataClienteSenzaPreferenze()
    {
        gestoreNotifiche.registraListener(clienteSenzaNotifiche.getId(), testListener);

        NotificaDTO notifica = new NotificaDTO("Tentativo di notifica");
        gestoreNotifiche.inviaNotifica(clienteSenzaNotifiche.getId(), notifica);

        assertFalse(testListener.haRicevutoNotifiche(),
                "Il listener non dovrebbe aver ricevuto notifiche");

        List<NotificaDTO> notificheNonLette =
                gestoreNotifiche.getNotificheNonLette(clienteSenzaNotifiche.getId());
        assertTrue(notificheNonLette == null || notificheNonLette.isEmpty(),
                "Non dovrebbero esserci notifiche per questo cliente");
    }

    @Test
    @Order(3)
    @DisplayName("Notifica promozionale solo a chi le accetta")
    void testNotificaPromozionaleSelettiva() {
        String messaggioPromo = "Sconto del 20% su tutti i viaggi ITALO!";
        NotificaDTO notificaPromo = new NotificaDTO(messaggioPromo);

        gestoreNotifiche.inviaNotificaPromozionale(clienteConNotifiche.getId(), notificaPromo);
        List<NotificaDTO> notificheConPromo =
                gestoreNotifiche.getNotificheNonLette(clienteConNotifiche.getId());
        assertEquals(1, notificheConPromo.size(),
                "Cliente con promo attive dovrebbe ricevere la notifica");

        gestoreNotifiche.inviaNotificaPromozionale(clienteSoloViaggio.getId(), notificaPromo);
        List<NotificaDTO> notificheSenzaPromo =
                gestoreNotifiche.getNotificheNonLette(clienteSoloViaggio.getId());
        assertTrue(notificheSenzaPromo == null || notificheSenzaPromo.isEmpty(),
                "Cliente senza promo non dovrebbe ricevere notifiche promozionali");
    }

    @Test
    @Order(4)
    @DisplayName("Test listener real-time per notifiche")
    void testListenerRealTime()
    {
        gestoreNotifiche.registraListener(clienteConNotifiche.getId(), testListener);

        String messaggio = "Ritardo di 10 minuti per il tuo treno";
        NotificaDTO notifica = new NotificaDTO(messaggio);
        gestoreNotifiche.inviaNotifica(clienteConNotifiche.getId(), notifica);

        assertTrue(testListener.haRicevutoNotifiche(),
                "Il listener dovrebbe aver ricevuto la notifica");
        assertEquals(1, testListener.getNotificheRicevute().size());
        assertEquals(messaggio, testListener.getNotificheRicevute().get(0).getMessaggio());
    }

    @Test
    @Order(5)
    @DisplayName("Test listener globale per tutte le notifiche")
    void testListenerGlobale()
    {
        TestNotificheListener listenerGlobale = new TestNotificheListener();
        gestoreNotifiche.registraListenerGlobale(listenerGlobale);

        try
        {
            NotificaDTO notifica1 = new NotificaDTO("Notifica cliente 1");
            NotificaDTO notifica2 = new NotificaDTO("Notifica cliente 2");

            gestoreNotifiche.inviaNotifica(clienteConNotifiche.getId(), notifica1);
            gestoreNotifiche.inviaNotifica(clienteSoloViaggio.getId(), notifica2);

            assertEquals(2, listenerGlobale.getNotificheRicevute().size(),
                    "Il listener globale dovrebbe ricevere tutte le notifiche");

        }
        finally
        {
            gestoreNotifiche.rimuoviListenerGlobale(listenerGlobale);
        }
    }

    @Test
    @Order(6)
    @DisplayName("Storico notifiche persiste nel database")
    void testStoricoNotifichePersistenza() throws SQLException
    {
        String messaggio = "Promo test per persistenza";
        NotificaDTO notifica = new NotificaDTO(messaggio);
        gestoreNotifiche.inviaNotificaPromozionale(clienteConNotifiche.getId(), notifica);

        gestoreNotifiche.getNotificheNonLette(clienteConNotifiche.getId());

        List<NotificaDTO> storico = gestoreNotifiche.getStoricoNotifiche(clienteConNotifiche.getId());

        assertTrue(storico.size() > 0, "Lo storico dovrebbe contenere almeno una notifica");

        boolean trovato = storico.stream()
                .anyMatch(n -> n.getMessaggio().equals(messaggio));
        assertTrue(trovato, "Il messaggio dovrebbe essere nello storico");

        assertTrue(verificaNotificaNelDB(clienteConNotifiche.getId(), messaggio),
                "La notifica dovrebbe essere salvata nel database");
    }

    @Test
    @Order(7)
    @DisplayName("Gestione notifica a cliente non esistente")
    void testNotificaClienteNonEsistente() {
        String idClienteFake = "CLIENTE_INESISTENTE_" + System.currentTimeMillis();

        assertDoesNotThrow(() -> {
            NotificaDTO notifica = new NotificaDTO("Test");
            gestoreNotifiche.inviaNotifica(idClienteFake, notifica);
        }, "Non dovrebbe lanciare eccezioni per cliente inesistente");

        List<NotificaDTO> notifiche = gestoreNotifiche.getNotificheNonLette(idClienteFake);
        assertTrue(notifiche == null || notifiche.isEmpty(),
                "Non dovrebbero esserci notifiche per cliente inesistente");
    }

    @Test
    @Order(8)
    @DisplayName("Conteggio notifiche non lette")
    void testConteggioNotificheNonLette()
    {
        for (int i = 1; i <= 3; i++)
        {
            NotificaDTO notifica = new NotificaDTO("Notifica numero " + i);
            gestoreNotifiche.inviaNotifica(clienteConNotifiche.getId(), notifica);
            System.out.println("Invio la notifica ("+ notifica+") al cliente "+ clienteConNotifiche.getId());
        }

        int conteggio = gestoreNotifiche.contaNotificheNonLette(clienteConNotifiche.getId());
        assertEquals(3, conteggio, "Dovrebbero esserci 3 notifiche non lette");

        gestoreNotifiche.getNotificheNonLette(clienteConNotifiche.getId());

        conteggio = gestoreNotifiche.contaNotificheNonLette(clienteConNotifiche.getId());
        assertEquals(0, conteggio, "Dopo la lettura non dovrebbero esserci notifiche non lette");
    }

    @Test
    @Order(9)
    @DisplayName("Gestione errori nel listener")
    void testGestioneErroriListener()
    {
        // Crea un listener che lancia eccezione
        NotificheListener listenerErrore = new NotificheListener(false)
        {
            @Override
            public void onNuovaNotifica(String idCliente, NotificaDTO notifica) {
                throw new RuntimeException("Errore simulato nel listener");
            }
        };

        gestoreNotifiche.registraListener(clienteConNotifiche.getId(), listenerErrore);

        assertDoesNotThrow(() -> {
            NotificaDTO notifica = new NotificaDTO("Test con errore");
            gestoreNotifiche.inviaNotifica(clienteConNotifiche.getId(), notifica);
        }, "L'errore nel listener non dovrebbe propagarsi");

        List<NotificaDTO> notifiche = gestoreNotifiche.getNotificheNonLette(clienteConNotifiche.getId());
        assertEquals(1, notifiche.size(),
                "La notifica dovrebbe essere stata aggiunta nonostante l'errore del listener");
    }

    @Test
    @Order(10)
    @DisplayName("Thread safety con accessi concorrenti")
    void testThreadSafety() throws InterruptedException
    {
        final int NUM_THREADS = 5;
        final int NOTIFICHE_PER_THREAD = 10;

        Thread[] threads = new Thread[NUM_THREADS];

        for (int i = 0; i < NUM_THREADS; i++)
        {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < NOTIFICHE_PER_THREAD; j++) {
                    NotificaDTO notifica = new NotificaDTO(
                            "Notifica thread " + threadNum + " num " + j
                    );
                    gestoreNotifiche.inviaNotifica(clienteConNotifiche.getId(), notifica);
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        List<NotificaDTO> tutteNotifiche =
                gestoreNotifiche.getNotificheNonLette(clienteConNotifiche.getId());

        assertEquals(NUM_THREADS * NOTIFICHE_PER_THREAD, tutteNotifiche.size(),
                "Tutte le notifiche dovrebbero essere state aggiunte correttamente");
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

    private void rimuoviNotificaDaDB(String idNotifica) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnessioneADB.getConnection();
            String sql = "DELETE FROM notifiche WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, idNotifica);
                stmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private boolean verificaNotificaNelDB(String idCliente, String messaggio) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnessioneADB.getConnection();
            String sql = "SELECT COUNT(*) FROM notifiche WHERE cliente_id = ? AND messaggio = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, idCliente);
                stmt.setString(2, messaggio);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } finally {
            ConnessioneADB.closeConnection(conn);
        }
        return false;
    }
}

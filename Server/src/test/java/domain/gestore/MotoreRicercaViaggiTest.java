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
import java.util.List;
import java.util.Calendar;
import java.util.ArrayList;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MotoreRicercaViaggiTest {

    private MotoreRicercaViaggi motoreRicerca;
    private GestoreViaggi gestoreViaggi;
    private GestoreClienti gestoreClienti;

    private Stazione stazioneRoma;
    private Stazione stazioneMilano;
    private Stazione stazioneNapoli;
    private List<String> viaggiDaPulire = new ArrayList<>();

    @BeforeAll
    void setupAll() throws Exception {
        System.out.println("\n=== INIZIO TEST MOTORE RICERCA VIAGGI ===");

        pulisciDatabaseCompleto();

        motoreRicerca = MotoreRicercaViaggi.getInstance();
        gestoreViaggi = GestoreViaggi.getInstance();
        gestoreClienti = GestoreClienti.getInstance();

        ArrayList<Integer> binari = new ArrayList<>();
        binari.add(1);
        binari.add(2);
        binari.add(3);
        binari.add(4);
        binari.add(5);

        stazioneRoma = new Stazione("Roma", "Roma Termini TEST", binari, 41.9028, 12.4964);
        stazioneMilano = new Stazione("Milano", "Milano Centrale TEST", binari, 45.4642, 9.1900);
        stazioneNapoli = new Stazione("Napoli", "Napoli Centrale TEST", binari, 40.8518, 14.2681);

        gestoreViaggi.aggiungiStazione(stazioneRoma);
        gestoreViaggi.aggiungiStazione(stazioneMilano);
        gestoreViaggi.aggiungiStazione(stazioneNapoli);

        Tratta trattaRomaMilano = new Tratta(stazioneRoma, stazioneMilano);
        Tratta trattaMilanoRoma = new Tratta(stazioneMilano, stazioneRoma);
        Tratta trattaRomaNapoli = new Tratta(stazioneRoma, stazioneNapoli);

        gestoreViaggi.aggiungiTratta(trattaRomaMilano);
        gestoreViaggi.aggiungiTratta(trattaMilanoRoma);
        gestoreViaggi.aggiungiTratta(trattaRomaNapoli);

        gestoreViaggi.aggiungiTreno("TR_TEST_REG_001", TipoTreno.INTERCITY); //treno1
        gestoreViaggi.aggiungiTreno("TR_TEST_AV_001", TipoTreno.ITALO); //treno2

        Calendar domani = Calendar.getInstance();
        domani.add(Calendar.DAY_OF_MONTH, 1);
        domani.set(Calendar.HOUR_OF_DAY, 8);
        domani.set(Calendar.MINUTE, 0);

        // Viaggio 1: Roma‑Milano con treno Intercity alle 8:00
        Calendar partenzaReg1 = (Calendar) domani.clone();
        Calendar arrivoReg1 = (Calendar) partenzaReg1.clone();
        arrivoReg1.add(Calendar.HOUR_OF_DAY, 4); // Arrivo alle 12:00

        Viaggio viaggio1 = gestoreViaggi.programmaViaggio(
                "TR_TEST_REG_001",
                trattaRomaMilano.getId(),
                partenzaReg1,
                arrivoReg1
        );
        viaggio1.setStato(StatoViaggio.PROGRAMMATO);
        viaggiDaPulire.add(viaggio1.getId());

        System.out.println("Id Viaggio 1 = "+ viaggio1.getId());

    // Viaggio 2: Roma‑Milano con Italo alle 9:00
        Calendar partenzaAV1 = (Calendar) domani.clone();
        partenzaAV1.set(Calendar.HOUR_OF_DAY, 9);
        Calendar arrivoAV1 = (Calendar) partenzaAV1.clone();
        arrivoAV1.add(Calendar.HOUR_OF_DAY, 3); // Arrivo alle 12:00

        Viaggio viaggio2 = gestoreViaggi.programmaViaggio(
                "TR_TEST_AV_001",
                trattaRomaMilano.getId(),
                partenzaAV1,
                arrivoAV1
        );
        viaggio2.setStato(StatoViaggio.PROGRAMMATO);
        viaggiDaPulire.add(viaggio2.getId());

        System.out.println("Id Viaggio 2 = "+ viaggio2.getId());

    // Viaggio 3: Milano‑Roma (ritorno) — lo stesso AV riparte alle 14:00
        Calendar partenzaRitorno = (Calendar) arrivoAV1.clone();
        partenzaRitorno.add(Calendar.HOUR_OF_DAY, 2); // Parte alle 14:00
        Calendar arrivoRitorno = (Calendar) partenzaRitorno.clone();
        arrivoRitorno.add(Calendar.HOUR_OF_DAY, 3);   // Arrivo alle 17:00

        Viaggio viaggio3 = gestoreViaggi.programmaViaggio(
                "TR_TEST_AV_001",
                trattaMilanoRoma.getId(),
                partenzaRitorno,
                arrivoRitorno
        );
        viaggio3.setStato(StatoViaggio.PROGRAMMATO);
        viaggiDaPulire.add(viaggio3.getId());
        System.out.println("Id Viaggio 3 = "+ viaggio3.getId());

    // Viaggio 4: Roma‑Napoli con treno regionale alle 15:00
        Calendar partenzaReg2 = (Calendar) arrivoReg1.clone();
        partenzaReg2.add(Calendar.HOUR_OF_DAY, 3); // Parte alle 15:00
        Calendar arrivoReg2 = (Calendar) partenzaReg2.clone();
        arrivoReg2.add(Calendar.HOUR_OF_DAY, 2);   // Arrivo alle 17:00

        Viaggio viaggio4 = gestoreViaggi.programmaViaggio(
                "TR_TEST_REG_001",
                trattaRomaNapoli.getId(),
                partenzaReg2,
                arrivoReg2
        );
        viaggio4.setStato(StatoViaggio.PROGRAMMATO);
        viaggiDaPulire.add(viaggio4.getId());
        System.out.println("Id Viaggio 4 = "+ viaggio4.getId());
        System.out.println("Setup completato: create " + viaggiDaPulire.size() + " viaggi di test");

    }

    @AfterAll
    void tearDownAll() throws SQLException {
        pulisciDatabaseCompleto();
        System.out.println("\n=== TEST MOTORE RICERCA COMPLETATI ===");
    }

    @Test
    @Order(1)
    @DisplayName("Ricerca viaggi solo andata con filtro base")
    void testRicercaViaggiSoloAndata() {
        // Preparo il filtro per cercare viaggi Roma-Milano domani
        Calendar domani = Calendar.getInstance();
        domani.add(Calendar.DAY_OF_MONTH, 1);

        FiltroPasseggeri filtro = new FiltroPasseggeri(
                2,                              // numero passeggeri
                ClasseServizio.ECONOMY,         // classe servizio
                TipoTreno.INTERCITY,           // tipo treno
                domani,                        // data andata
                null,                          // data ritorno (null = solo andata)
                true,                          // solo andata
                "Roma",                        // città partenza
                "Milano"                       // città arrivo
        );

        // Eseguo la ricerca
        List<Viaggio> risultati = motoreRicerca.cercaViaggio(filtro);

        // Verifico i risultati
        assertNotNull(risultati, "La lista risultati non dovrebbe essere null");
        assertEquals(1, risultati.size(), "Dovrebbe trovare esattamente 1 viaggio regionale Roma-Milano");

        Viaggio viaggioTrovato = risultati.get(0);
        assertEquals("Roma", viaggioTrovato.getTratta().getStazionePartenza().getCitta());
        assertEquals("Milano", viaggioTrovato.getTratta().getStazioneArrivo().getCitta());
        assertEquals(TipoTreno.INTERCITY, viaggioTrovato.getTreno().getTipo());

        System.out.println("Trovato viaggio: " + viaggioTrovato.getTratta().getStazionePartenza().getCitta() +
                " -> " + viaggioTrovato.getTratta().getStazioneArrivo().getCitta());
    }

    @Test
    @Order(2)
    @DisplayName("Ricerca viaggi con filtro alta velocità")
    void testRicercaViaggiAltaVelocita() {
        Calendar domani = Calendar.getInstance();
        domani.add(Calendar.DAY_OF_MONTH, 1);

        FiltroPasseggeri filtro = new FiltroPasseggeri(
                1,
                ClasseServizio.BUSINESS,
                TipoTreno.ITALO,
                domani,
                null,
                true,
                "Roma",
                "Milano"
        );

        List<Viaggio> risultati = motoreRicerca.cercaViaggio(filtro);

        assertNotNull(risultati);
        assertEquals(1, risultati.size(), "Dovrebbe trovare 1 viaggio alta velocità");
        assertEquals(TipoTreno.ITALO, risultati.get(0).getTreno().getTipo());

        System.out.println("Trovato viaggio alta velocità con " +
                risultati.get(0).getPostiDisponibiliPerClasse(ClasseServizio.BUSINESS) +
                " posti disponibili in Business");
    }

    @Test
    @Order(3)
    @DisplayName("Ricerca viaggi andata e ritorno")
    void testRicercaViaggiAndataRitorno()
    {
        Calendar domani = Calendar.getInstance();
        domani.add(Calendar.DAY_OF_MONTH, 1);

        Calendar dopodomani = (Calendar) domani.clone();
        dopodomani.add(Calendar.DAY_OF_MONTH, 1);

        FiltroPasseggeri filtro = new FiltroPasseggeri(
                2,
                ClasseServizio.ECONOMY,
                TipoTreno.ITALO,
                domani,
                domani,  // data ritorno specificata
                false,        // andata e ritorno
                "Roma",
                "Milano"
        );

        List<Viaggio> risultati = motoreRicerca.cercaViaggio(filtro);
        for(Viaggio v : risultati)
        {
            System.out.println("Trovato viaggio : "+ v.getId());
        }

        assertNotNull(risultati);
        assertTrue(risultati.size() >= 2, "Dovrebbe trovare almeno 2 viaggi (andata + ritorno)");

        // Verifico che ci siano viaggi in entrambe le direzioni
        boolean trovataAndata = false;
        boolean trovatoRitorno = false;

        for (Viaggio v : risultati) {
            if (v.getTratta().getStazionePartenza().getCitta().equals("Roma") &&
                    v.getTratta().getStazioneArrivo().getCitta().equals("Milano")) {
                trovataAndata = true;
            } else if (v.getTratta().getStazionePartenza().getCitta().equals("Milano") &&
                    v.getTratta().getStazioneArrivo().getCitta().equals("Roma")) {
                trovatoRitorno = true;
            }
        }

        assertTrue(trovataAndata, "Dovrebbe trovare almeno un viaggio di andata");
        assertTrue(trovatoRitorno, "Dovrebbe trovare almeno un viaggio di ritorno");

        System.out.println("Trovati " + risultati.size() + " viaggi andata/ritorno");
    }

    @Test
    @Order(4)
    @DisplayName("Ricerca con destinazione non esistente")
    void testRicercaDestinazioneNonEsistente() {
        Calendar domani = Calendar.getInstance();
        domani.add(Calendar.DAY_OF_MONTH, 1);

        FiltroPasseggeri filtro = new FiltroPasseggeri(
                1,
                ClasseServizio.ECONOMY,
                TipoTreno.INTERCITY,
                domani,
                null,
                true,
                "Roma",
                "Palermo"  // Città non presente nei test
        );

        List<Viaggio> risultati = motoreRicerca.cercaViaggio(filtro);

        assertNotNull(risultati);
        assertTrue(risultati.isEmpty(), "Non dovrebbe trovare viaggi per destinazioni non esistenti");

        System.out.println("Correttamente non trovati viaggi per destinazione inesistente");
    }

    @Test
    @Order(5)
    @DisplayName("Ricerca con posti insufficienti")
    void testRicercaPostiInsufficienti() {
        Calendar domani = Calendar.getInstance();
        domani.add(Calendar.DAY_OF_MONTH, 1);

        //Richiedo più posti di quelli disponibili su un treno
        FiltroPasseggeri filtro = new FiltroPasseggeri(
                300,  //Numero molto alto di passeggeri
                ClasseServizio.ECONOMY,
                TipoTreno.INTERCITY,
                domani,
                null,
                true,
                "Roma",
                "Milano"
        );

        List<Viaggio> risultati = motoreRicerca.cercaViaggio(filtro);

        assertNotNull(risultati);
        assertTrue(risultati.isEmpty(), "Non dovrebbe trovare viaggi con posti insufficienti");

        System.out.println("✓ Correttamente filtrati viaggi con posti insufficienti");
    }

    @Test
    @Order(6)
    @DisplayName("Ricerca viaggi per diverse tratte")
    void testRicercaDiverseTratte() {
        Calendar domani = Calendar.getInstance();
        domani.add(Calendar.DAY_OF_MONTH, 1);

        //Test Roma-Napoli
        FiltroPasseggeri filtroRomaNapoli = new FiltroPasseggeri(
                1,
                ClasseServizio.ECONOMY,
                TipoTreno.INTERCITY,
                domani,
                null,
                true,
                "Roma",
                "Napoli"
        );

        List<Viaggio> risultatiRomaNapoli = motoreRicerca.cercaViaggio(filtroRomaNapoli);

        assertNotNull(risultatiRomaNapoli);
        assertEquals(1, risultatiRomaNapoli.size(), "Dovrebbe trovare 1 viaggio Roma-Napoli");
        assertEquals("Napoli", risultatiRomaNapoli.get(0).getTratta().getStazioneArrivo().getCitta());

        System.out.println("✓ Trovato viaggio Roma-Napoli");
    }

    //Metodi di utilità per pulizia database
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
}

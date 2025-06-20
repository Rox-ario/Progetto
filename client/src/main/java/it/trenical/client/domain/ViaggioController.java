package it.trenical.client.domain;

import it.trenical.client.singleton.SessioneCliente;
import it.trenical.server.domain.FiltroPasseggeri;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.TipoTreno;
import it.trenical.server.dto.ViaggioDTO;
import it.trenical.server.grpc.ControllerGRPC;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ViaggioController
{
    public List<ViaggioDTO> cercaViaggio(String cittaPartenza, String cittaArrivo,
                                         Calendar dataAndata, Calendar dataRitorno,
                                         int numeroPasseggeri, ClasseServizio classePreferita,
                                         TipoTreno tipoTrenoPreferito)
    {
        try
        {
            if (!validaParametriRicerca(cittaPartenza, cittaArrivo, dataAndata, numeroPasseggeri))
            {
                return new ArrayList<>();
            }

            //capisco se è solo andata
            boolean soloAndata = (dataRitorno == null);

            FiltroPasseggeri filtro = new FiltroPasseggeri(
                    numeroPasseggeri,
                    classePreferita,
                    tipoTrenoPreferito,
                    dataAndata,
                    dataRitorno,
                    soloAndata,
                    cittaPartenza.trim(),
                    cittaArrivo.trim()
            );

            List<ViaggioDTO> risultati = ControllerGRPC.cercaViaggio(filtro);

            if (risultati.isEmpty())
            {
                System.out.println("Nessun viaggio trovato per i criteri specificati");
                System.out.println("Prova a modificare date, classe di servizio o tipo treno");
            }
            else
            {
                System.out.println("Trovati " + risultati.size() + " viaggi disponibili:");
                mostraRiepilogoRisultati(risultati);
            }
            return risultati;
        }

        catch (Exception e)
        {
            System.err.println("Errore durante la ricerca viaggi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean acquistaBiglietto(String idViaggio, ClasseServizio classeScelta, int numeroBiglietti)
    {
        try
        {
            //verifico l'accesso sia effettuato
            if (!verificaAccesso())
            {
                return false;
            }

            if (idViaggio == null || idViaggio.trim().isEmpty()) {
                System.err.println("ID viaggio non valido");
                return false;
            }

            if (numeroBiglietti <= 0)
            {
                System.err.println("Numero biglietti deve essere maggiore di 0");
                return false;
            }

            String idCliente = SessioneCliente.getInstance().getIdClienteLoggato();

            // Per ora gestiamo un biglietto alla volta
            // (Se serve gestire multipli, modificare il ControllerGRPC)
            for (int i = 0; i < numeroBiglietti; i++) {
                ControllerGRPC.acquistaBiglietto(idViaggio, idCliente, classeScelta);
            }

            System.out.println("Acquisto completato con successo!");
            System.out.println("L'importo è stato addebitato sul tuo conto");

            return true;

        }
        catch (Exception e)
        {
            System.err.println("Errore durante l'acquisto: " + e.getMessage());
            System.out.println("Verifica che ci siano posti disponibili e che il tuo saldo sia sufficiente");
            return false;
        }
    }


    public boolean acquistaBiglietto(String idViaggio, ClasseServizio classeScelta)
    {
        return acquistaBiglietto(idViaggio, classeScelta, 1);
    }


    public void mostraDettagliViaggio(ViaggioDTO viaggio) {
        if (viaggio == null) {
            System.err.println("❌ Viaggio non valido");
            return;
        }

        System.out.println("\n📋 DETTAGLI VIAGGIO");
        System.out.println("════════════════════");
        System.out.println("🆔 ID: " + viaggio.getID());
        System.out.println("🚂 Treno: " + viaggio.getTreno().getTipo() + " (" + viaggio.getTreno().getID() + ")");
        System.out.println("📍 Da: " + viaggio.getCittaPartenza());
        System.out.println("📍 A: " + viaggio.getCittaArrivo());
        System.out.println("🕐 Partenza: " + formatCalendar(viaggio.getInizio()));
        System.out.println("🕕 Arrivo: " + formatCalendar(viaggio.getFine()));
        System.out.println("📊 Stato: " + viaggio.getStato());
        System.out.println("💺 Posti disponibili: " + viaggio.getPostiDisponibili());

        // Mostra info fedeltà se cliente loggato
        if (SessioneCliente.getInstance().isLoggato() &&
                SessioneCliente.getInstance().getClienteCorrente().isFedelta()) {
            System.out.println("⭐ Come cliente FedeltàTreno potresti avere sconti speciali!");
        }

        System.out.println("════════════════════\n");
    }

    // ================================================================
    // METODI PRIVATI DI UTILITÀ
    // ================================================================

    /**
     * Verifica se l'utente è loggato
     */
    private boolean verificaAccesso() {
        if (!SessioneCliente.getInstance().isLoggato()) {
            System.err.println("❌ Devi effettuare il login per acquistare biglietti!");
            System.out.println("💡 Usa il menu 'Accedi' per autenticarti");
            return false;
        }
        return true;
    }

    /**
     * Valida i parametri di ricerca base
     */
    private boolean validaParametriRicerca(String cittaPartenza, String cittaArrivo,
                                           Calendar dataAndata, int numeroPasseggeri) {
        if (cittaPartenza == null || cittaPartenza.trim().isEmpty()) {
            System.err.println("❌ Città di partenza non può essere vuota");
            return false;
        }

        if (cittaArrivo == null || cittaArrivo.trim().isEmpty()) {
            System.err.println("❌ Città di arrivo non può essere vuota");
            return false;
        }

        if (cittaPartenza.trim().equalsIgnoreCase(cittaArrivo.trim())) {
            System.err.println("❌ Città di partenza e arrivo devono essere diverse");
            return false;
        }

        if (dataAndata == null) {
            System.err.println("❌ Data di andata non può essere vuota");
            return false;
        }

        // Controlla che la data non sia nel passato
        Calendar oggi = Calendar.getInstance();
        if (dataAndata.before(oggi)) {
            System.err.println("❌ Non puoi cercare viaggi nel passato");
            return false;
        }

        if (numeroPasseggeri <= 0) {
            System.err.println("❌ Numero passeggeri deve essere maggiore di 0");
            return false;
        }

        return true;
    }

    /**
     * Mostra un riepilogo dei risultati di ricerca
     */
    private void mostraRiepilogoRisultati(List<ViaggioDTO> viaggi) {
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.println("│                    RISULTATI RICERCA                   │");
        System.out.println("└─────────────────────────────────────────────────────────┘");

        for (int i = 0; i < Math.min(viaggi.size(), 5); i++) { // Mostra max 5
            ViaggioDTO v = viaggi.get(i);
            System.out.printf("🚂 %s → %s | %s | Posti: %d%n",
                    v.getCittaPartenza(),
                    v.getCittaArrivo(),
                    formatCalendar(v.getInizio()),
                    v.getPostiDisponibili()
            );
        }

        if (viaggi.size() > 5) {
            System.out.println("... e altri " + (viaggi.size() - 5) + " viaggi");
        }

        System.out.println("💡 Seleziona un viaggio per vedere i dettagli completi");
    }

    /**
     * Formatta una data/ora per la visualizzazione
     */
    private String formatCalendar(Calendar cal) {
        if (cal == null) return "N/A";

        return String.format("%02d/%02d/%d %02d:%02d",
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE)
        );
    }
}

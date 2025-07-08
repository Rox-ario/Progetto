package it.trenical.client.domain;

import io.grpc.StatusRuntimeException;
import it.trenical.client.grpc.ServerProxy;
import it.trenical.client.singleton.SessioneCliente;
import it.trenical.grpc.*;
import it.trenical.server.domain.FiltroPasseggeri;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.TipoTreno;
import it.trenical.server.dto.ViaggioDTO;
import it.trenical.server.grpc.ViaggioServiceImpl;

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

            List<ViaggioDTO> risultati = ServerProxy.cercaViaggio(filtro);

            if (risultati.isEmpty())
            {
                System.out.println("Nessun viaggio trovato per i criteri specificati");
                System.out.println("Prova a modificare date, classe di servizio o tipo treno");
            }
            else
            {
                System.out.println("Trovati " + risultati.size() + " viaggi disponibili:");
                mostraRiepilogoRisultati(risultati, classePreferita);
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

            for (int i = 0; i < numeroBiglietti; i++)
            {
                ServerProxy.acquistaBiglietto(idViaggio, idCliente, classeScelta);
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


    public void mostraDettagliViaggio(ViaggioDTO viaggio, double prezzoBase)
    {
        if (viaggio == null)
        {
            System.err.println("Viaggio non valido");
            return;
        }
        System.out.println("---------------------\n");

        System.out.printf("id: %s |%s → %s | partenza: %s | arrivo: %s | posti: %d%n",
                viaggio.getID(),
                viaggio.getCittaPartenza(),
                viaggio.getCittaArrivo(),
                formatCalendar(viaggio.getInizio()),
                formatCalendar(viaggio.getFine()),
                viaggio.getPostiDisponibili());
        System.out.printf("\nPrezzo sulla base della classe scelta: €%.2f%n", prezzoBase);

        //mostro le info fedeltà se cliente è loggato
        if (SessioneCliente.getInstance().isLoggato() &&
                SessioneCliente.getInstance().getClienteCorrente().isFedelta()) {
            System.out.println("Come cliente FedeltàTreno potresti avere sconti speciali!");
        }
        System.out.println("---------------------\n");
    }

    private boolean verificaAccesso()
    {
        if (!SessioneCliente.getInstance().isLoggato())
        {
            System.err.println("Devi effettuare il login per acquistare biglietti!");
            System.out.println("Usa il menu 'Accedi' per autenticarti");
            return false;
        }
        return true;
    }

    private boolean validaParametriRicerca(String cittaPartenza, String cittaArrivo,
                                           Calendar dataAndata, int numeroPasseggeri)
    {
        if (cittaPartenza == null || cittaPartenza.trim().isEmpty())
        {
            System.err.println("Città di partenza non può essere vuota");
            return false;
        }

        if (cittaArrivo == null || cittaArrivo.trim().isEmpty())
        {
            System.err.println("Città di arrivo non può essere vuota");
            return false;
        }

        if (cittaPartenza.trim().equalsIgnoreCase(cittaArrivo.trim()))
        {
            System.err.println("Città di partenza e arrivo devono essere diverse");
            return false;
        }

        if (dataAndata == null)
        {
            System.err.println("Data di andata non può essere vuota");
            return false;
        }

        //Controllo che la data non sia nel passato
        Calendar oggi = Calendar.getInstance();
        if (dataAndata.before(oggi))
        {
            System.err.println("Non puoi cercare viaggi nel passato");
            return false;
        }

        if (numeroPasseggeri <= 0)
        {
            System.err.println("Numero passeggeri deve essere maggiore di 0");
            return false;
        }

        return true;
    }

    private void mostraRiepilogoRisultati(List<ViaggioDTO> viaggi, ClasseServizio classePreferita)
    {
        System.out.println("Risultati ricerca viaggio:\n");

        for (ViaggioDTO v : viaggi)
        {
            double kilometri = v.getKilometri();
            double aggiuntaTipo = v.getTipo().getAumentoPrezzo();
            double aggiuntaServizio = classePreferita.getCoefficienteAumentoPrezzo();

            double prezzoBase = kilometri * aggiuntaServizio * aggiuntaTipo;
            mostraDettagliViaggio(v, prezzoBase);
        }
    }

    public boolean seguiTreno(String trenoId)
    {
        try
        {
            String clienteId = SessioneCliente.getInstance().getClienteCorrente().getId();

            SeguiTrenoRequest request = SeguiTrenoRequest.newBuilder()
                    .setClienteId(clienteId)
                    .setTrenoId(trenoId)
                    .build();

            SeguiTrenoResponse response = stub.seguiTreno(request);

            if (response.getSuccess()) {
                System.out.println("\n" + response.getMessage());
                return true;
            } else {
                System.err.println("\nErrore: " + response.getMessage());
                return false;
            }
        } catch (StatusRuntimeException e) {
            System.err.println("\nErrore di comunicazione: " + e.getMessage());
            return false;
        }
    }

    public boolean smettiDiSeguireTreno(String trenoId) {
        try {
            String clienteId = SessioneCliente.getInstance().getClienteCorrente().getId();

            SmettiDiSeguireTrenoRequest request = SmettiDiSeguireTrenoRequest.newBuilder()
                    .setClienteId(clienteId)
                    .setTrenoId(trenoId)
                    .build();

            SmettiDiSeguireTrenoResponse response = stub.smettiDiSeguireTreno(request);

            if (response.getSuccess()) {
                System.out.println("\n" + response.getMessage());
                return true;
            } else {
                System.err.println("\nErrore: " + response.getMessage());
                return false;
            }
        } catch (StatusRuntimeException e) {
            System.err.println("\nErrore di comunicazione: " + e.getMessage());
            return false;
        }
    }

    public List<TrenoSeguitoInfo> getTreniSeguiti()
    {
        List<TrenoSeguitoInfo> treniSeguiti = new ArrayList<>();

        try {
            String clienteId = SessioneCliente.getInstance().getClienteCorrente().getId();

            GetTreniSeguitiRequest request = GetTreniSeguitiRequest.newBuilder()
                    .setClienteId(clienteId)
                    .build();

            GetTreniSeguitiResponse response = stub.getTreniSeguiti(request);

            for (int i = 0; i < response.getTreniIdsCount(); i++) {
                treniSeguiti.add(new TrenoSeguitoInfo(
                        response.getTreniIds(i),
                        i < response.getTreniTipiCount() ? response.getTreniTipi(i) : "Treno"
                ));
            }
        } catch (StatusRuntimeException e)
        {
            System.err.println("Errore nel recupero dei treni seguiti: " + e.getMessage());
        }

        return treniSeguiti;
    }

    private String formatCalendar(Calendar cal)
    {
        if (cal == null) return "N/A";

        return cal.get(Calendar.DAY_OF_MONTH)+"/"+(cal.get(Calendar.MONTH) + 1)+"/"+ cal.get(Calendar.YEAR)+
                " "+ cal.get(Calendar.HOUR_OF_DAY) + ":"+cal.get(Calendar.MINUTE);
    }
}

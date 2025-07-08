package it.trenical.server.domain.gestore;

import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.*;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoViaggio;
import it.trenical.server.domain.enumerations.TipoBinario;
import it.trenical.server.domain.enumerations.TipoTreno;
import it.trenical.server.dto.NotificaDTO;
import it.trenical.server.observer.ViaggioETreno.NotificatoreClienteViaggio;
import it.trenical.server.observer.ViaggioETreno.ObserverViaggio;
import org.checkerframework.checker.units.qual.C;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class GestoreViaggi {
    private static GestoreViaggi instance = null;

    private final Map<String, Treno> treni;
    private final Map<String, Tratta> tratte;
    private final Map<String, Viaggio> viaggi;
    private Map<String, Stazione> stazioni = new HashMap<>();
    private Map<String, Set<String>> clientiPerTreno;

    private GestoreViaggi()
    {
        this.treni = new HashMap<>();
        this.tratte = new HashMap<>();
        this.viaggi = new HashMap<>();
        this.clientiPerTreno = new HashMap<>();

        caricaDatiDaDB();
    }

    private void caricaDatiDaDB()
    {
        caricaStazioni(); //prima le stazioni (necessarie per le tratte)
        caricaTreni();
        caricaTratte();
        caricaViaggi();
        caricaIscrizioniTreniDaDB();

        System.out.println("GestoreViaggi: caricati " + treni.size() + " treni, " +
                tratte.size() + " tratte, " + viaggi.size() + " viaggi, "+ stazioni.size()+" stazioni dal database");
    }

    private void caricaIscrizioniTreniDaDB()
    {
        String sql = "SELECT * FROM stazioni";
        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next())
            {
                String clienteId = rs.getString("cliente_id");
                String trenoId = rs.getString("treno_id");

                if(!clientiPerTreno.containsKey(trenoId))
                {
                    clientiPerTreno.put(trenoId, new HashSet<String>());
                }
                clientiPerTreno.get(trenoId).add(clienteId);
            }
            System.out.println("Caricate iscrizioni treni dal database");
        } catch (SQLException e) {
            System.err.println("Errore nel caricamento iscrizioni treni: " + e.getMessage());
        }
    }

    private void caricaStazioni()
    {
        String sql = "SELECT * FROM stazioni";
        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next())
            {
                String id = rs.getString("id");
                String citta = rs.getString("citta");
                String nome = rs.getString("nome");
                double lat = rs.getDouble("latitudine");
                double lon = rs.getDouble("longitudine");
                ArrayList<Integer> binari = parseBinari(rs.getString("binari"));

                Stazione s = new Stazione(id, citta, nome, binari, lat, lon);
                stazioni.put(id, s);
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore caricamento stazioni: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private ArrayList<Integer> parseBinari(String binariStr)
    {
        ArrayList<Integer> binari = new ArrayList<>();
        if (binariStr != null && !binariStr.isEmpty())
        {
            StringTokenizer sb = new StringTokenizer(binariStr, ",");
            while(sb.hasMoreTokens())
            {
                try
                {
                    binari.add(Integer.parseInt(sb.nextToken()));
                }
                catch (NumberFormatException e)
                {
                    // Ignora valori non validi
                }
            }
        }
        return binari;
    }

    private void caricaTreni()
    {
        String sql = "SELECT * FROM treni";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next())
            {
                String id = rs.getString("id");
                TipoTreno tipo = TipoTreno.valueOf(rs.getString("tipo"));

                Treno t = new Treno(id, tipo);
                treni.put(id, t);
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore caricamento treni: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void caricaTratte()
    {
        String sql = "SELECT * FROM tratte";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next())
            {
                String id = rs.getString("id");
                String partenzaId = rs.getString("stazione_partenza_id");
                String arrivoId = rs.getString("stazione_arrivo_id");

                Stazione partenza = stazioni.get(partenzaId);
                Stazione arrivo = stazioni.get(arrivoId);

                if (partenza != null && arrivo != null)
                {
                    Tratta t = new Tratta(id, partenza, arrivo);
                    tratte.put(id, t);
                }
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore caricamento tratte: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void caricaViaggi()
    {
        String sql = "SELECT * FROM viaggi";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String id = rs.getString("id");
                String trenoId = rs.getString("treno_id");
                String trattaId = rs.getString("tratta_id");
                Timestamp partenza = rs.getTimestamp("orario_partenza");
                Timestamp arrivo = rs.getTimestamp("orario_arrivo");
                String stato = rs.getString("stato");
                int ritardo = rs.getInt("ritardo_minuti");
                int binarioPartenza = rs.getInt("binario_partenza");
                int binarioArrivo = rs.getInt("binario_arrivo");

                Calendar calPartenza = Calendar.getInstance();
                calPartenza.setTimeInMillis(partenza.getTime());
                Calendar calArrivo = Calendar.getInstance();
                calArrivo.setTimeInMillis(arrivo.getTime());

                Treno treno = treni.get(trenoId);
                Tratta tratta = tratte.get(trattaId);

                if (treno != null && tratta != null)
                {
                    Viaggio v = new Viaggio(id, calPartenza, calArrivo, treno, tratta);

                    //imposto il maledetto stato e il ritardo
                    if (stato != null)
                    {
                        v.setStato(StatoViaggio.valueOf(stato));
                    }
                    if (ritardo > 0)
                    {
                        v.aggiornaRitardo(ritardo);
                    }
                    v.setBinarioDiArrivo(binarioArrivo);
                    v.setBinarioDiPartenza(binarioPartenza);
                    viaggi.put(id, v);

                    //registro il viaggio nel GestoreBiglietti
                    GestoreBiglietti.getInstance().aggiungiViaggio(id);
                }
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore caricamento viaggi: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public static synchronized GestoreViaggi getInstance()
    {
        if (instance == null) {
            instance = new GestoreViaggi();
        }
        return instance;
    }

    public void aggiungiTreno(String id, TipoTreno tipo)
    {
        if (!treni.containsKey(id))
        {
            Treno t = new Treno(id, tipo);
            salvaTrenoInDB(t);
            treni.put(id, t);
        }
        else
            System.out.println("Treno " + id + " già esistente");
    }

    private void salvaTrenoInDB(Treno t)
    {
        String sql = "INSERT INTO treni (id, tipo) VALUES (?, ?)";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, t.getID());
            pstmt.setString(2, t.getTipo().name());

            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore salvataggio treno: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public void aggiungiTratta(Tratta tratta)
    {
        if (!tratte.containsKey(tratta.getId()))
        {
            //prima mi salvo le stazioni se non esistono
            salvaStazioneSeNonEsiste(tratta.getStazionePartenza());
            salvaStazioneSeNonEsiste(tratta.getStazioneArrivo());

            //Poi salvo la trarra
            salvaTrattaInDB(tratta);

            tratte.put(tratta.getId(), tratta);
        }
    }

    private void salvaStazioneSeNonEsiste(Stazione s)
    {
        String sqlCheck = "SELECT COUNT(*) FROM stazioni WHERE id = ?";
        String sqlInsert = "INSERT INTO stazioni (id, citta, nome, latitudine, longitudine, binari) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();

            //ovviamente prima si controlla se esiste
            PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheck);
            pstmtCheck.setString(1, s.getId());
            ResultSet rs = pstmtCheck.executeQuery();

            if (rs.next() && rs.getInt(1) == 0)
            {
                //caso 1: non esiste, la inserisco
                PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert);
                pstmtInsert.setString(1, s.getId());
                pstmtInsert.setString(2, s.getCitta());
                pstmtInsert.setString(3, s.getNome());
                pstmtInsert.setDouble(4, s.getLatitudine());
                pstmtInsert.setDouble(5, s.getLongitudine());
                pstmtInsert.setString(6, binariToString(s.getBinari()));

                pstmtInsert.executeUpdate();
                stazioni.put(s.getId(), s);
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore salvataggio stazione: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }


    private String binariToString(List<Integer> binari)
    {
        if (binari == null || binari.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for(Integer binario : binari)
        {
            if(i == binari.size()-1)
                sb.append(binario);
            else
                sb.append(binario+",");
            i += 1;
        }
        return sb.toString();
    }

    private void salvaTrattaInDB(Tratta t)
    {
        String sql = "INSERT INTO tratte (id, stazione_partenza_id, stazione_arrivo_id) VALUES (?, ?, ?)";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, t.getId());
            pstmt.setString(2, t.getStazionePartenza().getId());
            pstmt.setString(3, t.getStazioneArrivo().getId());

            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore salvataggio tratta: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private boolean trenoUsato(Treno t, Calendar inizio, Calendar fine)
    {
        for (String id : viaggi.keySet()) {
            Viaggio viaggio = viaggi.get(id);
            Treno trenoAssociato = viaggio.getTreno();
            Calendar dataInizio = viaggio.getInizioReale();
            Calendar dataFine = viaggio.getFineReale();
            if (t.equals(trenoAssociato))
            {
                if (inizio.before(dataFine) && fine.after(dataInizio))
                {
                    System.out.println("Treno già usato nel viaggio "+ viaggio.getId());
                    return true; //il treno è usato e si sobrappongono gli orari
                }
            }
        }
        return false;
    }

    public Viaggio programmaViaggio(String trenoId, String trattaId, Calendar inizio, Calendar fine)
    {
        if (!treni.containsKey(trenoId) || !tratte.containsKey(trattaId))
        {
            throw new IllegalArgumentException("Treno o tratta non trovati");
        }
        Treno treno = treni.get(trenoId);
        Tratta tratta = tratte.get(trattaId);
        if (inizio == null || fine == null)
            throw new IllegalArgumentException("Inizio o Fine sono null");
        //controllo che il treno non sia già usato in un altro viaggio
        if (trenoUsato(treno, inizio, fine))
        {
            throw new IllegalStateException("Treno già usato in un altro viaggio e nello stesso orario");
        }

        Stazione sPartenza = tratta.getStazionePartenza();
        Stazione sArrivo = tratta.getStazioneArrivo();
        int binPar = scegliBinarioDisponibile(sPartenza, inizio, fine, TipoBinario.PARTENZA);
        int binArr = scegliBinarioDisponibile(sArrivo,  inizio, fine, TipoBinario.ARRIVO);
        String idViaggio = UUID.randomUUID().toString();
        Viaggio v = new Viaggio(idViaggio, inizio, fine, treno, tratta);
        v.setBinarioDiArrivo(binPar);
        v.setBinarioDiArrivo(binArr);

        salvaViaggioInDB(v);
        viaggi.put(idViaggio, v);

        System.out.println("Viaggio programmato correttamente");

        GestoreBiglietti gb = GestoreBiglietti.getInstance();
        gb.aggiungiViaggio(v.getId());

        if (clientiPerTreno.containsKey(trenoId))
        {
            int clientiNotificati = 0;

            for (String clienteId : clientiPerTreno.get(trenoId))
            {
                Cliente cliente = GestoreClienti.getInstance().getClienteById(clienteId);

                if (cliente != null && cliente.isRiceviNotifiche())
                {
                    //registro come observer
                    ObserverViaggio notificatore = new NotificatoreClienteViaggio(cliente);
                    v.attach(notificatore);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    NotificaDTO notifica = new NotificaDTO(
                            "Nuovo viaggio programmato per il treno che segui!\n" +
                                    "Treno: " + v.getTreno().getTipo() + " (ID: " + trenoId + ")\n" +
                                    "Tratta: " + v.getTratta().getStazionePartenza().getCitta() +
                                    " -> " + v.getTratta().getStazioneArrivo().getCitta() + "\n" +
                                    "Partenza: " + sdf.format(v.getInizioReale()) + "\n" +
                                    "Arrivo: " + sdf.format(v.getFineReale())
                    );
                    GestoreNotifiche.getInstance().inviaNotifica(clienteId, notifica);
                    clientiNotificati++;
                }
            }

            System.out.println("Notificati " + clientiNotificati + " clienti per il nuovo viaggio del treno " + trenoId);
        }
        return v;
    }

    //Restituisce un binario libero nella stazione 's', fra 'inizio' e 'fine'.
    private int scegliBinarioDisponibile(Stazione s, Calendar inizio, Calendar fine, TipoBinario tipo)
    {
        for (int binario : s.getBinari())
        {
            boolean occupato = false;
            // Scorro tutti i viaggi già programmati
            for (Viaggio v : viaggi.values())
            {
                if(!v.getTratta().getStazionePartenza().equals(s) || !v.getTratta().getStazioneArrivo().equals(s))
                    continue;
                if (v.getBinario(tipo) != binario)
                    continue;
                Calendar vIn = v.getInizioReale();
                Calendar vFi = v.getFineReale();
                if (inizio.before(vFi) && fine.after(vIn))
                {
                    occupato = true;
                    break;
                }
            }

            if (!occupato)
            {
                return binario;
            }
        }

        throw new IllegalStateException(
                "Nessun binario libero in stazione " + s.getId() +
                        " fra " + inizio.getTime() + " e " + fine.getTime()
        );
    }


    private void salvaViaggioInDB(Viaggio v) 
    {
        String sql = "INSERT INTO viaggi (id, treno_id, tratta_id, orario_partenza, orario_arrivo, " +
                "stato, ritardo_minuti, binario_partenza, binario_arrivo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, v.getId());
            pstmt.setString(2, v.getTreno().getID());
            pstmt.setString(3, v.getTratta().getId());
            pstmt.setTimestamp(4, new Timestamp(v.getInizioReale().getTimeInMillis()));
            pstmt.setTimestamp(5, new Timestamp(v.getFineReale().getTimeInMillis()));
            pstmt.setString(6, v.getStato().name());
            pstmt.setInt(7, 0); //ritardo iniziale ovviamene 0
            pstmt.setInt(8, v.getBinario(TipoBinario.PARTENZA));
            pstmt.setInt(9, v.getBinario(TipoBinario.ARRIVO));

            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore salvataggio viaggio: " + e.getMessage());
            throw new RuntimeException("Impossibile salvare il viaggio", e);
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public void cambiaStatoViaggio(String idViaggio, StatoViaggio nuovoStato)
    {
        if (!viaggi.containsKey(idViaggio))
            throw new IllegalArgumentException("Viaggio " + idViaggio + " non presente");
        Viaggio v = viaggi.get(idViaggio);

        StatoViaggio vecchioStato = v.getStato(); //giusto per capire se va o meno

        v.setStato(nuovoStato);

        aggiornaStatoViaggioInDB(idViaggio, nuovoStato);

        System.out.println("Il viaggio " + idViaggio + " è stato aggiornato da " + vecchioStato + " a " + nuovoStato);
    }

    private void aggiornaStatoViaggioInDB(String idViaggio, StatoViaggio stato)
    {
        String sql = "UPDATE viaggi SET stato = ? WHERE id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, stato.name());
            pstmt.setString(2, idViaggio);

            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore aggiornamento stato viaggio: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public Viaggio getViaggio(String id) {
        if (!viaggi.containsKey(id))
            return null;
        return viaggi.get(id);
    }

    public Collection<Tratta> getTratte() {
        return tratte.values();
    }

    private boolean stessaData(Calendar data1, Calendar data2) {
        return data1.get(Calendar.DAY_OF_MONTH) == data2.get(Calendar.DAY_OF_MONTH)
                && data1.get(Calendar.MONTH) == data2.get(Calendar.MONTH)
                && data1.get(Calendar.YEAR) == data2.get(Calendar.YEAR);
    }

    public List<Viaggio> getViaggiPerData(Calendar inizioCercato) {
        List<Viaggio> res = new ArrayList<>();
        for (String idViaggio : viaggi.keySet()) {
            Viaggio v = viaggi.get(idViaggio);
            Calendar inizioViaggio = v.getInizioReale(); //aggiornato con i ritardi
            if (stessaData(inizioViaggio, inizioCercato)) {
                res.add(v);
            }
        }
        if (res.isEmpty()) {
            System.out.println("Non ci sono viaggi in queste date");
        }
        return res;
    }

    public void aggiornaRitardoViaggio(String idViaggio, int ritardoInMinuti)
    {
        if (!viaggi.containsKey(idViaggio))
            throw new IllegalArgumentException("Viaggio " + idViaggio + " non presente");
        Viaggio v = viaggi.get(idViaggio);

        aggiornaRitardoViaggioInDB(idViaggio, ritardoInMinuti);
        v.aggiornaRitardo(ritardoInMinuti);

        System.out.println("Ritardo del Viaggio " + idViaggio + " aggiornato");
    }

    private void aggiornaRitardoViaggioInDB(String idViaggio, int ritardo)
    {
        String sql = "UPDATE viaggi SET ritardo_minuti = ritardo_minuti + ? WHERE id = ?";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setInt(1, ritardo);
            pstmt.setString(2, idViaggio);

            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore aggiornamento ritardo viaggio: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private boolean viaggioInCorso(String idViaggio) {
        Viaggio v = viaggi.get(idViaggio);
        if (v.getStato() == (StatoViaggio.IN_CORSO)
                || v.getStato() == (StatoViaggio.IN_RITARDO))
            return true;
        return false;
    }

    public void rimuoviViaggio(String idViaggio) {
        if (!viaggi.containsKey(idViaggio))
            throw new IllegalArgumentException("Viaggio " + idViaggio + " non presente");
        if (viaggioInCorso(idViaggio))
            throw new IllegalStateException("Il Viaggio " + idViaggio + " è in corso e non può essere annullato");

        rimuoviViaggioDaDB(idViaggio);
        viaggi.remove(idViaggio);
    }

    private void rimuoviViaggioDaDB(String idViaggio)
    {
        String sql = "DELETE FROM viaggi WHERE id = ?";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, idViaggio);
            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore rimozione viaggio: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public Viaggio getViaggioPerTreno(String id) {
        for (String idViaggio : viaggi.keySet()) {
            Viaggio v = viaggi.get(idViaggio);
            Treno t = v.getTreno();
            if (t.getID().equals(id) && v.getStato() != (StatoViaggio.TERMINATO))
                return v;
        }
        throw new IllegalArgumentException("Nessun viaggio in corso per il treno " + id);
    }

    public List<Viaggio> getViaggiPerFiltro(FiltroPasseggeri filtroPasseggeri) {
        if (filtroPasseggeri.isSoloAndata())
            return getViaggiSoloAndata(filtroPasseggeri);
        else
            return getViaggiAndataERitorno(filtroPasseggeri);
    }

    private List<Viaggio> getViaggiSoloAndata(FiltroPasseggeri filtroPasseggeri) {
        //Tiro fuori i maledetti parametri
        int numeroPasseggeri = filtroPasseggeri.getNumero();
        ClasseServizio classeServizio = filtroPasseggeri.getClasseServizio();
        TipoTreno tipoTreno = filtroPasseggeri.getTipoTreno();
        Calendar dataInizio = filtroPasseggeri.getDataInizio();
        String cittaDiAndata = filtroPasseggeri.getCittaDiAndata();
        String cittaDiArrivo = filtroPasseggeri.getCittaDiArrivo();

        //Ottiengo i viaggi in quel giormo
        List<Viaggio> viaggiDelGiorno = getViaggiPerData(dataInizio);

        //Passo allo scanning, utilizzo una copia della lista così nel mentre posso modificare quella originaria
        for (Viaggio v : new ArrayList<Viaggio>(viaggiDelGiorno)) {
            int postiDisponibiliPerClasse = v.getPostiDisponibiliPerClasse(classeServizio);
            TipoTreno trenoUsato = v.getTreno().getTipo();
            String cittaAndata = v.getTratta().getStazionePartenza().getCitta();
            String cittaArrivo = v.getTratta().getStazioneArrivo().getCitta();
            if (postiDisponibiliPerClasse < numeroPasseggeri ||
                    trenoUsato != tipoTreno || !cittaAndata.equals(cittaDiAndata) || !cittaArrivo.equals(cittaDiArrivo))
                viaggiDelGiorno.remove(v);
        }

        return viaggiDelGiorno;
    }

    private List<Viaggio> getViaggiAndataERitorno(FiltroPasseggeri filtroPasseggeri)
    {
        int numeroPasseggeri = filtroPasseggeri.getNumero();
        ClasseServizio classeServizio = filtroPasseggeri.getClasseServizio();
        TipoTreno tipoTreno = filtroPasseggeri.getTipoTreno();
        Calendar dataRitorno = filtroPasseggeri.getDataRitorno();
        String cittaDiAndata = filtroPasseggeri.getCittaDiAndata();
        String cittaDiArrivo = filtroPasseggeri.getCittaDiArrivo();

        List<Viaggio> risultati = new ArrayList<>();

        //Per prima cosa cerco i viaggi di andata
        List<Viaggio> viaggiAndata = getViaggiSoloAndata(filtroPasseggeri);
        risultati.addAll(viaggiAndata);
        for(Viaggio v : viaggiAndata)
        {
            System.out.println("Viaggi andata: "+ v.getId() + " da "+ v.getTratta().getStazionePartenza().getCitta() + " a "
            + v.getTratta().getStazioneArrivo().getCitta());
        }

        // dopodiché ritorno i viaggi di ritorno, mi sa che devo creare un filtro di ritorno
        FiltroPasseggeri filtroRitorno = new FiltroPasseggeri(
                numeroPasseggeri, classeServizio, tipoTreno,
                dataRitorno, null, true, cittaDiArrivo, cittaDiAndata);
        List<Viaggio> viaggiRitorno = getViaggiSoloAndata(filtroRitorno);
        for(Viaggio v : viaggiRitorno)
        {
            System.out.println("Viaggi ritorno: "+ v.getId() + " da "+ v.getTratta().getStazionePartenza().getCitta() + " a "
                    + v.getTratta().getStazioneArrivo().getCitta());
        }
        risultati.addAll(viaggiRitorno);

        //TODO eventualmente ordinare la lista o restituire un HashMap
        return risultati;
    }

    public List<Treno> getTuttiITreni()
    {
        return new ArrayList<>(treni.values());
    }

    public Treno getTreno(String id)
    {
        if(id != null && treni.containsKey(id))
            return treni.get(id);
        else
            return null;
    }

    public Stazione getStazione(String id)
    {
        if(id == null || !stazioni.containsKey(id))
            return null;
        else
            return stazioni.get(id);
    }

    public void aggiungiStazione(Stazione stazione)
    {
        salvaStazioneSeNonEsiste(stazione);
    }

    public List<Stazione> getTutteLeStazioni()
    {
        return new ArrayList<>(stazioni.values());
    }

    public List<Viaggio> getViaggiPerStato(StatoViaggio statoViaggio)
    {
        List<Viaggio> res = new ArrayList<Viaggio>();
        for(Viaggio v : viaggi.values())
        {
            if(v.getStato() == statoViaggio)
                res.add(v);
        }
        return res;
    }

    public List<Viaggio> getViaggiPerData(Calendar da, Calendar a)
    {
        List<Viaggio> res = new ArrayList<>();
        for(Viaggio v : viaggi.values())
        {
            if(inMezzo(v, da, a))
                res.add(v);
        }
        return res;
    }

    private boolean inMezzo(Viaggio v, Calendar da, Calendar a)
    {
        Calendar dataInizio = v.getInizio();
        Calendar dataFine = v.getFine();
        return dataInizio.after(da) && dataFine.before(a);
    }

    public Tratta getTratta(String id)
    {
        if(id == null || !tratte.containsKey(id))
            return null;
        return tratte.get(id);
    }

    public void rimuoviTreno(String id)
    {
        if (!treni.containsKey(id))
            throw new IllegalArgumentException("Treno " + id + " non trovato");

        //verifico se il treno è utilizzato in almeno un viaggio
        for (Viaggio v : viaggi.values())
        {
            Treno t = v.getTreno();
            if (t != null && t.getID().equals(id))
            {
                throw new IllegalStateException("Il treno " + id + " è utilizzato nel viaggio " + v.getId() + " e non può essere rimosso");
            }
        }

        rimuoviTrenoDaDB(id);
        treni.remove(id);
        System.out.println("Treno " + id + " rimosso correttamente");
    }

    private void rimuoviTrenoDaDB(String idTreno)
    {
        String sql = "DELETE FROM treni WHERE id = ?";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, idTreno);
            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore rimozione treno dal DB: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public void rimuoviStazione(String idStazione)
    {
        if (!stazioni.containsKey(idStazione))
            throw new IllegalArgumentException("Stazione a" + idStazione + " non trovata");

        for (Tratta tratta : tratte.values())
        {
            Stazione partenza = tratta.getStazionePartenza();
            Stazione arrivo = tratta.getStazioneArrivo();

            if ((partenza != null && partenza.getId().equals(idStazione)) ||
                    (arrivo != null && arrivo.getId().equals(idStazione)))
            {
                throw new IllegalStateException("La stazione " + idStazione + " è utilizzata nella tratta " + tratta.getId() + " e non può essere rimossa");
            }
        }

        rimuoviStazioneDaDB(idStazione);

        System.out.println("Stazione " + idStazione + " rimossa correttamente");
    }

    private void rimuoviStazioneDaDB(String idStazione)
    {
        String sql = "DELETE FROM stazioni WHERE id = ?";
        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, idStazione);
            pstmt.executeUpdate();
            stazioni.remove(idStazione);
        }
        catch (SQLException e)
        {
            System.err.println("Errore rimozione stazione dal DB: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public void iscriviClienteATreno(String clienteId, String trenoId) throws Exception
    {
        Cliente cliente = GestoreClienti.getInstance().getClienteById(clienteId);
        if (cliente == null)
        {
            throw new Exception("Spiacente, cliente "+clienteId+" non trovato");
        }

        Treno treno = treni.get(trenoId);
        if (treno == null)
        {
            throw new Exception("Spiacente, treno "+trenoId+" non trovato");
        }

        //sono già iscritto al treno quindi è inutile riscrivermi
        if (clientiPerTreno.containsKey(trenoId) && clientiPerTreno.get(trenoId).contains(clienteId))
        {
            throw new Exception("Sei già iscritto a questo treno");
        }

        salvaRegistrazioneClienteATrenoNelDB(clienteId, trenoId);
        if(!clientiPerTreno.containsKey(trenoId))
        {
            clientiPerTreno.put(trenoId, new HashSet<String>());
        }
        clientiPerTreno.get(trenoId).add(clienteId);

        //registro per viaggi attuali programmati
        int viaggiRegistrati = 0;
        for (Viaggio viaggio : viaggi.values())
        {
            if (viaggio.getTreno().getID().equals(trenoId) &&
                    viaggio.getInizioReale().after(Calendar.getInstance()))
            {
                if (cliente.isRiceviNotifiche())
                {
                    ObserverViaggio notificatore = new NotificatoreClienteViaggio(cliente);
                    viaggio.attach(notificatore);
                    viaggiRegistrati++;
                }
            }
        }

        //notifico con una conferma
        if (cliente.isRiceviNotifiche())
        {
            NotificaDTO conferma = new NotificaDTO(
                    "Ti sei iscritto al treno " + treno.getTipo() + " (ID: " + trenoId + ").\n" +
                            "Sei stato registrato per " + viaggiRegistrati + " viaggi futuri.\n" +
                            "Riceverai aggiornamenti per tutti i viaggi di questo treno."
            );
            GestoreNotifiche.getInstance().inviaNotifica(clienteId, conferma);
        }
    }

    private void salvaRegistrazioneClienteATrenoNelDB(String clienteId, String trenoId)
    {
        //salvo nel DB
        String sql = "INSERT INTO iscrizioni_treni (cliente_id, treno_id) VALUES (?, ?)";
        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, clienteId);
            pstmt.setString(2, trenoId);
            pstmt.executeUpdate();
        }
        catch (Exception e)
        {
            System.err.println("E' stato impossibile salvare nel DB la regiostrazione del cliente "+ clienteId +" al treno " + trenoId);
        }
        finally {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public void rimuoviIscrizioneTreno(String clienteId, String trenoId) throws Exception
    {
        if (!clientiPerTreno.containsKey(trenoId) || !clientiPerTreno.get(trenoId).contains(clienteId))
        {
            throw new Exception("Caro cliente "+ clienteId+" ci duole informarti che non sei iscritto al treno scelto "+ trenoId);
        }

        rimuoviRegistrazioneClienteATrenoNelDB(clienteId, trenoId);

        clientiPerTreno.get(trenoId).remove(clienteId);
        if (clientiPerTreno.get(trenoId).isEmpty())
        {
            clientiPerTreno.remove(trenoId);
        }
    }

    private void rimuoviRegistrazioneClienteATrenoNelDB(String clienteId, String trenoId)
    {
        // Rimuovi dal database
        String sql = "DELETE FROM iscrizioni_treni WHERE cliente_id = ? AND treno_id = ?";
        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, clienteId);
            pstmt.setString(2, trenoId);
            pstmt.executeUpdate();
        }
        catch (Exception e)
        {
            System.err.println("Errore: non è stato possibile rimuovere l'iscrizione del cliente "+clienteId+" al treno "+ trenoId);
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public List<String> getTreniSeguitiDaCliente(String clienteId)
    {
        List<String> treniSeguiti = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : clientiPerTreno.entrySet())
        {
            if (entry.getValue().contains(clienteId))
            {
                treniSeguiti.add(entry.getKey());
            }
        }
        return treniSeguiti;
    }
}

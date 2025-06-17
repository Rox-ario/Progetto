package it.trenical.server.domain.gestore;

import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.*;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoViaggio;
import it.trenical.server.domain.enumerations.TipoTreno;

import java.sql.*;
import java.util.*;

public final class GestoreViaggi {
    private static GestoreViaggi instance = null;

    private final Map<String, Treno> treni;
    private final Map<String, Tratta> tratte;
    private final Map<String, Viaggio> viaggi;
    private Map<String, Stazione> stazioniTemp = new HashMap<>(); //per quando sto caricando le stazioni ma non ho nè viaggi nè treni

    private GestoreViaggi()
    {
        this.treni = new HashMap<>();
        this.tratte = new HashMap<>();
        this.viaggi = new HashMap<>();

        caricaDatiDaDB();
    }

    private void caricaDatiDaDB()
    {
        caricaStazioni(); //prima le stazioni (necessarie per le tratte)
        caricaTreni();
        caricaTratte();
        caricaViaggi();

        System.out.println("GestoreViaggi: caricati " + treni.size() + " treni, " +
                tratte.size() + " tratte, " + viaggi.size() + " viaggi dal database");
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
                stazioniTemp.put(id, s);
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

                Stazione partenza = stazioniTemp.get(partenzaId);
                Stazione arrivo = stazioniTemp.get(arrivoId);

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



    private boolean trenoUsato(Treno t, Calendar inizio, Calendar fine) {
        for (String id : viaggi.keySet()) {
            Viaggio viaggio = viaggi.get(id);
            Treno trenoAssociato = viaggio.getTreno();
            Calendar dataInizio = viaggio.getInizioReale();
            Calendar dataFine = viaggio.getFineReale();
            if (t.equals(trenoAssociato)) {
                if (inizio.before(dataFine) || fine.after(dataInizio))
                    return true; //il treno è usato e si sobrappongono gli orari
            }
        }
        return false;
    }

    public boolean programmaViaggio(String trenoId, String trattaId, Calendar inizio, Calendar fine) {
        if (!treni.containsKey(trenoId) || !tratte.containsKey(trattaId)) {
            throw new IllegalArgumentException("Treno o tratta non trovati");
        }
        Treno treno = treni.get(trenoId);
        Tratta tratta = tratte.get(trattaId);
        if (inizio == null || fine == null)
            throw new IllegalArgumentException("Inizio o Fine sono null");
        //controllo che il treno non sia già usato in un altro viaggio
        if (trenoUsato(treno, inizio, fine)) {
            throw new IllegalStateException("Treno già usato in un altro viaggio e nello stesso orario");
        }
        String idViaggio = UUID.randomUUID().toString();
        Viaggio v = new Viaggio(idViaggio, inizio, fine, treno, tratta);

        salvaViaggioInDB(v);
        viaggi.put(idViaggio, v);
        
        System.out.println("Viaggio programmato correttamente");

        GestoreBiglietti gb = GestoreBiglietti.getInstance();
        gb.aggiungiViaggio(v.getId());
        return true;
    }

    private void salvaViaggioInDB(Viaggio v) 
    {
        String sql = "INSERT INTO viaggi (id, treno_id, tratta_id, orario_partenza, orario_arrivo, " +
                "stato, ritardo_minuti) VALUES (?, ?, ?, ?, ?, ?, ?)";
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

    public Collection<Treno> getTreni() {
        return treni.values();
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

    private List<Viaggio> getViaggiAndataERitorno(FiltroPasseggeri filtroPasseggeri) {
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

        // dopodiché ritorno i viaggi di ritorno, mi sa che devo creare un filtro di ritorno
        FiltroPasseggeri filtroRitorno = new FiltroPasseggeri(
                numeroPasseggeri, classeServizio, tipoTreno,
                dataRitorno, null, true, cittaDiArrivo, cittaDiAndata);
        List<Viaggio> viaggiRitorno = getViaggiSoloAndata(filtroRitorno);
        risultati.addAll(viaggiRitorno);

        //TODO eventualmente ordinare la lista o restituire un HashMap
        return risultati;
    }
}

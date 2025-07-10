package it.trenical.server.domain.gestore;

import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.*;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.StatoPromozione;
import it.trenical.server.domain.enumerations.TipoPromozione;
import it.trenical.server.domain.enumerations.TipoTreno;
import it.trenical.server.observer.Promozione.ObserverPromozione;
import it.trenical.server.observer.Promozione.ObserverPromozioneFedelta;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class CatalogoPromozione
{
    private static CatalogoPromozione instance = null;

    private final Map<TipoPromozione, List<Promozione>> promozioniPerTipo;

    private CatalogoPromozione()
    {
        promozioniPerTipo = new HashMap<>();
        promozioniPerTipo.put(TipoPromozione.FEDELTA, new ArrayList<>());
        promozioniPerTipo.put(TipoPromozione.TRATTA, new ArrayList<>());
        promozioniPerTipo.put(TipoPromozione.TRENO, new ArrayList<>());

        caricaPromozioniDaDB();
    }

    private void caricaPromozioniDaDB() {
        String sql = "SELECT * FROM promozioni";
        Connection conn = null;
        try {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next())
            {
                Promozione promozione = creaPromozioneDaResultSet(rs);
                if (promozione != null)
                {
                    promozioniPerTipo.get(promozione.getTipo()).add(promozione);

                    //registo observer SOLO all'avvio e non notifico
                    if (promozione.getTipo() == TipoPromozione.FEDELTA && promozione.isAttiva())
                    {
                        registraObserverPromozioneFedeltaSenzaNotifica((PromozioneFedelta) promozione);
                    }
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println(e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private Promozione creaPromozioneDaResultSet(ResultSet rs) throws SQLException
    {
        String id = rs.getString("id");
        String tipo = rs.getString("tipo");
        Date dataInizio = rs.getDate("data_inizio");
        Date dataFine = rs.getDate("data_fine");
        double percentualeSconto = rs.getDouble("percentuale_sconto");
        String stato = rs.getString("stato");

        //converto Date in Calendar
        Calendar calInizio = Calendar.getInstance();
        calInizio.setTime(dataInizio);
        Calendar calFine = Calendar.getInstance();
        calFine.setTime(dataFine);

        Promozione promozione = null;

        switch (TipoPromozione.valueOf(tipo))
        {
            case FEDELTA:
                promozione = new PromozioneFedelta(id, calInizio, calFine, percentualeSconto);
                break;

            case TRATTA:
                String trattaId = rs.getString("tratta_id");
                Tratta tratta = recuperaTratta(trattaId);
                if (tratta != null)
                {
                    promozione = new PromozioneTratta(id, tratta, calInizio, calFine, percentualeSconto);
                }
                break;

            case TRENO:
                String tipoTreno = rs.getString("tipo_treno");
                if (tipoTreno != null)
                {
                    promozione = new PromozioneTreno(id, calInizio, calFine, percentualeSconto,
                            TipoTreno.valueOf(tipoTreno));
                }
                break;
        }

        //ultimo step, imposto lo stato
        if (promozione != null && "ATTIVA".equals(stato))
        {
            promozione.setStatoPromozioneATTIVA();
        }
        return promozione;
    }

    private Tratta recuperaTratta(String trattaId)
    {
        String sql = "SELECT t.*, sp.citta as citta_partenza, sp.nome as nome_partenza, " +
                "sp.latitudine as lat_partenza, sp.longitudine as lon_partenza, " +
                "sp.binari as binari_partenza, " +
                "sa.citta as citta_arrivo, sa.nome as nome_arrivo, " +
                "sa.latitudine as lat_arrivo, sa.longitudine as lon_arrivo, " +
                "sa.binari as binari_arrivo " +
                "FROM tratte t " +
                "JOIN stazioni sp ON t.stazione_partenza_id = sp.id " +
                "JOIN stazioni sa ON t.stazione_arrivo_id = sa.id " +
                "WHERE t.id = ?";

        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, trattaId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next())
            {
                //creo le stazioni con i binari parsati dalla che costituisce l'attrivuto binari
                Stazione partenza = new Stazione(
                        rs.getString("stazione_partenza_id"),
                        rs.getString("citta_partenza"),
                        rs.getString("nome_partenza"),
                        parseBinari(rs.getString("binari_partenza")),
                        rs.getDouble("lat_partenza"),
                        rs.getDouble("lon_partenza")
                );

                Stazione arrivo = new Stazione(
                        rs.getString("stazione_arrivo_id"),
                        rs.getString("citta_arrivo"),
                        rs.getString("nome_arrivo"),
                        parseBinari(rs.getString("binari_arrivo")),
                        rs.getDouble("lat_arrivo"),
                        rs.getDouble("lon_arrivo")
                );

                return new Tratta(trattaId, partenza, arrivo);
            }

        }
        catch (SQLException e)
        {
            System.err.println(e.getMessage());
        } finally
        {
            ConnessioneADB.closeConnection(conn);
        }
        return null;
    }

    private ArrayList<Integer> parseBinari(String binariStr)
    {
        ArrayList<Integer> binari = new ArrayList<>();
        if (binariStr != null && !binariStr.isEmpty())
        {
            StringTokenizer sb = new StringTokenizer(binariStr, ",");
            while(sb.hasMoreTokens())
            {
                binari.add(Integer.parseInt(sb.nextToken()));
            }
        }
        return binari;
    }

    public static synchronized CatalogoPromozione getInstance() {
        if (instance == null) {
            instance = new CatalogoPromozione();
        }
        return instance;
    }

    private boolean verificaSovrapposizione(Promozione nuovaPromo)
    {
        List<Promozione> promozioniStessoTipo = promozioniPerTipo.get(nuovaPromo.getTipo());

        System.out.println("Promo da esaminare = "+ nuovaPromo.getID() + ", "+ nuovaPromo.getTipo());
        System.out.println("Tutte le promo create:");
        for (Promozione esistente : promozioniStessoTipo)
        {
            System.out.println("Promo = " + esistente.getID() + ", " + esistente.getTipo());
        }
        for (Promozione esistente : promozioniStessoTipo)
        {
            if (sovrapponeTemporalmente(nuovaPromo, esistente))
            {
                System.out.println("La promo esistente " + esistente.getID() +
                        " si sovrappone con nuova promo "+ nuovaPromo.getID());
                //in base al tipo verifico
                if (nuovaPromo.getTipo() == TipoPromozione.FEDELTA && nuovaPromo.getTipo() == esistente.getTipo())
                {
                    System.out.println("La promo esistente " + esistente.getID() +
                            " e la nuova promo "+ nuovaPromo.getID() +" sono di tipo Fedelta'");
                    return true;
                }
                else if (nuovaPromo.getTipo() == TipoPromozione.TRATTA)
                {
                    // Le promozioni tratta si sovrappongono solo se hanno la stessa tratta
                    PromozioneTratta nuova = (PromozioneTratta) nuovaPromo;
                    PromozioneTratta vecchia = (PromozioneTratta) esistente;
                    if (nuova.getTratta().equals(vecchia.getTratta())) {
                        return true;
                    }
                }
                else if (nuovaPromo.getTipo() ==TipoPromozione.TRENO)
                {
                    // Le promozioni treno si sovrappongono solo se hanno lo stesso tipo treno
                    PromozioneTreno nuova = (PromozioneTreno) nuovaPromo;
                    PromozioneTreno vecchia = (PromozioneTreno) esistente;
                    if (nuova.getTipoTreno() == vecchia.getTipoTreno())
                    {
                        System.out.println("Il tipo di treno di " + vecchia.getID() +
                                " e' "+ vecchia.getTipoTreno()+ " e quello della nuova promo "+ nuova.getID() +
                        " e' "+ nuova.getTipoTreno());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean sovrapponeTemporalmente(Promozione p1, Promozione p2)
    {
        Calendar inizioP1 = p1.getDataInizio();
        Calendar fineP1 = p1.getDataFine();
        Calendar inizioP2 = p2.getDataInizio();
        Calendar fineP2 = p2.getDataFine();

        //Due periodi si sovrappongono se:
        //L'inizio di uno è tra l'inizio e la fine dell'altro
        //La fine di uno è tra l'inizio e la fine dell'altro
        //Uno contiene completamente l'altro
        // 1) Inizio di p1 dentro p2
        if (inizioP1.compareTo(inizioP2) >= 0 && inizioP1.before(fineP2))
            return true;

        // 2) Fine di p1 dentro p2
        if (fineP1.after(inizioP2) && fineP1.compareTo(fineP2) <= 0)
            return true;

        // 3) p1 contiene completamente p2
        if (inizioP1.before(inizioP2) && fineP1.after(fineP2))
            return true;

        // 4) p2 contiene completamente p1  (opzionale, ma copre ogni scenario)
        if (inizioP2.before(inizioP1) && fineP2.after(fineP1))
            return true;

        return false;
    }

    public void aggiungiPromozione(Promozione p)
    {
        if(p == null)
            throw new IllegalArgumentException("Errore: la promozione inserita è null");
        if (verificaSovrapposizione(p))
            throw new IllegalArgumentException("Errore: la promozione si sovrappone con altre");

        salvaPromozioneInDB(p);
        System.out.println("Promozione "+ p.getID()+" salvata nel db");
        promozioniPerTipo.get(p.getTipo()).add(p);

        if (p.getTipo() == TipoPromozione.FEDELTA)
        {
            System.out.println("Registro gli observers a promozione fedelta': "+ p.getID());
            registraObserverPromozioneFedelta((PromozioneFedelta) p);
        }
        System.out.println("Promozione creata\nDettagli: "+ p.toString());
    }


    private void salvaPromozioneInDB(Promozione p)
    {
        String sql = "INSERT INTO promozioni (id, tipo, data_inizio, data_fine, percentuale_sconto, " +
                "stato, tratta_id, tipo_treno) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, p.getID());
            pstmt.setString(2, p.getTipo().name());
            pstmt.setDate(3, new java.sql.Date(p.getDataInizio().getTimeInMillis()));
            pstmt.setDate(4, new java.sql.Date(p.getDataFine().getTimeInMillis()));
            pstmt.setDouble(5, p.getPercentualeSconto());
            pstmt.setString(6, p.getStatoPromozione().name());

            //devo occuparmi del tipo... quindi inserire campi specifici
            if (p.getTipo() == TipoPromozione.TRATTA)
            {
                pstmt.setString(7, ((PromozioneTratta) p).getTratta().getId());
                pstmt.setNull(8, Types.VARCHAR);
            }
            else if (p.getTipo() ==  TipoPromozione.TRENO)
            {
                pstmt.setNull(7, Types.VARCHAR);
                pstmt.setString(8, ((PromozioneTreno) p).getTipoTreno().name());
            }
            else
            {
                pstmt.setNull(7, Types.VARCHAR);
                pstmt.setNull(8, Types.VARCHAR);
            }

            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore salvataggio promozione: " + e.getMessage());
            throw new RuntimeException("Impossibile salvare la promozione", e);
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public void aggiornaStatoPromozioneInDB(String promozioneId, StatoPromozione nuovoStato)
    {
        String sql = "UPDATE promozioni SET stato = ? WHERE id = ?";

        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, nuovoStato.name());
            pstmt.setString(2, promozioneId);

            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore aggiornamento stato promozione: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public void rimuoviPromozione(Promozione p)
    {
        if(p == null)
            throw new IllegalArgumentException("Errore: la promozione inserita è null");

        rimuoviPromozioneDaDB(p.getID());
        promozioniPerTipo.get(p.getTipo()).remove(p);
    }

    private void rimuoviPromozioneDaDB(String promozioneId)
    {
        String sql = "DELETE FROM promozioni WHERE id = ?";

        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, promozioneId);
            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore rimozione promozione: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public PromozioneTratta getPromozioneAttivaTratta(Tratta t) {
        String sql = "SELECT * FROM promozioni WHERE tipo = 'TRATTA' AND stato = 'ATTIVA' AND tratta_id = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, t.getId());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next())
            {
                PromozioneTratta promo = (PromozioneTratta) creaPromozioneDaResultSet(rs);
                System.out.println("Trovata promozione Tratta attiva: " + promo);
                return promo;
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore recupero promozione tratta attiva: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }

        return null;
    }

    private void ricaricaPromozioniTrattaDaDB()
    {
        String sql = "SELECT * FROM promozioni WHERE tipo = 'TRATTA' AND stato = 'ATTIVA'";
        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            //pulisc le promozioni tratta in memoria
            promozioniPerTipo.get(TipoPromozione.TRATTA).clear();

            while (rs.next())
            {
                Promozione promozione = creaPromozioneDaResultSet(rs);
                if (promozione != null)
                {
                    promozioniPerTipo.get(TipoPromozione.TRATTA).add(promozione);
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Errore ricaricamento promozioni tratta: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public PromozioneFedelta getPromozioneAttivaFedelta()
    {
        String sql = "SELECT * FROM promozioni WHERE tipo = 'FEDELTA' AND stato = 'ATTIVA' LIMIT 1";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next())
            {
                PromozioneFedelta promo = (PromozioneFedelta) creaPromozioneDaResultSet(rs);
                System.out.println("Trovata promozione Fedelta' attiva: " + promo);
                return promo;
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore recupero promozione fedeltà attiva: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }

        return null;
    }

    public List<Promozione> getPromozioniAttive()
    {
        ArrayList<Promozione> promozioni = new ArrayList<>();
        for(List<Promozione> lista : promozioniPerTipo.values())
        {
            for(Promozione p : lista)
            {
                System.out.println("Controllo promozione "+ p.getID()+", "+p.getTipo()+" sia attiva");
                if(p.isAttiva())
                {
                    System.out.println("Promozione "+ p.getID()+", "+p.getTipo()+" e' attiva");
                    promozioni.add(p);
                }
            }
        }
        return promozioni;
    }

    public PromozioneTreno getPromozioneAttivaPerTipoTreno(TipoTreno tipoTreno) {
        String sql = "SELECT * FROM promozioni WHERE tipo = 'TRENO' AND stato = 'ATTIVA' AND tipo_treno = ?";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, tipoTreno.name());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next())
            {
                PromozioneTreno promo = (PromozioneTreno) creaPromozioneDaResultSet(rs);
                System.out.println("Trovata promozione Treno attiva: " + promo);
                return promo;
            }

        }
        catch (SQLException e)
        {
            System.err.println("Errore recupero promozione treno attiva: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }

        return null;
    }

    private void ricaricaPromozioniTrenoDaDB()
    {
        String sql = "SELECT * FROM promozioni WHERE tipo = 'TRENO' AND stato = 'ATTIVA'";
        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            promozioniPerTipo.get(TipoPromozione.TRENO).clear();

            while (rs.next())
            {
                Promozione promozione = creaPromozioneDaResultSet(rs);
                if (promozione != null)
                {
                    promozioniPerTipo.get(TipoPromozione.TRENO).add(promozione);
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Errore ricaricamento promozioni fedeltà: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }


    private void registraObserverPromozioneFedelta(PromozioneFedelta promozione)
    {
        GestoreClienti gc = GestoreClienti.getInstance();
        List<Cliente> clientiFedelta = gc.getClientiFedeltaConNotifiche();

        System.out.println("Registro observer per NUOVA promozione: " + promozione.getID());
        for (Cliente cliente : clientiFedelta)
        {
            System.out.println("Cliente Fedelta': " + cliente.getId());
            ObserverPromozione observer = new ObserverPromozioneFedelta(cliente);
            promozione.attach(observer);
        }

        if (!clientiFedelta.isEmpty() && promozione.isAttiva())
        {
            promozione.notifica();
            System.out.println("Notifico i clienti sulla NUOVA promozione " + promozione.getID());
        }
    }

    public List<Promozione> getPromoPerTipo(TipoPromozione tipoPromozione)
    {
        return new ArrayList<>(promozioniPerTipo.get(tipoPromozione));
    }

    public List<Promozione> getTutteLePromozioni()
    {
        List<Promozione> promo = new ArrayList<>();
        for(TipoPromozione tipo : promozioniPerTipo.keySet())
        {
            promo.addAll(promozioniPerTipo.get(tipo));
        }
        return promo;
    }

    public void aggiornaStatoPromozioni()
    {
        Calendar oggi = Calendar.getInstance();

        for(Promozione p : getTutteLePromozioni())
        {
            //se la promozione è terminata (data fine passata)
            if(p.getDataFine().before(oggi)) {
                rimuoviPromozioneDaDB(p.getID());
                rimuoviPromozione(p);
                System.out.println("Promozione "+ p.toString()+ " rimossa da Promozioni in quanto TERMINATA");
            }
            //se la promozione dovrebbe essere attiva (iniziata ma non finita)
            else if(p.getDataInizio().before(oggi) || p.getDataInizio().equals(oggi))
            {
                if(p.getStatoPromozione() == StatoPromozione.PROGRAMMATA)
                {
                    StatoPromozione statoVecchio = p.getStatoPromozione();
                    p.setStatoPromozioneATTIVA();
                    aggiornaStatoPromozioneInDB(p.getID(), p.getStatoPromozione());
                    System.out.println("Stato della promozione "+ p.getID() + " aggiornato da "+statoVecchio +" a "+ p.getStatoPromozione());

                    //se è una promozione fedeltà che DIVENTA attiva, notifica
                    if (p.getTipo() == TipoPromozione.FEDELTA && statoVecchio == StatoPromozione.PROGRAMMATA)
                    {
                        // Prima registra gli observer se non sono già registrati
                        registraObserverPromozioneFedelta((PromozioneFedelta) p);
                        // Poi notifica
                        ((PromozioneFedelta) p).notifica();
                        System.out.println("Notifico i clienti: promozione " + p.getID() + " è ora ATTIVA");
                    }
                }
            }
        }
    }

    private void registraObserverPromozioneFedeltaSenzaNotifica(PromozioneFedelta promozione)
    {
        GestoreClienti gc = GestoreClienti.getInstance();
        List<Cliente> clientiFedelta = gc.getClientiFedeltaConNotifiche();

        System.out.println("Registro observer senza notificare per promozione esistente: " + promozione.getID());
        for (Cliente cliente : clientiFedelta)
        {
            System.out.println("Cliente Fedelta': " + cliente.getId());
            ObserverPromozione observer = new ObserverPromozioneFedelta(cliente);
            promozione.attach(observer);
        }
    }
}

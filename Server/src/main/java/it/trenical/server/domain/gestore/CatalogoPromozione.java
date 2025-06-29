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
        try
        {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next())
            {
                Promozione promozione = creaPromozioneDaResultSet(rs);
                if (promozione != null)
                {
                    //l'aggiungo alla mappa in memoria
                    promozioniPerTipo.get(promozione.getTipo()).add(promozione);

                    //Se è una promozione fedeltà attiva, devo registrare gli observers
                    if (promozione.getTipo() == TipoPromozione.FEDELTA && promozione.isAttiva())
                    {
                        registraObserverPromozioneFedelta((PromozioneFedelta) promozione);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } finally {
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
                promozione = new PromozioneFedelta(calInizio, calFine, percentualeSconto);
                break;

            case TRATTA:
                String trattaId = rs.getString("tratta_id");
                Tratta tratta = recuperaTratta(trattaId);
                if (tratta != null)
                {
                    promozione = new PromozioneTratta(tratta, calInizio, calFine, percentualeSconto);
                }
                break;

            case TRENO:
                String tipoTreno = rs.getString("tipo_treno");
                if (tipoTreno != null)
                {
                    promozione = new PromozioneTreno(calInizio, calFine, percentualeSconto,
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

        System.out.println("Promo da esaminare = "+ nuovaPromo.getID());
        System.out.println("Tutte le promo create:");
        for (Promozione esistente : promozioniStessoTipo)
        {
            System.out.println("Promo = " + esistente.getID());
        }
        for (Promozione esistente : promozioniStessoTipo)
        {
            System.out.println("Promo esistente = "+ esistente.getID());
            if (sovrapponeTemporalmente(nuovaPromo, esistente))
            {
                // in base al tipo verifico cose
                if (nuovaPromo.getTipo() == TipoPromozione.FEDELTA) {
                    // Le promozioni fedeltà non possono sovrapporsi MAI
                    PromozioneFedelta nuova = (PromozioneFedelta) nuovaPromo;
                    PromozioneFedelta vecchia = (PromozioneFedelta) esistente;
                    if(nuova.equals(vecchia))
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
                else if (nuovaPromo.getTipo() ==TipoPromozione.TRENO) {
                    // Le promozioni treno si sovrappongono solo se hanno lo stesso tipo treno
                    PromozioneTreno nuova = (PromozioneTreno) nuovaPromo;
                    PromozioneTreno vecchia = (PromozioneTreno) esistente;
                    if (nuova.getTipoTreno() == vecchia.getTipoTreno()) {
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
        return !(fineP1.before(inizioP2) || inizioP1.after(fineP2));
    }

    public void aggiungiPromozione(Promozione p)
    {
        if(p == null)
            throw new IllegalArgumentException("Errore: la promozione inserita è null");
        if (verificaSovrapposizione(p))
            throw new IllegalArgumentException("Errore: la promozione si sovrappone con altre nel periodo: "+
                    p.getDataInizio().get(Calendar.DAY_OF_MONTH)+"/"+(p.getDataInizio().get(Calendar.MONTH)+1)+"/"+p.getDataInizio().get(Calendar.YEAR)+
                    "-"+p.getDataInizio().get(Calendar.DAY_OF_MONTH)+"/"+(p.getDataFine().get(Calendar.MONTH)+1)+"/"+p.getDataFine().get(Calendar.YEAR));

        salvaPromozioneInDB(p);
        promozioniPerTipo.get(p.getTipo()).add(p);

        //Se è una promozione fedeltà, registro tutti i clienti fedeltà come observer
        if (p.getTipo() == TipoPromozione.FEDELTA)
        {
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

    public PromozioneTratta getPromozioneAttivaTratta(Tratta t)
    {
        List<Promozione> promoTratte = promozioniPerTipo.get(TipoPromozione.TRATTA);
        for(Promozione p : promoTratte)
        {
            PromozioneTratta promo = (PromozioneTratta) p;
            if(promo.getTratta().equals(t) && promo.isAttiva())
            {
                return promo;
            }
        }
        return null;
    }

    public PromozioneFedelta getPromozioneAttivaFedelta()
    {
        for(Promozione promozione : promozioniPerTipo.get(TipoPromozione.FEDELTA))
        {
            PromozioneFedelta promozioneFedelta = (PromozioneFedelta) promozione;
            if(promozioneFedelta.isAttiva())
            {
                return promozioneFedelta;
            }
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
                if(p.isAttiva())
                    promozioni.add(p);
            }
        }
        return promozioni;
    }

    public PromozioneTreno getPromozioneAttivaPerTipoTreno(TipoTreno t)
    {
        List<Promozione> promoTratte = promozioniPerTipo.get(TipoPromozione.TRENO);
        for(Promozione p : promoTratte)
        {
            PromozioneTreno promo = (PromozioneTreno) p;
            if(promo.getTipoTreno() == t && promo.isAttiva())
            {
                return promo;
            }
        }
        return null;
    }

    private void registraObserverPromozioneFedelta(PromozioneFedelta promozione)
    {
        GestoreClienti gc = GestoreClienti.getInstance();

        //Ottiengo solo i clienti fedeltà che VOGLIONO ricevere promozioni
        List<Cliente> clientiFedelta = gc.getClientiFedeltaConNotifiche();

        for (Cliente cliente : clientiFedelta)
        {
            ObserverPromozione observer = new ObserverPromozioneFedelta(cliente);
            promozione.attach(observer);
        }

        //Notifico immediatamente tutti i clienti registrati
        if (!clientiFedelta.isEmpty()) {
            promozione.notifica();
        }
    }

    public List<Promozione> getPromoPerTipo(TipoPromozione tipoPromozione)
    {
        List<Promozione> res = new ArrayList<>(promozioniPerTipo.get(tipoPromozione));
        return res;
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
}

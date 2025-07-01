package it.trenical.server.domain.gestore;

import com.sun.jdi.connect.Connector;
import it.trenical.server.database.ConnessioneADB;
import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.Viaggio;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoBiglietto;
import it.trenical.server.dto.NotificaDTO;
import it.trenical.server.dto.RimborsoDTO;
import it.trenical.server.observer.ViaggioETreno.NotificatoreClienteViaggio;
import it.trenical.server.observer.ViaggioETreno.ObserverViaggio;

import java.sql.*;
import java.util.*;

public class GestoreBiglietti
{
    private static GestoreBiglietti instance = null;

    private Map<String, Biglietto> bigliettiPerID;
    private Map<String, List<Biglietto>> bigliettiPerUtente;
    private Map<String, List<Biglietto>> bigliettiPerViaggio;

    private GestoreBiglietti()
    {
        bigliettiPerID = new HashMap<>();
        bigliettiPerUtente = new HashMap<>();
        bigliettiPerViaggio = new HashMap<>();

        caricaDatiDaDB();
    }

    private void caricaDatiDaDB()
    {
        String sql = "SELECT * FROM biglietti";
        Connection conn = null;
        try
        {
            conn = ConnessioneADB.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while(rs.next())
            {
                String id = rs.getString("id");
                String viaggio_id = rs.getString("viaggio_id");
                String cliente_id = rs.getString("cliente_id");
                String classe_servizio = rs.getString("classe_servizio");
                double prezzo = rs.getDouble("prezzo");
                String stato = rs.getString("stato");
                Timestamp data_acquisto = rs.getTimestamp("data_acquisto");

                ClasseServizio classeServizio = ClasseServizio.valueOf(classe_servizio);
                StatoBiglietto statoBiglietto = StatoBiglietto.valueOf(stato);
                Calendar dataAcquisto = Calendar.getInstance();
                dataAcquisto.setTimeInMillis(data_acquisto.getTime());

                Biglietto biglietto = new Biglietto(id, viaggio_id, cliente_id, classeServizio, statoBiglietto, dataAcquisto, prezzo);

                GestoreViaggi gv = GestoreViaggi.getInstance();
                Viaggio viaggio = gv.getViaggio(viaggio_id);
                if (viaggio != null)
                {
                    //ricalcolo da capo il prezzo del biglietto, punto debolissimo
                    biglietto.inizializzaPrezzoBiglietto(viaggio);

                    GestoreClienti gc = GestoreClienti.getInstance();
                    Cliente cliente = gc.getClienteById(cliente_id);
                    if (cliente != null)
                    {
                        biglietto.applicaPromozione(cliente);
                    }
                }
                bigliettiPerID.put(id, biglietto);

                if (!bigliettiPerUtente.containsKey(cliente_id))
                {
                    bigliettiPerUtente.put(cliente_id, new ArrayList<>());
                }
                bigliettiPerUtente.get(cliente_id).add(biglietto);

                if (!bigliettiPerViaggio.containsKey(viaggio_id))
                {
                    bigliettiPerViaggio.put(viaggio_id, new ArrayList<>());
                }
                bigliettiPerViaggio.get(viaggio_id).add(biglietto);
            }
        }
        catch(SQLException e)
        {
            System.err.println("Errore: "+ e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void salvaBigliettoInDB(Biglietto biglietto)
    {
        String sql = "INSERT INTO biglietti (id, viaggio_id, cliente_id, classe_servizio, prezzo, stato, data_acquisto) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;

        try {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, biglietto.getID());
            pstmt.setString(2, biglietto.getIDViaggio());
            pstmt.setString(3, biglietto.getIDCliente());
            pstmt.setString(4, biglietto.getClasseServizio().name());
            pstmt.setDouble(5, biglietto.getPrezzo());
            pstmt.setString(6, biglietto.getStato().name());
            pstmt.setTimestamp(7, new Timestamp(biglietto.getDataAcquisto().getTimeInMillis()));

            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore salvataggio biglietto: " + e.getMessage());
            throw new RuntimeException("Impossibile salvare il biglietto", e);
        }
        finally
        {
            System.out.println("Biglietto "+ biglietto.toString()+ " salvato nel db");
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void rimuoviBigliettoDaDB(String idBiglietto)
    {
        String sql = "DELETE FROM biglietti WHERE id = ?";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, idBiglietto);
            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore rimozione biglietto: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    private void aggiornaBigliettoInDB(Biglietto biglietto)
    {
        String sql = "UPDATE biglietti SET classe_servizio = ?, prezzo = ?, stato = ? WHERE id = ?";
        Connection conn = null;

        try
        {
            conn = ConnessioneADB.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, biglietto.getClasseServizio().name());
            pstmt.setDouble(2, biglietto.getPrezzo());
            pstmt.setString(3, biglietto.getStato().name());
            pstmt.setString(4, biglietto.getID());

            pstmt.executeUpdate();

        }
        catch (SQLException e)
        {
            System.err.println("Errore aggiornamento biglietto: " + e.getMessage());
        }
        finally
        {
            ConnessioneADB.closeConnection(conn);
        }
    }

    public void pagaBiglietto(String id)
    {
        //valido il biglietto
        Biglietto biglietto = bigliettiPerID.get(id);
        if (biglietto == null)
        {
            throw new IllegalArgumentException("Biglietto non trovato: " + id);
        }

        biglietto.SetStatoBigliettoPAGATO();

        //aggiorno il numero di posti nel viaggio
        Viaggio v = GestoreViaggi.getInstance().getViaggio(biglietto.getIDViaggio());
        System.out.println("Posti in "+ biglietto.getClasseServizio()+" prima: "+ v.getPostiDisponibiliPerClasse(biglietto.getClasseServizio()));
        v.riduciPostiDisponibiliPerClasse(biglietto.getClasseServizio(), 1);
        System.out.println("Posti in "+ biglietto.getClasseServizio()+" dopo: "+ v.getPostiDisponibiliPerClasse(biglietto.getClasseServizio()));


        //dopo aver aggiornato in memoria, aggiorno su DB
        aggiornaBigliettoInDB(biglietto);

        System.out.println("Biglietto " + id + " pagato con successo");
    }

    public static synchronized GestoreBiglietti getInstance()
    {
        if(instance == null)
            instance = new GestoreBiglietti();
        return instance;
    }

    public Biglietto creaBiglietto(String IDViaggio, String IDUtente, ClasseServizio classeServizio)
    {
        GestoreViaggi gv = GestoreViaggi.getInstance();
        GestoreClienti gc = GestoreClienti.getInstance();

        if(gv.getViaggio(IDViaggio) == null)
        {
            throw new IllegalArgumentException("ERRORE: IMPOSSIBILE CREARE IL BIGLIETTO, Il viaggio "+ IDViaggio+" non esiste");
        }
        if(!gc.esisteClienteID(IDUtente))
        {
            throw new IllegalArgumentException("ERRORE: IMPOSSIBILE CREARE IL BIGLIETTO, l'utente "+ IDUtente+" non esiste");
        }
        Biglietto b = new Biglietto(IDViaggio, IDUtente, classeServizio);
        b.inizializzaPrezzoBiglietto(gv.getViaggio(IDViaggio)); //inizializzo il prezzo
        System.out.println("prezzo = "+ b.getPrezzoBiglietto());

        //applico eventuali promozioni
        Cliente clienteBiglietto = gc.getClienteById(IDUtente);
        b.applicaPromozione(clienteBiglietto);
        System.out.println("prezzo con promozione applicata = "+ b.getPrezzoBiglietto());

        aggiungiBiglietto(b, IDViaggio, IDUtente);
        return b;
    }

    private void aggiungiBiglietto(Biglietto b, String IDViaggio, String IDUtente)
    {
        salvaBigliettoInDB(b);

        bigliettiPerID.put(b.getID(), b);
        if(!bigliettiPerUtente.containsKey(IDUtente)) //è la prima volta che acquista
        {
            bigliettiPerUtente.put(IDUtente, new ArrayList<>());
        }
        bigliettiPerUtente.get(IDUtente).add(b);
        //il controllo per il viaggio non lo faccio perché quando creo un viaggio aggiorno la mappa qui
        bigliettiPerViaggio.get(IDViaggio).add(b);

        registraClientePerNotificheViaggio(IDUtente, IDViaggio);
    }

    private void registraClientePerNotificheViaggio(String idCliente, String idViaggio)
    {
        GestoreViaggi gv = GestoreViaggi.getInstance();
        GestoreClienti gc = GestoreClienti.getInstance();

        Viaggio viaggio = gv.getViaggio(idViaggio);
        Cliente cliente = gc.getClienteById(idCliente);

        if (viaggio != null && cliente != null && cliente.isRiceviNotifiche())
        {
            ObserverViaggio notificatore = new NotificatoreClienteViaggio(cliente);
            viaggio.attach(notificatore);

            //invio una notifica per la conferma dell'acquisto
            NotificaDTO conferma = new NotificaDTO(
                    "Biglietto acquistato con successo!\n" +
                            "Viaggio: " + viaggio.getId() + "\n" +
                            "Da: " + viaggio.getTratta().getStazionePartenza().getCitta() + "\n" +
                            "A: " + viaggio.getTratta().getStazioneArrivo().getCitta() + "\n" +
                            "Riceverai aggiornamenti su questo viaggio."
            );
            GestoreNotifiche.getInstance().inviaNotifica(idCliente, conferma);
        }
    }

    public void aggiungiViaggio(String IDViaggio)
    {
        bigliettiPerViaggio.put(IDViaggio, new ArrayList<>());
    }

    public RimborsoDTO cancellaBiglietto(Biglietto b)
    {
        rimuoviBigliettoDaDB(b.getID());

        bigliettiPerID.remove(b.getID());
        bigliettiPerViaggio.get(b.getIDViaggio()).remove(b);
        bigliettiPerUtente.get(b.getIDCliente()).remove(b);

        //prendo i dati per il rimborso e li restituisco in un DTO
        String idbiglietto = b.getID();
        String idUtente = b.getIDCliente();
        double saldo = b.getPrezzoBiglietto();
        return new RimborsoDTO(idbiglietto, idUtente, saldo);
    }

    public List<Biglietto> getBigliettiUtente(String IDUtente)
    {
        if(!bigliettiPerUtente.containsKey(IDUtente))
            throw new IllegalArgumentException("Errore: l'utente "+ IDUtente+" non ha biglietti acquistati");
        return bigliettiPerUtente.get(IDUtente);
    }

    public Collection<Biglietto> getBigliettiAttivi()
    {
        return bigliettiPerID.values();
    }

    public List<Biglietto> getBigliettiPerViaggio(String IDViaggio)
    {
        if(!bigliettiPerViaggio.containsKey(IDViaggio))
            throw new IllegalArgumentException("Errore: il viaggio "+ IDViaggio+" non esiste");
        return bigliettiPerViaggio.get(IDViaggio);
    }

    public Biglietto getBigliettoPerID(String ID)
    {
        return bigliettiPerID.get(ID);
    }

    private void modificaBigliettoUtente(String IDBiglietto, String IDUtente, ClasseServizio classeServizio)
    {
        for(Biglietto b : getBigliettiUtente(IDUtente))
        {
            if (b.getID().equals(IDBiglietto))
                b.modificaClasseServizio(classeServizio);
        }
    }

    private void modificaBigliettoClasseServizio(String IDBiglietto, String IDViaggio, ClasseServizio classeServizio)
    {
        for(Biglietto b : getBigliettiPerViaggio(IDViaggio))
        {
            if (b.getID().equals(IDBiglietto))
                b.modificaClasseServizio(classeServizio);
        }
    }

    public void modificaClasseServizio(String IDBiglietto, String IDUtente, String IDViaggio, ClasseServizio classeServizio)
    {
        Biglietto biglietto = bigliettiPerID.get(IDBiglietto);
        if (biglietto == null)
        {
            throw new IllegalArgumentException("Biglietto non trovato: " + IDBiglietto);
        }

        //Modifico la classe di servizio (questo avvia automaticamente il ricalcolo del prezzo base)
        biglietto.modificaClasseServizio(classeServizio);

        aggiornaBigliettoInDB(biglietto);

        //Invio una notifica al cliente sulla modifica
        GestoreClienti gc = GestoreClienti.getInstance();
        Cliente cliente = gc.getClienteById(IDUtente);
        if (cliente != null && cliente.isRiceviNotifiche())
        {
            String messaggio = "Il tuo biglietto "+ IDBiglietto+" è stato modificato.\n" +
                            "Nuova classe di servizio: " +classeServizio+"\n"+
                            "Nuovo prezzo: "+ biglietto.getPrezzo();

            NotificaDTO notifica = new NotificaDTO(messaggio);
            GestoreNotifiche.getInstance().inviaNotifica(IDUtente, notifica);
        }
    }
}

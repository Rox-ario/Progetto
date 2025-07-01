
package it.trenical.server.command.biglietto;

import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.Viaggio;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoViaggio;
import it.trenical.server.domain.gestore.GestoreBiglietti;
import it.trenical.server.domain.gestore.GestoreClienti;
import it.trenical.server.domain.gestore.GestoreViaggi;

import java.util.Calendar;

public class AssegnaBiglietto implements ComandoBiglietto
{
    private final String IDViaggio;
    private final ClasseServizio classeServizio;
    private final String IDCliente;
    private Biglietto biglietto;

    public AssegnaBiglietto(String IDViaggio, String IDCliente, ClasseServizio classeServizio)
    {
        if (IDViaggio == null || IDViaggio.trim().isEmpty())
        {
            throw new IllegalArgumentException("ID viaggio non può essere nullo o vuoto");
        }
        if (IDCliente == null || IDCliente.trim().isEmpty())
        {
            throw new IllegalArgumentException("ID cliente non può essere nullo o vuoto");
        }
        if (classeServizio == null)
        {
            throw new IllegalArgumentException("Classe di servizio non può essere nulla");
        }

        this.IDViaggio = IDViaggio.trim();
        this.IDCliente = IDCliente.trim();
        this.classeServizio = classeServizio;
        biglietto = null;
    }

    @Override
    public void esegui()
    {
        GestoreBiglietti gb = GestoreBiglietti.getInstance();
        GestoreViaggi gv = GestoreViaggi.getInstance();

        //recupero il viaggio (GestoreBiglietti.creaBiglietto già controlla l'esistenza)
        Viaggio viaggio = gv.getViaggio(IDViaggio);
        if (viaggio == null)
        {
            throw new IllegalArgumentException("Errore: il viaggio " + IDViaggio + " non esiste");
        }

        //controlli non fatto om GestoreBiglietti, lo faccio qua
        verificaViaggioPrenotiabile(viaggio);
        verificaDisponibilitaPosti(viaggio);

        try
        {
            //provo a ridurre i posti
            viaggio.riduciPostiDisponibiliPerClasse(classeServizio, 1);

            //tutto il resto lo lascio a Gestore
            //(validazioni cliente, creazione, prezzo, promozioni, salvataggio, notifiche)
            this.biglietto = gb.creaBiglietto(IDViaggio, IDCliente, classeServizio);

            System.out.println("Biglietto assegnato con successo per il viaggio " + IDViaggio);
            System.out.println("Cliente: " + IDCliente);
            System.out.println("Classe: " + classeServizio);
            System.out.println("Stato: NON_PAGATO");

        }
        catch (Exception e)
        {
            //ripristino i posti se la creazione fallisce
            try
            {
                viaggio.incrementaPostiDisponibiliPerClasse(classeServizio, 1);
                System.out.println("reintegro posti completato");
            }
            catch (Exception rollbackError)
            {
                System.err.println("ERRORE CRITICO: Impossibile fare rollback dei posti! " + rollbackError.getMessage());
            }
            throw new RuntimeException("Errore durante l'assegnazione del biglietto: " + e.getMessage(), e);
        }
    }

    private void verificaViaggioPrenotiabile(Viaggio viaggio)
    {
        //Controllo lo stato del viaggio
        StatoViaggio stato = viaggio.getStato();
        if (stato == StatoViaggio.TERMINATO)
        {
            throw new IllegalStateException("Errore: non è possibile prenotare biglietti per un viaggio terminato");
        }

        //Controllo che il viaggio non sia già iniziato
        Calendar ora = Calendar.getInstance();
        Calendar inizioViaggio = viaggio.getInizioReale(); //include eventuali ritardi

        if (ora.after(inizioViaggio))
        {
            throw new IllegalStateException("Errore: non è possibile prenotare biglietti per un viaggio già iniziato");
        }

        //Controllo opzionale: non permettere prenotazioni troppo a ridosso della partenza
        //(ad esempio meno di 30 minuti prima)
        Calendar limitePrenotazione = (Calendar) inizioViaggio.clone();
        limitePrenotazione.add(Calendar.MINUTE, -30);

        if (ora.after(limitePrenotazione))
        {
            throw new IllegalStateException("Errore: non è possibile prenotare biglietti meno di 30 minuti prima della partenza");
        }
    }

    private void verificaDisponibilitaPosti(Viaggio viaggio)
    {
        int postiDisponibili = viaggio.getPostiDisponibiliPerClasse(classeServizio);

        if (postiDisponibili <= 0)
        {
            throw new IllegalStateException("Errore: non ci sono posti disponibili per la classe "+classeServizio+" nel viaggio "+IDViaggio);
        }

        System.out.println("Posti disponibili per " + classeServizio + ": " + postiDisponibili);
    }

    public Biglietto getBiglietto()
    {
        return biglietto;
    }
}

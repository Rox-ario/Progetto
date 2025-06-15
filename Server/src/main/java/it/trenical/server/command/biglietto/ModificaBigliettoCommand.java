package it.trenical.server.command.biglietto;

import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.Viaggio;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoBiglietto;
import it.trenical.server.domain.enumerations.StatoViaggio;
import it.trenical.server.domain.gestore.GestoreBiglietti;
import it.trenical.server.domain.gestore.GestoreViaggi;
import it.trenical.server.dto.ModificaBigliettoDTO;

import java.util.Calendar;

public class ModificaBigliettoCommand implements ComandoBiglietto
{
    private final ModificaBigliettoDTO modificaBigliettoDTO;

    public ModificaBigliettoCommand(ModificaBigliettoDTO modificaBigliettoDTO)
    {
        this.modificaBigliettoDTO = modificaBigliettoDTO;
    }

    @Override
    public void esegui()
    {
        GestoreBiglietti gb = GestoreBiglietti.getInstance();
        Biglietto b = gb.getBigliettoPerID(modificaBigliettoDTO.getIDBiglietto());
        if(b == null)
            throw new IllegalArgumentException("Errore: il biglietto "+ modificaBigliettoDTO.getIDBiglietto() + " non esiste");
        if(b.getStato() == StatoBiglietto.PAGATO)
            throw new IllegalArgumentException("Errore: il biglietto "+ modificaBigliettoDTO.getIDBiglietto()+" è già stato pagato");

        ClasseServizio vecchiaClasse = b.getClasseServizio();
        String utente = b.getIDCliente();
        String IDViaggio = b.getIDViaggio();

        GestoreViaggi gv = GestoreViaggi.getInstance();
        Viaggio v = gv.getViaggio(IDViaggio);
        if(v == null)
            throw new IllegalArgumentException("Errore: il viaggio "+ v.getId()+" non esiste");

        //il viaggio esiste
        if(Calendar.getInstance().before(v.getInizioReale()) && v.getPostiDisponibiliPerClasse(modificaBigliettoDTO.getClasseServizio()) > 0)
        {
            gb.modificaClasseServizio(b.getID(), b.getIDCliente(), b.getIDViaggio(), modificaBigliettoDTO.getClasseServizio());
            v.riduciPostiDisponibiliPerClasse(modificaBigliettoDTO.getClasseServizio(), 1);
            v.incrementaPostiDisponibiliPerClasse(vecchiaClasse, 1);
        }
        else
            throw new IllegalArgumentException("Errore: il treno è già partito oppure non ci sono più posti");

    }
}

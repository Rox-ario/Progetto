package it.trenical.server.command.biglietto;

import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.gestore.GestoreBanca;
import it.trenical.server.domain.gestore.GestoreBiglietti;
import it.trenical.server.dto.RimborsoDTO;

public class CancellaBiglietto implements ComandoBiglietto
{
    private final String IDbiglietto;

    public CancellaBiglietto(String IDbiglietto)
    {
        if(IDbiglietto == null)
            throw new IllegalArgumentException("Errore: il biglietto non può essere null");
        this.IDbiglietto = IDbiglietto;
    }

    @Override
    public void esegui()
    {
        GestoreBiglietti gb = GestoreBiglietti.getInstance();
        Biglietto biglietto = gb.getBigliettoPerID(IDbiglietto);
        if(biglietto == null)
        {
            throw new IllegalArgumentException("Il biglietto "+ IDbiglietto+ "non si può annullare perché non esiste");
        }
        else
        {
            RimborsoDTO dto = gb.cancellaBiglietto(biglietto);
            GestoreBanca.getInstance().rimborsa(dto);
        }
    }
}

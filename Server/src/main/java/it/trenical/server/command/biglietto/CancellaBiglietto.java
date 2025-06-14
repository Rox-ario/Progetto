package it.trenical.server.command.biglietto;

import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.gestore.GestoreBiglietti;

public class CancellaBiglietto implements ComandoBiglietto
{
    private final Biglietto biglietto;

    public CancellaBiglietto(Biglietto biglietto)
    {
        if(biglietto == null)
            throw new IllegalArgumentException("Errore: il biglietto non può essere null");
        this.biglietto = biglietto;
    }

    @Override
    public void esegui()
    {
        GestoreBiglietti gb = GestoreBiglietti.getInstance();
        gb.cancellaBiglietto(biglietto);
    }
}

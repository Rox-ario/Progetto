package it.trenical.server.command.biglietto;

import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.gestore.GestoreBanca;
import it.trenical.server.domain.gestore.GestoreBiglietti;

public class PagaBiglietto implements ComandoBiglietto
{
    private final String IDBiglietto;

    public PagaBiglietto(String idBiglietto) {
        this.IDBiglietto = idBiglietto;
    }

    @Override
    public void esegui()
    {
        GestoreBiglietti gb = GestoreBiglietti.getInstance();
        Biglietto biglietto = gb.getBigliettoPerID(IDBiglietto);
        if(biglietto == null)
        {
            throw new IllegalArgumentException("Il biglietto "+ IDBiglietto+ "non si può annullare perché non esiste");
        }
        else
        {
            double costo = biglietto.getPrezzo().getPrezzo();
            GestoreBanca.getInstance().eseguiPagamento(biglietto.getIDCliente(), costo);
        }
    }
}

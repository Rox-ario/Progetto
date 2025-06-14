package it.trenical.server.command.biglietto;

import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.PrezzoBiglietto;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoBiglietto;
import it.trenical.server.domain.gestore.GestoreBiglietti;

public class AssegnaBiglietto implements ComandoBiglietto
{
    private final String IDViaggio;
    private ClasseServizio classeServizio;
    private final String IDCliente;

    public AssegnaBiglietto(String IDViaggio, String IDCliente, ClasseServizio classeServizio)
    {
        this.IDViaggio = IDViaggio;
        this.IDCliente = IDCliente;
        this.classeServizio = classeServizio;
    }
    @Override
    public void esegui()
    {
        GestoreBiglietti gb = GestoreBiglietti.getInstance();
        gb.creaBiglietto(IDViaggio, IDCliente, classeServizio);
    }
}

package it.trenical.server.domain;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoBiglietto;
import it.trenical.server.domain.gestore.GestoreViaggi;

import java.util.UUID;

public class Biglietto
{
    private final String ID;
    private final String IDViaggio;
    private PrezzoBiglietto prezzo; //lo calcola da sè
    private ClasseServizio classeServizio;
    private final String IDCliente;
    private StatoBiglietto statoBiglietto;

    public Biglietto(String IDViaggio, String IDCliente, ClasseServizio classeServizio)
    {
        this.ID = UUID.randomUUID().toString();
        this.IDViaggio = IDViaggio;
        this.IDCliente = IDCliente;
        this.classeServizio = classeServizio;
        this.statoBiglietto = StatoBiglietto.NON_PAGATO;
    }

    @Override
    public String toString() {
        return "Biglietto{" +
                "ID='" + ID + '\'' +
                ", IDViaggio='" + IDViaggio + '\'' +
                ", prezzo=" + prezzo +
                ", classeServizio=" + classeServizio +
                ", IDCliente='" + IDCliente + '\'' +
                '}';
    }

    public StatoBiglietto getStato()
    {
        return statoBiglietto;
    }

    public void modificaClasseServizio(ClasseServizio classe)
    {
        classeServizio = classe;
    }

    public ClasseServizio getClasseServizio()
    {
        return classeServizio;
    }

    public String getID() {
        return ID;
    }

    public String getIDViaggio() {
        return IDViaggio;
    }

    public double getPrezzo() {
        return prezzo.getPrezzo();
    }

    public String getIDCliente() {
        return IDCliente;
    }

    public void SetStatoBigliettoPAGATO()
    {
        if(getStato() == StatoBiglietto.PAGATO)
            throw new IllegalArgumentException("Errore: il biglietto "+ getID()+" è già stato pagato");
        if(getStato() == StatoBiglietto.NON_PAGATO)
            this.statoBiglietto = StatoBiglietto.PAGATO;
    }

    public void inizializzaPrezzoBiglietto(Viaggio v)
    {
        prezzo = new PrezzoBiglietto(this, v);
    }

    public void applicaPromozione(Cliente c)
    {
        prezzo.applicaPromozione(c);
    }
}

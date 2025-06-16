package it.trenical.server.domain;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoBiglietto;

import java.util.Calendar;
import java.util.UUID;

public class Biglietto
{
    private final String ID;
    private final String IDViaggio;
    private PrezzoBiglietto prezzoBiglietto; //lo calcola da sè
    private ClasseServizio classeServizio;
    private final String IDCliente;
    private final Calendar dataAcquisto;
    private StatoBiglietto statoBiglietto;
    private double prezzoOriginale;

    public Biglietto(String IDViaggio, String IDCliente, ClasseServizio classeServizio)
    {
        this.ID = UUID.randomUUID().toString();
        this.IDViaggio = IDViaggio;
        this.IDCliente = IDCliente;
        this.classeServizio = classeServizio;
        this.statoBiglietto = StatoBiglietto.NON_PAGATO;
        this.dataAcquisto = Calendar.getInstance();
    }

    public double getPrezzo()
    {
        return prezzoOriginale;
    }

    @Override
    public String toString() {
        return "Biglietto{" +
                "ID='" + ID + '\'' +
                ", IDViaggio='" + IDViaggio + '\'' +
                ", prezzo=" + prezzoBiglietto +
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
        //Ricalcolo il prezzo dopo la modifica
        if (prezzoBiglietto != null)
        {
            prezzoBiglietto.ricalcolaPrezzo();
        }
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

    public double getPrezzoBiglietto() {
        return prezzoBiglietto.getPrezzo();
    }

    public String getIDCliente() {
        return IDCliente;
    }

    public void SetStatoBigliettoPAGATO()
    {
        if(getStato() == StatoBiglietto.PAGATO)
            throw new IllegalArgumentException("Errore: il biglietto "+ getID()+" è già stato pagato");
        if(getStato() == StatoBiglietto.NON_PAGATO)
        {
            this.statoBiglietto = StatoBiglietto.PAGATO;
            this.prezzoOriginale = getPrezzoBiglietto();
        }
    }

    public void inizializzaPrezzoBiglietto(Viaggio v)
    {
        prezzoBiglietto = new PrezzoBiglietto(this, v);
    }

    public void applicaPromozione(Cliente c)
    {
        prezzoBiglietto.applicaPromozione(c);
    }

    public PrezzoBiglietto getOggettoPrezzoBiglietto()
    {
        return prezzoBiglietto;
    }
}

package it.trenical.server.domain;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoBiglietto;

import java.util.Calendar;
import java.util.UUID;

public class Biglietto
{
    private String ID;
    private String IDViaggio;
    private PrezzoBiglietto prezzoBiglietto; //lo calcola da sè
    private ClasseServizio classeServizio;
    private String IDCliente;
    private Calendar dataAcquisto;
    private StatoBiglietto statoBiglietto;
    private double prezzo;

    public Biglietto(String IDViaggio, String IDCliente, ClasseServizio classeServizio)
    {
        this.ID = UUID.randomUUID().toString();
        this.IDViaggio = IDViaggio;
        this.IDCliente = IDCliente;
        this.classeServizio = classeServizio;
        this.statoBiglietto = StatoBiglietto.NON_PAGATO;
        this.dataAcquisto = Calendar.getInstance();
    }

    //per quando riprendo tutto dal DB
    public Biglietto(String ID, String IDViaggio, String IDCliente, ClasseServizio classeServizio,
                     StatoBiglietto stato, Calendar dataAcquisto, double prezzoOriginale)
    {
        this.ID = ID;
        this.IDViaggio = IDViaggio;
        this.IDCliente = IDCliente;
        this.classeServizio = classeServizio;
        this.statoBiglietto = stato;
        this.dataAcquisto = dataAcquisto;
        this.prezzo = prezzoOriginale;
    }

    public double getPrezzo()
    {
        return prezzo;
    }

    @Override
    public String toString() {
        return "Biglietto{" +
                "ID='" + ID + '\'' +
                ", IDViaggio='" + IDViaggio + '\'' +
                ", prezzo=" + prezzoBiglietto.getPrezzo() +
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
            this.prezzo = prezzoBiglietto.getPrezzo();
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
            if(this.prezzo == 0)//quindi non è stato impostato
            {
                this.prezzo = getPrezzoBiglietto();
            }
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

    public void setPrezzo(double prezzo)
    {
        this.prezzo = prezzo;
    }

    public void setStatoBiglietto(StatoBiglietto stato)
    {
        this.statoBiglietto = stato;
    }

    public Calendar getDataAcquisto()
    {
        return dataAcquisto;
    }

    public StatoBiglietto getStatoBiglietto()
    {
        return statoBiglietto;
    }
}

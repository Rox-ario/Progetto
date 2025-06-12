package it.trenical.server.domain;

public class Biglietto
{
    private final String ID;
    private final String IDViaggio;
    private PrezzoBiglietto prezzo; //lo calcola da sè
    private ClasseServizio classeServizio;
    private final String IDCliente;
    private StatoBiglietto statoBiglietto;
    private final String posto;

    public Biglietto(String ID, String IDViaggio, String IDCliente, String posto, ClasseServizio classeServizio)
    {
        this.ID = ID;
        this.IDViaggio = IDViaggio;
        this.IDCliente = IDCliente;
        this.posto = posto;
        this.classeServizio = classeServizio;
        this.statoBiglietto = StatoBiglietto.VALIDO;
    }

    @Override
    public String toString() {
        return "Biglietto{" +
                "ID='" + ID + '\'' +
                ", IDViaggio='" + IDViaggio + '\'' +
                ", prezzo=" + prezzo +
                ", classeServizio=" + classeServizio +
                ", IDCliente='" + IDCliente + '\'' +
                ", posto='" + posto + '\'' +
                '}';
    }

    //public void modificaStato();

    //public void elimina() non sembra avere senso

    //public Biglietto copia()

    public StatoBiglietto getStato()
    {
        return statoBiglietto;
    }

    public void modificaPosto()
    {

    }

    public void modificaClasseServizio(ClasseServizio classe)
    {

    }
}

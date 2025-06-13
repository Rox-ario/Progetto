package it.trenical.server.domain;

public class PrezzoBiglietto
{
    private final Biglietto b;
    private final Viaggio v; //in base alle feature del viaggio e del treno, ne calcola il prezzo
    private double prezzo = 0;

    public PrezzoBiglietto(Biglietto b, Viaggio v)
    {
        this.b = b;
        this.v = v;
    }

    public void calcolaPrezzo()
    {

    }

    public double getPrezzo()
    {
        return prezzo;
    }
}

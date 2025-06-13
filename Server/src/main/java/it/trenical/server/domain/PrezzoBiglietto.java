package it.trenical.server.domain;

public class PrezzoBiglietto
{
    private final Biglietto biglietto;
    private final Viaggio viaggio; //in base alle feature del viaggio e del treno, ne calcola il prezzo
    private double prezzo = 0;

    public PrezzoBiglietto(Biglietto b, Viaggio v)
    {
        this.biglietto = b;
        this.viaggio = v;
    }

    public void calcolaPrezzo()
    {
        double kilometri = viaggio.getKilometri();
        double aggiuntaTipo = viaggio.getTreno().getTipo().getAumentoPrezzo();
        double aggiuntaServizio = biglietto.getClasseServizio().getCoefficienteAumentoPrezzo();

        prezzo = kilometri*aggiuntaServizio*aggiuntaTipo;
    }

    public double getPrezzo()
    {
        return prezzo;
    }
}

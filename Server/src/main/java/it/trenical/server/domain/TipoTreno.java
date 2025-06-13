package it.trenical.server.domain;

public enum TipoTreno
{
    INTERCITY(1.3),
    COMFORT(1.0),
    ITALO(1.2);

    private final double aumentoPrezzo;

    TipoTreno(double aumentoPrezzo)
    {
        this.aumentoPrezzo = aumentoPrezzo;
    }

    public double getAumentoPrezzo()
    {
        return aumentoPrezzo;
    }
}

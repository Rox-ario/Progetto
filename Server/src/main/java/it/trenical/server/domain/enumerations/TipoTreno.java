package it.trenical.server.domain.enumerations;

public enum TipoTreno
{
    INTERCITY(1.3, 120.0),
    COMFORT(1.0, 100.0),
    ITALO(1.2, 130.0);

    private final double aumentoPrezzo;
    private final double velocitaMedia;

    TipoTreno(double aumentoPrezzo, double velocitaMedia)
    {
        this.aumentoPrezzo = aumentoPrezzo;
        this.velocitaMedia = velocitaMedia;
    }

    public double getAumentoPrezzo()
    {
        return aumentoPrezzo;
    }
    public double getVelocitaMedia(){return velocitaMedia;}
}

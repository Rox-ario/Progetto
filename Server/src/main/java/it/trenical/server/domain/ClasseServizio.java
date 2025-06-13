package it.trenical.server.domain;

public enum ClasseServizio
{
    BUSINESS(1.35),
    ECONOMY(1.0),
    FEDELTA(0.90),
    LOW_COST(0.95);

    private final double coefficienteAumentoPrezzo;

    ClasseServizio(double coefficienteAumentoPrezzo) {
        this.coefficienteAumentoPrezzo = coefficienteAumentoPrezzo;
    }

    public double getCoefficienteAumentoPrezzo() {
        return coefficienteAumentoPrezzo;
    }
}

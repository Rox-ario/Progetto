package it.trenical.server.dto;

public class RimborsoDTO
{
    private final String idBiglietto;
    private final String idClienteRimborsato;
    private final double importoRimborsato;

    public RimborsoDTO(String idBiglietto, String idClienteRimborsato, double importoRimborsato) {
        this.idBiglietto = idBiglietto;
        this.idClienteRimborsato = idClienteRimborsato;
        this.importoRimborsato = importoRimborsato;
    }

    public String getIdBiglietto() {
        return idBiglietto;
    }

    public String getIdClienteRimborsato() {
        return idClienteRimborsato;
    }

    public double getImportoRimborsato() {
        return importoRimborsato;
    }
}

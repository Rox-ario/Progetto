package it.trenical.server.domain;

public class Treno
{
    private final String ID;
    private final int posti;
    private final String tipo;

    public Treno(String ID, String tipo, int posti) {
        this.ID = ID;
        this.tipo = tipo;
        this.posti = posti;
    }

    public String getID() {
        return ID;
    }

    public int getPosti() {
        return posti;
    }

    public String getTipo() {
        return tipo;
    }
}

package it.trenical.server.dto;

public class DatiBancariDTO
{
    private String nomeBanca;
    private String numeroCarta;
    private double saldo;

    public DatiBancariDTO(){}

    public DatiBancariDTO(String nomeBanca, String numeroCarta, double saldo)
    {
        this.nomeBanca = nomeBanca;
        this.numeroCarta = numeroCarta;
        this.saldo = saldo;
    }

    public String getNomeBanca() {
        return nomeBanca;
    }

    public void setNomeBanca(String nomeBanca) {
        this.nomeBanca = nomeBanca;
    }

    public String getNumeroCarta() {
        return numeroCarta;
    }

    public void setNumeroCarta(String numeroCarta) {
        this.numeroCarta = numeroCarta;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }
}

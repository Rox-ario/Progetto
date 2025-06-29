package it.trenical.server.dto;

public class DatiBancariDTO
{
    private String idCliente;
    private String nomeCliente;
    private String cognome;
    private String nomeBanca;
    private String numeroCarta;
    private double saldo;

    public DatiBancariDTO(String id, String nome, String cognome, String numeroCarta)
    {
        idCliente = id;
        nomeCliente = nome;
        this.cognome = cognome;
        nomeBanca = "Banca Trenical";
        this.numeroCarta = numeroCarta;
        saldo = 1000.0;
    }

    //per DB
    public DatiBancariDTO(String id, String nome, String cognome, String nomeBanca, String numeroCarta, double saldo)
    {
        idCliente = id;
        nomeCliente = nome;
        this.cognome = cognome;
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

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    @Override
    public String toString() {
        return "[" +
                "idCliente='" + idCliente + '\'' +
                ", nomeCliente='" + nomeCliente + '\'' +
                ", cognome='" + cognome + '\'' +
                ", nomeBanca='" + nomeBanca + '\'' +
                ", numeroCarta='" + numeroCarta + '\'' +
                ", saldo=" + saldo +
                ']';
    }
}

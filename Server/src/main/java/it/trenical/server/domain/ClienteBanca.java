package it.trenical.server.domain;

public class ClienteBanca
{
    private final String idCliente;
    private final String nome;
    private final String banca;
    private final String numeroCarta;
    private double saldo;

    public ClienteBanca(String idCliente, String nome, String banca, String numeroCarta, double saldo) {
        this.idCliente = idCliente;
        this.nome = nome;
        this.banca = banca;
        this.numeroCarta = numeroCarta;
        this.saldo = saldo;
    }


    public String getIdCliente() { return idCliente; }
    public String getNome() { return nome; }
    public String getBanca() { return banca; }
    public String getNumeroCarta() { return numeroCarta; }
    public double getSaldo() { return saldo; }

    public void addebita(double importo) {
        this.saldo -= importo;
    }

    public void accredita(double importo) {
        this.saldo += importo;
    }
}

package it.trenical.server.domain.cliente;

import java.util.UUID;

public class ClienteBanca
{
    private final String idCliente;
    private final String nome;
    private final String cognome;
    private final String banca;
    private final String numeroCarta;
    private double saldo;

    //per db
    public ClienteBanca(String idCliente, String nome, String cognome, String banca, String numeroCarta, double saldo) {
        this.idCliente = idCliente;
        this.nome = nome;
        this.cognome = cognome;
        this.banca = banca;
        this.numeroCarta = numeroCarta;
        this.saldo = saldo;
    }

    //per creazione
    public ClienteBanca(String id, String nome, String cognome, String numeroCarta, double saldo) {
        this.idCliente = id;
        this.nome = nome;
        this.cognome = cognome;
        this.banca = "Banca Trenical";
        this.numeroCarta = numeroCarta;
        this.saldo = saldo;
    }


    public String getIdCliente() { return idCliente; }
    public String getNome() { return nome; }
    public String getBanca() { return banca; }
    public String getNumeroCarta() { return numeroCarta; }
    public double getSaldo() { return saldo; }
    public String getCognome() {return cognome;}

    public void addebita(double importo) {
        this.saldo -= importo;
    }

    public void accredita(double importo) {
        this.saldo += importo;
    }
}

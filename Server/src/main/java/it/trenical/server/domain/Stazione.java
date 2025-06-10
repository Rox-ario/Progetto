package it.trenical.server.domain;

import java.util.ArrayList;
import java.util.List;

public class Stazione
{
    private final String id;
    private final String citta;
    private final String nome;
    private final ArrayList<Integer> binari;

    public Stazione(String id, String citta, String nome, ArrayList<Integer> binari) {
        this.id = id;
        this.citta = citta;
        this.nome = nome;
        this.binari = new ArrayList<>(binari); //per proteggere la lista di binari
    }

    public String getId() {
        return id;
    }

    public String getCitta() {
        return citta;
    }

    public String getNome() {
        return nome;
    }

    public List<Integer> getBinari() {
        return new ArrayList<>(binari); //per non esporre la collezione interna a chiunque abbia il dto
                                        // in quanto potrebbe modificarla
    }
}

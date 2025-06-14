package it.trenical.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Stazione
{
    private final String id;
    private final String citta;
    private final String nome;
    private final ArrayList<Integer> binari;
    private final double latitudine;
    private final double longitudine;

    public Stazione(String id, String citta, String nome, ArrayList<Integer> binari, double latitudine, double longitudine)
    {
        this.id = id;
        this.citta = citta;
        this.nome = nome;
        this.binari = binari;
        this.latitudine = latitudine;
        this.longitudine = longitudine;
    }

    public String getId()
    {
        return id;
    }

    public String getCitta()
    {
        return citta;
    }

    public String getNome()
    {
        return nome;
    }

    public List<Integer> getBinari()
    {
        return new ArrayList<>(binari); //per non esporre la collezione interna a chiunque abbia il dto
                                        // in quanto potrebbe modificarla
    }

    public double getLatitudine()
    {
        return latitudine;
    }

    public double getLongitudine()
    {
        return longitudine;
    }

    public void addBinario(int a)
    {
        binari.add(a);
    }

    public void addBinari(List<Integer> binariNuovi)
    {
        binari.addAll(binariNuovi);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Stazione stazione)) return false;
        return Double.compare(latitudine, stazione.latitudine) == 0 && Double.compare(longitudine, stazione.longitudine) == 0 && Objects.equals(id, stazione.id) && Objects.equals(citta, stazione.citta) && Objects.equals(nome, stazione.nome) && Objects.equals(binari, stazione.binari);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, citta, nome, binari, latitudine, longitudine);
    }
}

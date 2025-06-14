package it.trenical.server.domain.gestore;

import it.trenical.server.domain.*;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.TipoTreno;

import java.util.*;

public class CatalogoPromozione
{
    private static CatalogoPromozione instance = null;

    private final Map<Class<? extends Promozione>, List<Promozione>> promozioniPerTipo;

    private CatalogoPromozione()
    {
        promozioniPerTipo = new HashMap<>();
        promozioniPerTipo.put(PromozioneFedelta.class, new ArrayList<>());
        promozioniPerTipo.put(PromozioneTratta.class, new ArrayList<>());
        promozioniPerTipo.put(PromozioneTreno.class, new ArrayList<>());
    }

    public static synchronized CatalogoPromozione getInstance() {
        if (instance == null) {
            instance = new CatalogoPromozione();
        }
        return instance;
    }

    public void aggiungiPromozione(Promozione p)
    {
        if(p == null)
            throw new IllegalArgumentException("Errore: la promozione inserita è null");
        promozioniPerTipo.get(p.getClass()).add(p);
    }

    public void rimuoviPromozione(Promozione p)
    {
        if(p == null)
            throw new IllegalArgumentException("Errore: la promozione inserita è null");
        promozioniPerTipo.get(p.getClass()).remove(p);
    }

    public List<PromozioneTratta> getPromoPerTratta(Tratta t)
    {
        ArrayList<PromozioneTratta> promozioni = new ArrayList<>();
        List<Promozione> promoTratte = promozioniPerTipo.get(PromozioneTratta.class);
        for(Promozione p : promoTratte)
        {
            PromozioneTratta promo = (PromozioneTratta) p;
            if(promo.getTratta().equals(t) && !promo.getDataFine().before(Calendar.getInstance()))
            {
                promozioni.add(promo);
            }
        }
        return promozioni;
    }

    public List<Promozione> getPromozioniAttive()
    {
        ArrayList<Promozione> promozioni = new ArrayList<>();
        for(List<Promozione> lista : promozioniPerTipo.values())
        {
            for(Promozione p : lista)
            {
                if(p.isAttiva())
                    promozioni.add(p);
            }
        }
        return promozioni;
    }

    public List<PromozioneTreno> getPromoPerTipoTreno(TipoTreno t)
    {
        ArrayList<PromozioneTreno> promozioni = new ArrayList<>();
        List<Promozione> promoTratte = promozioniPerTipo.get(PromozioneTreno.class);
        for(Promozione p : promoTratte)
        {
            PromozioneTreno promo = (PromozioneTreno) p;
            if(promo.getTipoTreno() == t && !promo.getDataFine().before(Calendar.getInstance()))
            {
                promozioni.add(promo);
            }
        }
        return promozioni;
    }
}

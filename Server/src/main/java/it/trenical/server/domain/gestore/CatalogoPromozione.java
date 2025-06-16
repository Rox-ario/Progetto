package it.trenical.server.domain.gestore;

import it.trenical.server.domain.*;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.TipoPromozione;
import it.trenical.server.domain.enumerations.TipoTreno;
import it.trenical.server.observer.Promozione.ObserverPromozione;
import it.trenical.server.observer.Promozione.ObserverPromozioneFedelta;

import java.util.*;

public class CatalogoPromozione
{
    private static CatalogoPromozione instance = null;

    private final Map<TipoPromozione, List<Promozione>> promozioniPerTipo;

    private CatalogoPromozione()
    {
        promozioniPerTipo = new HashMap<>();
        promozioniPerTipo.put(TipoPromozione.FEDELTA, new ArrayList<>());
        promozioniPerTipo.put(TipoPromozione.TRATTA, new ArrayList<>());
        promozioniPerTipo.put(TipoPromozione.TRENO, new ArrayList<>());
    }

    public static synchronized CatalogoPromozione getInstance() {
        if (instance == null) {
            instance = new CatalogoPromozione();
        }
        return instance;
    }

    private boolean verificaSovrapposizione(Promozione nuovaPromo)
    {
        List<Promozione> promozioniStessoTipo = promozioniPerTipo.get(nuovaPromo.getTipo());

        for (Promozione esistente : promozioniStessoTipo)
        {
            if (sovrapponeTemporalmente(nuovaPromo, esistente))
            {
                // in base al tipo verifico cose
                if (nuovaPromo.getTipo() == TipoPromozione.FEDELTA) {
                    // Le promozioni fedeltà non possono sovrapporsi MAI
                    return true;
                }
                else if (nuovaPromo.getTipo() == TipoPromozione.TRATTA)
                {
                    // Le promozioni tratta si sovrappongono solo se hanno la stessa tratta
                    PromozioneTratta nuova = (PromozioneTratta) nuovaPromo;
                    PromozioneTratta vecchia = (PromozioneTratta) esistente;
                    if (nuova.getTratta().equals(vecchia.getTratta())) {
                        return true;
                    }
                }
                else if (nuovaPromo.getTipo() ==TipoPromozione.TRENO) {
                    // Le promozioni treno si sovrappongono solo se hanno lo stesso tipo treno
                    PromozioneTreno nuova = (PromozioneTreno) nuovaPromo;
                    PromozioneTreno vecchia = (PromozioneTreno) esistente;
                    if (nuova.getTipoTreno() == vecchia.getTipoTreno()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean sovrapponeTemporalmente(Promozione p1, Promozione p2)
    {
        Calendar inizioP1 = p1.getDataInizio();
        Calendar fineP1 = p1.getDataFine();
        Calendar inizioP2 = p2.getDataInizio();
        Calendar fineP2 = p2.getDataFine();

        //Due periodi si sovrappongono se:
        //L'inizio di uno è tra l'inizio e la fine dell'altro
        //La fine di uno è tra l'inizio e la fine dell'altro
        //Uno contiene completamente l'altro
        return !(fineP1.before(inizioP2) || inizioP1.after(fineP2));
    }

    public void aggiungiPromozione(Promozione p)
    {
        if(p == null)
            throw new IllegalArgumentException("Errore: la promozione inserita è null");
        if (verificaSovrapposizione(p))
            throw new IllegalArgumentException("Errore: la promozione si sovrappone con altre nel periodo: "+
                    p.getDataInizio().get(Calendar.DAY_OF_MONTH)+"/"+p.getDataInizio().get(Calendar.MONTH)+"/"+p.getDataInizio().get(Calendar.YEAR)+
                    "-"+p.getDataInizio().get(Calendar.DAY_OF_MONTH)+"/"+p.getDataInizio().get(Calendar.MONTH)+"/"+p.getDataInizio().get(Calendar.YEAR));
        promozioniPerTipo.get(p.getTipo()).add(p);

        //Se è una promozione fedeltà, registro tutti i clienti fedeltà come observer
        if (p.getTipo() == TipoPromozione.FEDELTA)
        {
            registraObserverPromozioneFedelta((PromozioneFedelta) p);
        }
    }

    public void rimuoviPromozione(Promozione p)
    {
        if(p == null)
            throw new IllegalArgumentException("Errore: la promozione inserita è null");
        promozioniPerTipo.get(p.getTipo()).remove(p);
    }

    public PromozioneTratta getPromozioneAttivaTratta(Tratta t)
    {
        List<Promozione> promoTratte = promozioniPerTipo.get(TipoPromozione.TRATTA);
        for(Promozione p : promoTratte)
        {
            PromozioneTratta promo = (PromozioneTratta) p;
            if(promo.getTratta().equals(t) && promo.isAttiva())
            {
                return promo;
            }
        }
        return null;
    }

    public PromozioneFedelta getPromozioneAttivaFedelta()
    {
        for(Promozione promozione : promozioniPerTipo.get(TipoPromozione.FEDELTA))
        {
            PromozioneFedelta promozioneFedelta = (PromozioneFedelta) promozione;
            if(promozioneFedelta.isAttiva())
            {
                return promozioneFedelta;
            }
        }
        return null;
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

    public PromozioneTreno getPromozioneAttivaPerTipoTreno(TipoTreno t)
    {
        List<Promozione> promoTratte = promozioniPerTipo.get(TipoPromozione.TRENO);
        for(Promozione p : promoTratte)
        {
            PromozioneTreno promo = (PromozioneTreno) p;
            if(promo.getTipoTreno() == t && promo.isAttiva())
            {
                return promo;
            }
        }
        return null;
    }

    private void registraObserverPromozioneFedelta(PromozioneFedelta promozione)
    {
        GestoreClienti gc = GestoreClienti.getInstance();

        //Ho bisogno dei clienti che siano fedelta e abbiano accettato di ricevere notifiche
        List<Cliente> clientiFedelta = gc.getClientiFedelta();

        for (Cliente cliente : clientiFedelta) {
            ObserverPromozione observer = new ObserverPromozioneFedelta(cliente);
            promozione.attach(observer);
        }

        // Notifica immediatamente tutti i clienti registrati
        promozione.notifica();
    }
}

package it.trenical.server.domain;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.gestore.CatalogoPromozione;

import java.util.List;

public class PrezzoBiglietto
{
    private final Biglietto biglietto;
    private final Viaggio viaggio; //in base alle feature del viaggio e del treno, ne calcola il prezzo
    private double prezzoBase = 0;
    private double prezzoFinale = 0;
    private double scontoApplicato = 0;

    public PrezzoBiglietto(Biglietto b, Viaggio v)
    {
        this.biglietto = b;
        this.viaggio = v;
        ricalcolaPrezzo();
    }

    public void ricalcolaPrezzo()
    {
        calcolaPrezzoBase();
        prezzoFinale = prezzoBase; // Reset del prezzo finale
        scontoApplicato = 0; // Reset dello sconto
    }

    public void applicaPromozione(Cliente c)
    {
        CatalogoPromozione cp = CatalogoPromozione.getInstance();
        double prezzo = prezzoBase;
        if(c.haAdesioneFedelta()) //controllo se la promozione è di tipo Fedelta
        {
            PromozioneFedelta promozioneFedelta = cp.getPromozioneAttivaFedelta();
            if(promozioneFedelta != null)
                promozioneFedelta.applicaSconto(prezzo);
        }
        Tratta t = viaggio.getTratta();
        PromozioneTratta promozioneTratta = cp.getPromozioneAttivaTratta(t);
        if(promozioneTratta != null)
            promozioneTratta.applicaSconto(prezzo);
        PromozioneTreno promozioneTreno = cp.getPromozioneAttivaPerTipoTreno(viaggio.getTreno().getTipo());
        if(promozioneTreno != null)
            promozioneTreno.applicaSconto(prezzo);

        scontoApplicato = prezzoBase - prezzo;
        prezzoFinale = prezzo;
    }

    private void calcolaPrezzoBase()
    {
        double kilometri = viaggio.getKilometri();
        double aggiuntaTipo = viaggio.getTreno().getTipo().getAumentoPrezzo();
        double aggiuntaServizio = biglietto.getClasseServizio().getCoefficienteAumentoPrezzo();

        prezzoBase = kilometri * aggiuntaServizio * aggiuntaTipo;
        System.out.println("prezzoBase = "+ prezzoBase);
        //questo però è solo il prezzo base
    }

    public double getPrezzo() {
        return prezzoFinale;
    }

    public double getPrezzoBase() {
        return prezzoBase;
    }

    public double getScontoApplicato() {
        return scontoApplicato;
    }
}

package it.trenical.server.domain;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.gestore.CatalogoPromozione;

import java.util.List;

public class PrezzoBiglietto
{
    private final Biglietto biglietto;
    private final Viaggio viaggio; //in base alle feature del viaggio e del treno, ne calcola il prezzo
    private double prezzo = 0;

    public PrezzoBiglietto(Biglietto b, Viaggio v)
    {
        this.biglietto = b;
        this.viaggio = v;
        calcolaPrezzo();
    }

    public void applicaPromozione(Cliente c)
    {
        CatalogoPromozione cp = CatalogoPromozione.getInstance();
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
    }

    private void calcolaPrezzo()
    {
        double kilometri = viaggio.getKilometri();
        double aggiuntaTipo = viaggio.getTreno().getTipo().getAumentoPrezzo();
        double aggiuntaServizio = biglietto.getClasseServizio().getCoefficienteAumentoPrezzo();

        prezzo = kilometri*aggiuntaServizio*aggiuntaTipo;
        //questo però è solo il prezzo iniziale
    }

    public double getPrezzo()
    {
        return prezzo;
    }
}

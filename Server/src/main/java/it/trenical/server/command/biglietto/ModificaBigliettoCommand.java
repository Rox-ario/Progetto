package it.trenical.server.command.biglietto;

import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.CalcolatorePenali;
import it.trenical.server.domain.Viaggio;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoBiglietto;
import it.trenical.server.domain.enumerations.StatoViaggio;
import it.trenical.server.domain.gestore.GestoreBanca;
import it.trenical.server.domain.gestore.GestoreBiglietti;
import it.trenical.server.domain.gestore.GestoreClienti;
import it.trenical.server.domain.gestore.GestoreViaggi;
import it.trenical.server.dto.ModificaBigliettoDTO;
import it.trenical.server.dto.RimborsoDTO;

import java.util.Calendar;

public class ModificaBigliettoCommand implements ComandoBiglietto
{
    private final ModificaBigliettoDTO modificaBigliettoDTO;

    public ModificaBigliettoCommand(ModificaBigliettoDTO modificaBigliettoDTO)
    {
        this.modificaBigliettoDTO = modificaBigliettoDTO;
    }

    @Override
    public void esegui()
    {
        GestoreBiglietti gb = GestoreBiglietti.getInstance();
        String idBiglietto = modificaBigliettoDTO.getIDBiglietto();

        Biglietto b = gb.getBigliettoPerID(idBiglietto);

        if(b == null)
            throw new IllegalArgumentException("Errore: il biglietto "+ idBiglietto + " non esiste");
        if(b.getStato() != StatoBiglietto.PAGATO)
            throw new IllegalArgumentException("Errore: il biglietto "+ idBiglietto+" non è stato ancora pagato");

        System.out.println("ModificaBigliettoCommand: Prezzo Biglietto = "+ b.getPrezzoBiglietto());
        ClasseServizio vecchiaClasse = b.getClasseServizio();
        ClasseServizio nuovaClasse = modificaBigliettoDTO.getClasseServizio();
        String idCliente = b.getIDCliente();
        String IDViaggio = b.getIDViaggio();

        //ora controllo le cose relative al viaggio
        GestoreViaggi gv = GestoreViaggi.getInstance();
        Viaggio v = gv.getViaggio(IDViaggio);
        if(v == null)
            throw new IllegalArgumentException("Errore: il viaggio "+ IDViaggio +" non esiste");

        //Controllo che il viaggio non sia già iniziato e ci siano posti disponibili
        Calendar now = Calendar.getInstance();
        if(now.after(v.getInizioReale()))
        {
            throw new IllegalArgumentException("Errore: il treno è già partito, impossibile modificare il biglietto");
        }

        if(v.getPostiDisponibiliPerClasse(nuovaClasse) <= 0)
        {
            throw new IllegalArgumentException("Errore: non ci sono posti disponibili nella classe " + nuovaClasse);
        }

        //Salvo il prezzo originale PRIMA della modifica
        double prezzoOriginale = b.getPrezzoBiglietto();
        System.out.println("Prezzo originale = "+ b.getPrezzoBiglietto());

        gb.modificaClasseServizio(b.getID(), b.getIDCliente(), b.getIDViaggio(), nuovaClasse);

        //Riapplico le promozioni con il nuovo prezzo
        GestoreClienti gc = GestoreClienti.getInstance();
        Cliente cliente = gc.getClienteById(idCliente);
        b.applicaPromozione(cliente);

        //Calcolo la differenza tariffaria
        double nuovoPrezzo = b.getPrezzoBiglietto();
        System.out.println("Nuovo prezzo = "+ nuovoPrezzo);
        double differenzaTariffaria = nuovoPrezzo - prezzoOriginale;
        System.out.println("DifferenzaTariffaria = "+ differenzaTariffaria);

        System.out.println("Modifica biglietto - Prezzo originale: " + prezzoOriginale +
                ", Nuovo prezzo: " + nuovoPrezzo +
                ", Differenza: " + differenzaTariffaria);

        //Calcolo eventuali penali
        double penale = CalcolatorePenali.calcolaPenale(now, v.getInizioReale(), differenzaTariffaria);

        //Applico il moltiplicatore per downgrade di classe se posso
        if (differenzaTariffaria < 0)  //il prezzo originale è maggiore del prezzo nuovo, quindi passo da una classe superiore a una inferiore il prezzo deve diminuire
        {
            double moltiplicatorePenale = CalcolatorePenali.calcolaPenaleDowngrade(vecchiaClasse, nuovaClasse);
            penale *= moltiplicatorePenale;
        }

        //Una volta che ho calcolato la penale, gestisco tutto il ciclo sequenziale con la banca
        GestoreBanca bancaManager = GestoreBanca.getInstance();

        System.out.println("Differenza tariffaria > 0?");
        if (differenzaTariffaria > 0)
        {
            System.out.println("Differenza tariffaria > 0");
            //Il nuovo biglietto costa di più: addebita la differenza
            double importoDaPagare = differenzaTariffaria;
            System.out.println("Addebito differenza tariffaria: " + importoDaPagare);
            if (!bancaManager.eseguiPagamento(idCliente, importoDaPagare)) //quindi se il cliente non può pagare
            {
                System.out.println("Pagamento non riuscito perché il saldo è insufficiente");
                //il pagamento quindi fallisce e notifico
                gb.modificaClasseServizio(b.getID(), b.getIDCliente(), b.getIDViaggio(), vecchiaClasse);
                v.riduciPostiDisponibiliPerClasse(vecchiaClasse, 1);
                v.incrementaPostiDisponibiliPerClasse(nuovaClasse, 1);
                throw new IllegalStateException("Pagamento della differenza tariffaria fallito. Modifica annullata.");
            }
        }
        else if (differenzaTariffaria < 0)
        {
            System.out.println("Differenza tar < 0");
            //Il nuovo biglietto costa meno: calcola il rimborso al netto della penale
            double rimborsoLordo = Math.abs(differenzaTariffaria);
            double rimborsoNetto = rimborsoLordo - penale;

            System.out.println("Rimborso lordo: " + rimborsoLordo +
                    ", Penale: " + penale +
                    ", Rimborso netto: " + rimborsoNetto);

            if (rimborsoNetto > 0)
            {
                System.out.println("Rimborso netto > 0");
                RimborsoDTO rimborso = new RimborsoDTO(b.getID(), idCliente, rimborsoNetto);
                bancaManager.rimborsa(rimborso);
            } else
            {
                System.out.println("Rimborso netto < 0");
                //La penale supera il rimborso: potrebbe essere necessario un pagamento aggiuntivo
                double importoDaPagare = Math.abs(rimborsoNetto);
                if (importoDaPagare > 0) {
                    System.out.println("La penale supera il rimborso. Importo da pagare: " + importoDaPagare);
                    if (!bancaManager.eseguiPagamento(idCliente, importoDaPagare)) {
                        // Rollback
                        gb.modificaClasseServizio(b.getID(), b.getIDCliente(), b.getIDViaggio(), vecchiaClasse);
                        v.riduciPostiDisponibiliPerClasse(vecchiaClasse, 1);
                        v.incrementaPostiDisponibiliPerClasse(nuovaClasse, 1);
                        throw new IllegalStateException("Pagamento della penale fallito. Modifica annullata.");
                    }
                }
            }
        }
        //Se differenzaTariffaria == 0, non c'è nulla da pagare o rimborsare

        // Aggiorna la disponibilità dei posti
        v.riduciPostiDisponibiliPerClasse(nuovaClasse, 1);
        v.incrementaPostiDisponibiliPerClasse(vecchiaClasse, 1);

        System.out.println("Modifica biglietto completata con successo");
    }
}

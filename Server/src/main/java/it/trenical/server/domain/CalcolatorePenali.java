package it.trenical.server.domain;

import it.trenical.server.domain.enumerations.ClasseServizio;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class CalcolatorePenali
{
    //Percentuali di penale in base a quanto manca alla partenza
    private static final double PENALE_OLTRE_7_GIORNI = 0.0;
    private static final double PENALE_3_7_GIORNI = 0.10;
    private static final double PENALE_1_3_GIORNI = 0.25;
    private static final double PENALE_MENO_24_ORE = 0.50;


    public static double calcolaPenale(Calendar dataModifica, Calendar dataPartenza, double differenzaTariffaria)
    {
        //Se il nuovo biglietto costa di più, non c'è penale
        if (differenzaTariffaria > 0)
        {
            return 0;
        }

        //Calcol0 i giorni mancanti alla partenza
        long diffInMillis = dataPartenza.getTimeInMillis() - dataModifica.getTimeInMillis();
        long giorniMancanti = TimeUnit.MILLISECONDS.toDays(diffInMillis);

        double percentualePenale;

        if (giorniMancanti > 7)
        {
            percentualePenale = PENALE_OLTRE_7_GIORNI;
        } else if (giorniMancanti >= 3)
        {
            percentualePenale = PENALE_3_7_GIORNI;
        } else if (giorniMancanti >= 1)
        {
            percentualePenale = PENALE_1_3_GIORNI;
        } else
        {
            percentualePenale = PENALE_MENO_24_ORE;
        }

        //La penale si applica solo sul valore assoluto della differenza negativa
        return Math.abs(differenzaTariffaria) * percentualePenale;
    }

    public static double calcolaPenaleDowngrade(ClasseServizio vecchiaClasse, ClasseServizio nuovaClasse)
    {
        //Se si passa da una classe superiore a una inferiore, pago penale extra
        if (vecchiaClasse == ClasseServizio.BUSINESS &&
                (nuovaClasse == ClasseServizio.ECONOMY || nuovaClasse == ClasseServizio.LOW_COST))
        {
            return 1.2;
        }
        return 1.0;
    }
}

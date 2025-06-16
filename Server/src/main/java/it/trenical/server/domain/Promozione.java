package it.trenical.server.domain;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.enumerations.StatoPromozione;
import it.trenical.server.domain.enumerations.TipoPromozione;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public interface Promozione
{
    StatoPromozione getStatoPromozione();
    String getID();
    Calendar getDataInizio();
    Calendar getDataFine();
    void setStatoPromozioneATTIVA();
    void setStatoPromozionePROGRAMMATA();
    double applicaSconto(double prezzo);
    boolean isAttiva();
    boolean isProgrammata();
    double getPercentualeSconto();
    void setPercentualeSconto(double p);
    TipoPromozione getTipo();
}

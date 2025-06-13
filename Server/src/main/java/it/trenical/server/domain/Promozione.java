package it.trenical.server.domain;

import it.trenical.server.domain.enumerations.StatoPromozione;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public interface Promozione
{
    StatoPromozione getStatoPromozione();
    String getID();
    Calendar getDataInizio();

}

package it.trenical.server.dto;

import it.trenical.server.domain.enumerations.TipoPromozione;

import java.util.Calendar;

public class PromozioneDTO
{
    private final String id;
    private final String descrizione;
    private final double percentuale_sconto;
    private final Calendar data_inizio;
    private final Calendar data_fine;
    private final TipoPromozione tipo;
    private final String tratta_id;//opzionale se è di tipo treno
    private final String tipo_treno;//opzionale se è di tipo tratta

    public PromozioneDTO(String id, String descrizione, double percentuale_sconto, Calendar data_inizio, Calendar data_fine, TipoPromozione tipo, String tratta_id, String tipo_treno) {
        this.id = id;
        this.descrizione = descrizione;
        this.percentuale_sconto = percentuale_sconto;
        this.data_inizio = data_inizio;
        this.data_fine = data_fine;
        this.tipo = tipo;
        this.tratta_id = tratta_id;
        this.tipo_treno = tipo_treno;
    }

    @Override
    public String toString()
    {
        if(tipo == TipoPromozione.FEDELTA)
            return "[" +
                "id='" + id +
                ", tipo=" + tipo +
                ", descrizione='" + descrizione +
                ", percentuale_sconto=" + percentuale_sconto +
                ", dataInizio= " + data_inizio.get(Calendar.DAY_OF_MONTH) +"/"+(data_inizio.get(Calendar.MONTH)+1)+"/"+data_inizio.get(Calendar.YEAR)+
                ", dataFine= " + data_fine.get(Calendar.DAY_OF_MONTH) +"/"+(data_fine.get(Calendar.MONTH)+1)+"/"+data_fine.get(Calendar.YEAR) +
                "]";
        else if (tipo == TipoPromozione.TRENO)
        {
            return "[" +
                    "id='" + id +
                    ", tipo=" + tipo +
                    ", descrizione='" + descrizione +
                    ", percentuale_sconto=" + percentuale_sconto +
                    ", dataInizio= " + data_inizio.get(Calendar.DAY_OF_MONTH) +"/"+(data_inizio.get(Calendar.MONTH)+1)+"/"+data_inizio.get(Calendar.YEAR)+
                    ", dataFine= " + data_fine.get(Calendar.DAY_OF_MONTH) +"/"+(data_fine.get(Calendar.MONTH)+1)+"/"+data_fine.get(Calendar.YEAR) +
                    ", idTreno = " + tipo_treno+
                    "]";
        }
        else
            return "[" +
                    "id='" + id +
                    ", tipo=" + tipo +
                    ", descrizione='" + descrizione +
                    ", percentuale_sconto=" + percentuale_sconto +
                    ", dataInizio= " + data_inizio.get(Calendar.DAY_OF_MONTH) +"/"+(data_inizio.get(Calendar.MONTH)+1)+"/"+data_inizio.get(Calendar.YEAR)+
                    ", dataFine= " + data_fine.get(Calendar.DAY_OF_MONTH) +"/"+(data_fine.get(Calendar.MONTH)+1)+"/"+data_fine.get(Calendar.YEAR) +
                    ", idTratta = " + tratta_id+
                    "]";
    }
}

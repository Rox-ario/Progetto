package it.trenical.server.dto;

import it.trenical.server.domain.Treno;
import it.trenical.server.domain.Tratta;
import it.trenical.server.domain.enumerations.StatoViaggio;
import java.util.Calendar;

public class ViaggioDTO
{
    private String ID;
    private Calendar inizio;
    private Calendar fine;
    private Treno treno;
    private Tratta tratta;
    private StatoViaggio stato;
    private int postiDisponibili;
    private String cittaPartenza;
    private String cittaArrivo;

    public ViaggioDTO() {}

    public ViaggioDTO(String ID, Calendar inizio, Calendar fine, Treno treno, Tratta tratta,
                      StatoViaggio stato, int postiDisponibili, String cittaPartenza, String cittaArrivo) {
        this.ID = ID;
        this.inizio = inizio;
        this.fine = fine;
        this.treno = treno;
        this.tratta = tratta;
        this.stato = stato;
        this.postiDisponibili = postiDisponibili;
        this.cittaPartenza = cittaPartenza;
        this.cittaArrivo = cittaArrivo;
    }


    public String getID()
    {
        return ID;
    }
    public void setID(String ID)
    {
        this.ID = ID;
    }

    public Calendar getInizio()
    {
        return inizio;
    }
    public void setInizio(Calendar inizio)
    {
        this.inizio = inizio;
    }

    public Calendar getFine()
    {
        return fine;
    }
    public void setFine(Calendar fine)
    {
        this.fine = fine;
    }

    public Treno getTreno()
    {
        return treno;
    }
    public void setTreno(Treno treno)
    {
        this.treno = treno;
    }

    public Tratta getTratta()
    {
        return tratta;
    }
    public void setTratta(Tratta tratta)
    {
        this.tratta = tratta;
    }

    public StatoViaggio getStato()
    {
        return stato;
    }
    public void setStato(StatoViaggio stato)
    {
        this.stato = stato;
    }

    public int getPostiDisponibili()
    {
        return postiDisponibili;
    }
    public void setPostiDisponibili(int postiDisponibili)
    {
        this.postiDisponibili = postiDisponibili;
    }

    public String getCittaPartenza()
    {
        return cittaPartenza;
    }
    public void setCittaPartenza(String cittaPartenza)
    {
        this.cittaPartenza = cittaPartenza;
    }

    public String getCittaArrivo()
    {
        return cittaArrivo;
    }
    public void setCittaArrivo(String cittaArrivo)
    {
        this.cittaArrivo = cittaArrivo;
    }

    @Override
    public String toString() {
        return "ViaggioDTO{" +
                "ID='" + ID + '\'' +
                ", inizio=" + inizio.getTime() +
                ", fine=" + fine.getTime() +
                ", stato=" + stato +
                ", cittaPartenza='" + cittaPartenza + '\'' +
                ", cittaArrivo='" + cittaArrivo + '\'' +
                ", postiDisponibili=" + postiDisponibili +
                '}';
    }
}

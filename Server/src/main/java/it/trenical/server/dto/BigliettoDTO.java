package it.trenical.server.dto;

import it.trenical.server.domain.PrezzoBiglietto;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoBiglietto;

import java.util.Calendar;

public class BigliettoDTO
{
    private  String ID;
    private  String IDViaggio;
    private  ClasseServizio classeServizio;
    private  String IDCliente;
    private  Calendar dataAcquisto;
    private  StatoBiglietto statoBiglietto;
    private  double prezzo;

    public BigliettoDTO(String ID,
                        String IDViaggio,
                        ClasseServizio classeServizio,
                        String IDCliente,
                        Calendar dataAcquisto,
                        StatoBiglietto statoBiglietto,
                        double prezzo) {
        this.ID = ID;
        this.IDViaggio = IDViaggio;
        this.classeServizio = classeServizio;
        this.IDCliente = IDCliente;
        this.dataAcquisto = dataAcquisto;
        this.statoBiglietto = statoBiglietto;
        this.prezzo = prezzo;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getIDViaggio() {
        return IDViaggio;
    }

    public void setIDViaggio(String IDViaggio) {
        this.IDViaggio = IDViaggio;
    }

    public ClasseServizio getClasseServizio() {
        return classeServizio;
    }

    public void setClasseServizio(ClasseServizio classeServizio) {
        this.classeServizio = classeServizio;
    }

    public String getIDCliente() {
        return IDCliente;
    }

    public void setIDCliente(String IDCliente) {
        this.IDCliente = IDCliente;
    }

    public Calendar getDataAcquisto() {
        return dataAcquisto;
    }

    public void setDataAcquisto(Calendar dataAcquisto) {
        this.dataAcquisto = dataAcquisto;
    }

    public StatoBiglietto getStatoBiglietto() {
        return statoBiglietto;
    }

    public void setStatoBiglietto(StatoBiglietto statoBiglietto) {
        this.statoBiglietto = statoBiglietto;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public void setPrezzo(double prezzo) {
        this.prezzo = prezzo;
    }

    @Override
    public String toString() {
        return "BigliettoDTO{" +
                "ID='" + ID + '\'' +
                ", IDViaggio='" + IDViaggio + '\'' +
                ", classeServizio=" + classeServizio +
                ", IDCliente='" + IDCliente + '\'' +
                ", dataAcquisto=" + dataAcquisto +
                ", statoBiglietto=" + statoBiglietto +
                ", prezzo=" + prezzo +
                '}';
    }
}

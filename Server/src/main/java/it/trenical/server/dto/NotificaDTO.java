package it.trenical.server.dto;

import java.util.Calendar;

public class NotificaDTO {
    private final String messaggio;
    private final Calendar timestamp;

    public NotificaDTO(String messaggio) {
        this.messaggio = messaggio;
        this.timestamp = Calendar.getInstance();
    }

    public String getMessaggio() { return messaggio; }
    public Calendar getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp.getTime() + "] aggiornamento:"+ messaggio;
    }
}


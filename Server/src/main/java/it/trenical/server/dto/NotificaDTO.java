package it.trenical.server.dto;

import java.util.Calendar;

public class NotificaDTO {
    private final String messaggio;
    private Calendar timestamp;

    public NotificaDTO(String messaggio)
    {
        this.messaggio = messaggio;
        this.timestamp = Calendar.getInstance();
    }

    //per riprenderlo da DB
    public NotificaDTO(String messaggio, Calendar timestamp)
    {
        this.messaggio = messaggio;
        this.timestamp = timestamp;
    }

    public String getMessaggio()
    {
        return messaggio;
    }
    public Calendar getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Calendar timestamp)
    {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp.getTime() + "] aggiornamento:"+ messaggio;
    }

    public String getTempoStampato()
    {
        return timestamp.get(Calendar.DAY_OF_MONTH)+"/"+(timestamp.get(Calendar.MONTH)+1)+"/"+timestamp.get(Calendar.YEAR)
                +" "+timestamp.get(Calendar.HOUR_OF_DAY)+":"+timestamp.get(Calendar.MINUTE);
        //esempio 16/06/2025 17:51
    }
}


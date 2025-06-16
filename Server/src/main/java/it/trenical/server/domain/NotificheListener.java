package it.trenical.server.domain;

import it.trenical.server.dto.NotificaDTO;

public class NotificheListener
{
    private final boolean mostraIdCliente;

    public NotificheListener(boolean mostraIdCliente) {
        this.mostraIdCliente = mostraIdCliente;
    }

    public void onNuovaNotifica(String idCliente, NotificaDTO notifica)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("NUOVA NOTIFICA");
        if (mostraIdCliente) {
            sb.append(" per ").append(idCliente).append("...");
        }
        sb.append("\n");
        sb.append(notifica.getTempoStampato());

        //Spezzo il messaggio tramite split fortissimo
        String[] righe = notifica.getMessaggio().split("\n");
        for (String riga : righe)
        {
            sb.append(">>").append(riga).append("\n");
        }

        System.out.println(sb.toString());
    }
}

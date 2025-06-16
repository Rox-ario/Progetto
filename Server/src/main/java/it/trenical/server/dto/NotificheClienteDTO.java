package it.trenical.server.dto;

import java.util.List;

public class NotificheClienteDTO
{
    private final String idCliente;
    private final List<NotificaDTO> notifiche;
    private final int numeroNotifiche;
    private final boolean soloNonLette;

    public NotificheClienteDTO(String idCliente, List<NotificaDTO> notifiche, boolean soloNonLette)
    {
        this.idCliente = idCliente;
        this.notifiche = notifiche;
        this.numeroNotifiche = notifiche.size();
        this.soloNonLette = soloNonLette;
    }

    public String getIdCliente() { return idCliente; }
    public List<NotificaDTO> getNotifiche() { return notifiche; }
    public int getNumeroNotifiche() { return numeroNotifiche; }
    public boolean isSoloNonLette() { return soloNonLette; }
}


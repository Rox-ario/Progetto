package it.trenical.server.command.cliente;

import it.trenical.server.domain.gestore.GestoreNotifiche;
import it.trenical.server.dto.NotificaDTO;
import it.trenical.server.dto.NotificheClienteDTO;

import java.util.List;

public class RecuperaNotificheCommand implements ComandoCliente {
    private final String idCliente;
    private final boolean soloNonLette; // true = non lette, false = storico completo
    private NotificheClienteDTO risultato;

    public RecuperaNotificheCommand(String idCliente, boolean soloNonLette) {
        this.idCliente = idCliente;
        this.soloNonLette = soloNonLette;
    }

    @Override
    public void esegui() throws Exception {
        GestoreNotifiche gn = GestoreNotifiche.getInstance();
        List<NotificaDTO> notifiche;

        if (soloNonLette) {
            notifiche = gn.getNotificheNonLette(idCliente);
        } else {
            notifiche = gn.getStoricoNotifiche(idCliente);
        }

        risultato = new NotificheClienteDTO(idCliente, notifiche, soloNonLette);
    }

    public NotificheClienteDTO getRisultato() {
        return risultato;
    }
}

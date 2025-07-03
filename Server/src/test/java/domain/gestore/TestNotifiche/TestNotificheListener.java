package domain.gestore.TestNotifiche;

import it.trenical.server.domain.NotificheListener;
import it.trenical.server.dto.NotificaDTO;

import java.util.ArrayList;
import java.util.List;

public class TestNotificheListener extends NotificheListener
{
    private final List<NotificaDTO> notificheRicevute = new ArrayList<>();
    private final List<String> clientiNotificati = new ArrayList<>();

    public TestNotificheListener()
    {
        super(false); //Non mostra ID cliente nel log
    }

    @Override
    public void onNuovaNotifica(String idCliente, NotificaDTO notifica)
    {
        notificheRicevute.add(notifica);
        clientiNotificati.add(idCliente);
    }

    public List<NotificaDTO> getNotificheRicevute()
    {
        return new ArrayList<>(notificheRicevute);
    }

    public void reset()
    {
        notificheRicevute.clear();
        clientiNotificati.clear();
    }

    public boolean haRicevutoNotifiche()
    {
        return !notificheRicevute.isEmpty();
    }
}

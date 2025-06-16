package it.trenical.server.command.cliente;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.gestore.GestoreClienti;

public class AderisciAFedeltaCommand implements ComandoCliente
{
    private final String idUtente;
    private final boolean attivaNotifichePromozioni;

    public AderisciAFedeltaCommand(String idUtente, boolean attivaNotifichePromozioni)
    {
        this.idUtente = idUtente;
        this.attivaNotifichePromozioni = attivaNotifichePromozioni;
    }

    @Override
    public void esegui() throws Exception {
        GestoreClienti gc = GestoreClienti.getInstance();
        if (!gc.esisteClienteID(idUtente)) {
            throw new IllegalArgumentException("Cliente non trovato");
        }

        Cliente vecchio = gc.getClienteById(idUtente);
        Cliente nuovo = new Cliente.Builder()
                .ID(vecchio.getId())
                .Email(vecchio.getEmail())
                .Nome(vecchio.getNome())
                .Cognome(vecchio.getCognome())
                .Password(vecchio.getPassword())
                .isFedelta(true)
                .riceviNotifiche(vecchio.isRiceviNotifiche())
                .riceviPromozioni(attivaNotifichePromozioni) // Può scegliere se attivarle
                .build();

        gc.aggiornaCliente(idUtente, nuovo);
    }
}

package it.trenical.server.command.cliente;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.gestore.GestoreClienti;

public class RimuoviFedeltaCommand implements ComandoCliente {
    private final String idUtente;

    public RimuoviFedeltaCommand(String idUtente) {
        this.idUtente = idUtente;
    }

    @Override
    public void esegui() throws Exception {
        GestoreClienti gc = GestoreClienti.getInstance();
        Cliente vecchio = gc.getClienteById(idUtente);

        if (vecchio == null) {
            throw new IllegalArgumentException("Cliente non trovato");
        }

        if (!vecchio.haAdesioneFedelta()) {
            throw new IllegalArgumentException("Il cliente non è iscritto al programma fedeltà");
        }

        //Rimuvo fedeltà E notifiche promozioni
        Cliente nuovo = new Cliente.Builder()
                .ID(vecchio.getId())
                .Email(vecchio.getEmail())
                .Nome(vecchio.getNome())
                .Cognome(vecchio.getCognome())
                .Password(vecchio.getPassword())
                .isFedelta(false)
                .riceviNotifiche(vecchio.isRiceviNotifiche())
                .riceviPromozioni(false) // AUTOMATICAMENTE false
                .build();

        gc.aggiornaCliente(idUtente, nuovo);
    }
}

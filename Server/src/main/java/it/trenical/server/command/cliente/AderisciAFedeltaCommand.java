package it.trenical.server.command.cliente;

import it.trenical.server.domain.Cliente;
import it.trenical.server.domain.gestore.GestoreClienti;

public class AderisciAFedeltaCommand implements ComandoCliente
{
    private final String idUtente;

    public AderisciAFedeltaCommand(String idUtente) {
        this.idUtente = idUtente;
    }

    @Override
    public void esegui() throws Exception
    {
        GestoreClienti gc = GestoreClienti.getInstance();
        if(!gc.esisteClienteID(idUtente))
        {
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
                .build();

        gc.aggiornaCliente(idUtente, nuovo);

    }
}

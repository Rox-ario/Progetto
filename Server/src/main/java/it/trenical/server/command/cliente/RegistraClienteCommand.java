package it.trenical.server.command.cliente;

import it.trenical.server.domain.Cliente;
import it.trenical.server.domain.gestore.GestoreClienti;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.utils.ClienteAssembler;

public class RegistraClienteCommand implements ComandoCliente {

    private final ClienteDTO dto;
    private final String password;

    public RegistraClienteCommand(ClienteDTO dto, String password) {
        this.dto = dto;
        this.password = password;
    }

    @Override
    public void esegui() {
        GestoreClienti clienti = GestoreClienti.getInstance();

        if (clienti.esisteClienteEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email già registrata");
        }

        Cliente cliente = ClienteAssembler.fromDTO(dto, password);

        clienti.aggiungiCliente(cliente);
    }
}


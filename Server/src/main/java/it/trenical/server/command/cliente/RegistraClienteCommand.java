package it.trenical.server.command.cliente;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.gestore.GestoreClienti;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.utils.ClienteAssembler;

public class RegistraClienteCommand implements ComandoCliente
{
    private final ClienteDTO dto;
    private final String password;

    public RegistraClienteCommand(ClienteDTO dto, String password) {
        this.dto = dto;
        this.password = password;

        //Se il nuovo cliente è non fedeltà allor non può avere notifiche sulle promozioni fedeltà
        if (!dto.isFedelta() && dto.isRiceviPromozioni())
        {
            dto.setRiceviPromozioni(false);
        }
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


package it.trenical.server.command.cliente;

import it.trenical.server.domain.Cliente;
import it.trenical.server.domain.GestoreClienti;
import it.trenical.server.dto.ModificaClienteDTO;
import it.trenical.server.utils.ClienteAssembler;

public class ModificaClienteCommand implements ComandoCliente {
    private final String idCliente;
    private final ModificaClienteDTO dto;

    public ModificaClienteCommand(String idCliente, ModificaClienteDTO dto) {
        this.idCliente = idCliente;
        this.dto = dto;
    }

    @Override
    public void esegui() {
        GestoreClienti gestore = GestoreClienti.getInstance();
        Cliente vecchio = gestore.getClienteById(idCliente);

        if (vecchio == null) {
            throw new IllegalArgumentException("Cliente non trovato");
        }

        Cliente nuovo = ClienteAssembler.applicaModifiche(dto, vecchio);
        gestore.aggiornaCliente(idCliente, nuovo);
    }
}


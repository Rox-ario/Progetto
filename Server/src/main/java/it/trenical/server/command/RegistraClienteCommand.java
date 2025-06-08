package it.trenical.server.command;

import it.trenical.server.domain.Cliente;
import it.trenical.server.domain.ClienteBanca;
import it.trenical.server.domain.GestoreBanca;
import it.trenical.server.domain.GestoreClienti;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.dto.DatiBancariDTO;
import it.trenical.server.utils.ClienteAssembler;

public class RegistraClienteCommand implements ComandoCliente{

    private final ClienteDTO dto;
    private final String password;
    private final DatiBancariDTO datiBancari;

    public RegistraClienteCommand(ClienteDTO dto, String password, DatiBancariDTO datiBancari) {
        this.dto = dto;
        this.password = password;
        this.datiBancari = datiBancari;
    }

    @Override
    public void esegui() {
        GestoreClienti clienti = GestoreClienti.getInstance();
        GestoreBanca banca = GestoreBanca.getInstance();

        if (clienti.esisteClienteEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email già registrata");
        }

        Cliente cliente = ClienteAssembler.fromDTO(dto, password);
        ClienteBanca cb = ClienteAssembler.daDatiBancariDTO(datiBancari, cliente.getId(), cliente.getNome());

        clienti.aggiungiCliente(cliente);
        banca.registraClienteBanca(cb);
    }
}


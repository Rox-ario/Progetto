package it.trenical.server.command.cliente;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.gestore.GestoreClienti;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.dto.DatiBancariDTO;
import it.trenical.server.utils.Assembler;

public class RegistraClienteCommand implements ComandoCliente
{
    private final ClienteDTO dto;
    private final String password;
    private final DatiBancariDTO datiBancari;

    public RegistraClienteCommand(ClienteDTO dto, String password) {
        this.dto = dto;
        this.password = password;
        this.datiBancari = null;

        //Se il nuovo cliente è non fedeltà allor non può avere notifiche sulle promozioni fedeltà
        if (!dto.isFedelta() && dto.isRiceviPromozioni())
        {
            dto.setRiceviPromozioni(false);
        }
    }

    public RegistraClienteCommand(ClienteDTO dto, String password, DatiBancariDTO datiBancari) {
        this.dto = dto;
        this.password = password;
        this.datiBancari = datiBancari;

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

        Cliente cliente = Assembler.fromDTO(dto, password);
        clienti.aggiungiCliente(cliente, datiBancari);
    }
}


package it.trenical.server.command.cliente;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.gestore.GestoreClienti;
import it.trenical.server.dto.ModificaClienteDTO;
import it.trenical.server.utils.ClienteAssembler;

public class ModificaClienteCommand implements ComandoCliente
{
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

        // Se sta rimuovendo fedeltà, deve anche rimuovere notifiche promozioni
        boolean riceviPromozioni = vecchio.isRiceviPromozioni();
        if (!dto.isFedelta() && vecchio.haAdesioneFedelta()) {
            riceviPromozioni = false;
        }

        Cliente nuovo = new Cliente.Builder()
                .ID(vecchio.getId())
                .Email(vecchio.getEmail())
                .Nome(dto.getNome())
                .Cognome(dto.getCognome())
                .Password(dto.getPassword())
                .isFedelta(dto.isFedelta())
                .riceviNotifiche(vecchio.isRiceviNotifiche())
                .riceviPromozioni(riceviPromozioni)
                .build();

        gestore.aggiornaCliente(idCliente, nuovo);
    }
}


package it.trenical.server.utils;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.cliente.ClienteBanca;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.dto.DatiBancariDTO;
import it.trenical.server.dto.ModificaClienteDTO;

public class ClienteAssembler
{
    public static ClienteDTO toDTO(Cliente cliente)
    {
        return new ClienteDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getCognome(),
                cliente.getEmail(),
                cliente.getPassword(),
                cliente.haAdesioneFedelta(),
                cliente.isRiceviNotifiche(),
                cliente.isRiceviPromozioni());
    }

    public static Cliente fromDTO(ClienteDTO dto, String password)
    {
        return new Cliente.Builder().ID(dto.getId())
                .Nome(dto.getNome()).Cognome(dto.getCognome())
                .Email(dto.getEmail()).Password(password)
                .isFedelta(dto.isFedelta()).build();
    }

    public static ClienteBanca daDatiBancariDTO(DatiBancariDTO dto, String idCliente, String nomeCliente) {
        return new ClienteBanca(
                idCliente,
                nomeCliente,
                dto.getNomeBanca(),
                dto.getNumeroCarta(),
                dto.getSaldo()
        );
    }

    public static DatiBancariDTO aDatiBancariDTO(ClienteBanca cb) {
    return new DatiBancariDTO(
            cb.getBanca(),
            cb.getNumeroCarta(),
            cb.getSaldo()
    );
}

    public static Cliente applicaModifiche(ModificaClienteDTO dto, Cliente vecchio) {
        return new Cliente.Builder()
                .ID(vecchio.getId())
                .Email(vecchio.getEmail()) // non modificabile
                .Nome(dto.getNome())
                .Cognome(dto.getCognome())
                .Password(dto.getPassword())
                .isFedelta(dto.isFedelta())
                .build();
    }
}

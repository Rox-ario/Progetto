package it.trenical.server.utils;

import it.trenical.server.domain.Biglietto;
import it.trenical.server.domain.Viaggio;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.cliente.ClienteBanca;
import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.dto.*;

import java.util.Calendar;

public class Assembler
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
                .isFedelta(dto.isFedelta())
                .riceviNotifiche(dto.isRiceviNotifiche())
                .riceviPromozioni(dto.isRiceviPromozioni())
                .build();
    }

    public static ClienteBanca daDatiBancariDTO(DatiBancariDTO dto)
    {
        return new ClienteBanca(
                dto.getIdCliente(),
                dto.getNomeCliente(),
                dto.getCognome(),
                dto.getNomeBanca(),
                dto.getNumeroCarta(),
                dto.getSaldo()
        );
    }

    public static DatiBancariDTO aDatiBancariDTO(ClienteBanca cb)
    {
        return new DatiBancariDTO(
                cb.getIdCliente(),
            cb.getNome(),
            cb.getCognome(),
            cb.getBanca(),
            cb.getNumeroCarta(),
            cb.getSaldo()
        );
    }

    public static Cliente applicaModifiche(ModificaClienteDTO dto, Cliente vecchio)
    {
        return new Cliente.Builder()
                .ID(vecchio.getId())
                .Email(vecchio.getEmail()) // non modificabile
                .Nome(dto.getNome())
                .Cognome(dto.getCognome())
                .Password(dto.getPassword())
                .isFedelta(dto.isFedelta())
                .riceviNotifiche(vecchio.isRiceviNotifiche())
                .riceviPromozioni(vecchio.isRiceviPromozioni())
                .build();
    }

    public static BigliettoDTO bigliettoToDTO(Biglietto biglietto)
    {
        return new BigliettoDTO(
                biglietto.getID(),
                biglietto.getIDViaggio(),
                biglietto.getClasseServizio(),
                biglietto.getIDCliente(),
                biglietto.getDataAcquisto(),
                biglietto.getStato(),
                biglietto.getPrezzo()
        );
    }

    public static ViaggioDTO viaggioToDTO(Viaggio v)
    {
        int postiTotali = 0;
        for (ClasseServizio classe : ClasseServizio.values()) {
            postiTotali += v.getPostiDisponibiliPerClasse(classe);
        }

        String cittaPartenza = v.getTratta().getStazionePartenza().getCitta();
        String cittaArrivo = v.getTratta().getStazioneArrivo().getCitta();

        Calendar inizioReale = v.getInizioReale();
        Calendar fineReale = v.getFineReale();

        return new ViaggioDTO(
                v.getId(),
                inizioReale,
                fineReale,
                v.getTreno(),
                v.getTratta(),
                v.getStato(),
                postiTotali,
                cittaPartenza,
                cittaArrivo,
                v.getKilometri()
        );
    }
}

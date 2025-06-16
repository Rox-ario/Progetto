package it.trenical.server.command.cliente;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.gestore.GestoreClienti;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.utils.ClienteAssembler;

public class LoginClienteCommand implements ComandoCliente
{
    private final String email;
    private final String password;
    private ClienteDTO dto;

    public LoginClienteCommand(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @Override
    public void esegui() throws Exception
    {
        GestoreClienti gc = GestoreClienti.getInstance();
        if(!gc.autenticaCliente(email, password))
            throw new IllegalArgumentException("E' stato impossibile autenticare il cliente: "+ email);
        this.dto = ClienteAssembler.toDTO(gc.getClienteByEmail(email));
    }

    public ClienteDTO getClienteDTO()
    {
        return dto;
    }
}

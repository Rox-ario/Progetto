package it.trenical.server.command;

import it.trenical.server.domain.Cliente;
import it.trenical.server.domain.GestoreClienti;
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
        if (!gc.esisteClienteEmail(email))
            throw new IllegalArgumentException("Non esiste il cliente "+ email);

        Cliente c = gc.getClienteByEmail(email);
        if(!c.getPassword().equals(password))
            throw new IllegalArgumentException("Password errata");

        this.dto = ClienteAssembler.toDTO(gc.getClienteByEmail(email));
    }

    public ClienteDTO getClienteDTO()
    {
        return dto;
    }
}

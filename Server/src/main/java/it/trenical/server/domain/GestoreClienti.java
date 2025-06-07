package it.trenical.server.domain;

import java.util.HashMap;
import java.util.Map;

public class GestoreClienti
{
    private static GestoreClienti instance = null;
    private final Map<String, Cliente> clientiById;
    private final Map<String, Cliente> clientiByEmail;

    private GestoreClienti()
    {
        clientiById = new HashMap<>();
        clientiByEmail = new HashMap<>();
    }

    public static GestoreClienti getInstance()
    {
        if (instance == null) {
            instance = new GestoreClienti();
        }
        return instance;
    }

    public boolean esisteClienteID(String id)
    {
        if(clientiById.containsKey(id))
        {
           return true;
        }
        return false;
    }

    public boolean esisteClienteEmail(String email)
    {
        if(clientiByEmail.containsKey(email))
        {
            return true;
        }
        return false;
    }

    public Cliente getClienteById(String id)
    {
        if(clientiById.containsKey(id))
            return clientiById.get(id);
        return null;
    }

    public Cliente getClienteByEmail(String email)
    {
        if(clientiByEmail.containsKey(email))
            return clientiByEmail.get(email);
        return null;
    }

    public void aggiungiCliente(Cliente c)
    {
        String id = c.getId();
        String email = c.getEmail();

        clientiById.putIfAbsent(id, c);
        clientiByEmail.putIfAbsent(email, c);
    }

    public boolean autenticaCliente(String email, String password)
    {
        if(!esisteClienteEmail(email))
            return false;
        String pwC = clientiByEmail.get(email).getPassword();
        return pwC.equals(password);
    }
}

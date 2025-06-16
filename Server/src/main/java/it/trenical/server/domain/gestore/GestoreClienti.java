package it.trenical.server.domain.gestore;

import it.trenical.server.domain.cliente.Cliente;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GestoreClienti
{
    private static GestoreClienti instance = null;
    private final Map<String, Cliente> clientiById;
    private final Map<String, Cliente> clientiByEmail;

    private GestoreClienti()
    {
        clientiById = new HashMap<>();
        clientiByEmail = new HashMap<>();
    }

    public static synchronized GestoreClienti getInstance()
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

    public void aggiornaCliente(String idCliente, Cliente nuovo)
    {
        if(!esisteClienteID(idCliente))
            return;
        Cliente vecchio = clientiById.get(idCliente);
        String email = vecchio.getEmail();

        clientiById.put(idCliente, nuovo);

        if(!email.equals(nuovo.getEmail()))
        {
            clientiByEmail.remove(email);
        }

        clientiByEmail.put(nuovo.getEmail(), nuovo);
    }

    public List<Cliente> getClientiFedelta()
    {
        List<Cliente> clientiFedelta = new ArrayList<>();
        for(Cliente c : clientiById.values())
        {
            if(c.haAdesioneFedelta())
                clientiFedelta.add(c);
        }
        return clientiFedelta;
    }

    public boolean validaPreferenzeNotifiche(String idCliente, boolean riceviPromozioni)
    {
        Cliente cliente = getClienteById(idCliente);
        if (cliente == null)
            return false;
        //Se vuole ricevere promozioni, deve essere fedeltà
        if (riceviPromozioni && !cliente.haAdesioneFedelta())
        {
            return false;
        }
        return true;
    }
}

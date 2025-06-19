package it.trenical.client.singleton;

import it.trenical.server.dto.ClienteDTO;

public class SessioneCliente
{
    private static SessioneCliente instance;
    private String idClienteLoggato;
    private ClienteDTO clienteCorrente;

    private SessioneCliente() {}

    public static synchronized SessioneCliente getInstance() {
        if (instance == null) {
            instance = new SessioneCliente();
        }
        return instance;
    }

    public void login(ClienteDTO cliente)
    {
        this.idClienteLoggato = cliente.getId();
        this.clienteCorrente = cliente;
        System.out.println("Sessione avviata per: " + cliente.getNome() + " " + cliente.getCognome());
    }

    public void logout()
    {
        if (idClienteLoggato != null) {
            System.out.println("Logout effettuato per: " + clienteCorrente.getNome());
        }
        this.idClienteLoggato = null;
        this.clienteCorrente = null;
    }

    public String getIdClienteLoggato()
    {
        return idClienteLoggato;
    }

    public ClienteDTO getClienteCorrente()
    {
        return clienteCorrente;
    }

    public boolean isLoggato()
    {
        return idClienteLoggato != null && clienteCorrente != null;
    }

    public String getNomeClienteCorrente()
    {
        if(isLoggato())
            return clienteCorrente.getNome();
        return "Ospite";
    }
}

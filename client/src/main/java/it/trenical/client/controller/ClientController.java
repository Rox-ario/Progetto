package it.trenical.client.controller;

import it.trenical.server.command.cliente.RegistraClienteCommand;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.grpc.ControllerGRPC;

import java.util.UUID;

public class ClientController
{


    public void registrati(String nome, String cognome, String email, String password, boolean fedelta) {
        String id = UUID.randomUUID().toString();

        ClienteDTO dto = new ClienteDTO(id, nome, cognome, email, fedelta);

        RegistraClienteCommand comando = new RegistraClienteCommand(dto, password);

        try {
            ControllerGRPC.eseguiComandoCliente(comando);
            System.out.println("Registrazione completata con successo.");
        } catch (Exception e) {
            System.err.println("Errore durante la registrazione: " + e.getMessage());
        }
    }
}

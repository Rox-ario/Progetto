package it.trenical.server.grpc;
import io.grpc.stub.StreamObserver;
import it.trenical.grpc.*;
import it.trenical.server.command.cliente.*;
import it.trenical.server.dto.ClienteDTO;

/*
 Implementazione del servizio di autenticazione gRPC.
 Gestisce login, registrazione e logout dei clienti.
 Questo servizio fa da ponte tra le richieste gRPC e i Command del dominio.
 */
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase
{
    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver)
    {
        System.out.println("Richiesta login per: " + request.getEmail());

        try
        {
            LoginClienteCommand command = new LoginClienteCommand(
                    request.getEmail(),
                    request.getPassword()
            );
            command.esegui();

            ClienteDTO clienteDTO = command.getClienteDTO();

            ClienteInfo clienteInfo = ClienteInfo.newBuilder()
                    .setId(clienteDTO.getId())
                    .setNome(clienteDTO.getNome())
                    .setCognome(clienteDTO.getCognome())
                    .setEmail(clienteDTO.getEmail())
                    .setIsFedelta(clienteDTO.isFedelta())
                    .build();

            LoginResponse response = LoginResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Login effettuato con successo")
                    .setCliente(clienteInfo)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.out.println("Login completato per: " + clienteDTO.getEmail());

        }
        catch (Exception e)
        {
            LoginResponse response = LoginResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Errore login: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.err.println("Login fallito: " + e.getMessage());
        }
    }

    @Override
    public void registra(RegistraRequest request, StreamObserver<RegistraResponse> responseObserver) {
        System.out.println("Richiesta registrazione per: " + request.getEmail());

        try
        {
            //creo un il DTO dal request gRPC
            ClienteDTO clienteDTO = new ClienteDTO();
            clienteDTO.setId(java.util.UUID.randomUUID().toString());
            clienteDTO.setNome(request.getNome());
            clienteDTO.setCognome(request.getCognome());
            clienteDTO.setEmail(request.getEmail());
            clienteDTO.setPassword(request.getPassword());
            clienteDTO.setFedelta(request.getIsFedelta());
            clienteDTO.setRiceviNotifiche(request.getRiceviNotifiche());
            clienteDTO.setRiceviPromozioni(request.getRiceviPromozioni());

            //il command per la registrazione
            RegistraClienteCommand command = new RegistraClienteCommand(
                    clienteDTO,
                    request.getPassword()
            );
            command.esegui();

            //mi serve la risposta di successoo
            RegistraResponse response = RegistraResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Registrazione completata con successo")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.out.println("Registrazione completata per: " + request.getEmail());

        }
        catch (Exception e)
        {
            RegistraResponse response = RegistraResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Errore registrazione: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.err.println("Registrazione fallita: " + e.getMessage());
        }
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver)
    {
        System.out.println("Richiesta logout per cliente: " + request.getClienteId());

        //dato che la sessione del client gestisce la maledetta logout, il server non deve fare molto

        LogoutResponse response = LogoutResponse.newBuilder()
                .setSuccess(true)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        System.out.println("Logout completato per: " + request.getClienteId());
    }
}

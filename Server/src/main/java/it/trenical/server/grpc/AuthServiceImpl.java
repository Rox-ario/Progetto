package it.trenical.server.grpc;
import io.grpc.stub.StreamObserver;
import it.trenical.grpc.*;
import it.trenical.server.command.cliente.*;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.dto.DatiBancariDTO;

/*
 Implementazione del servizio di autenticazione gRPC.
 Gestisce login, registrazione e logout dei clienti.
 Questo servizio fa da ponte tra le richieste gRPC e i Command del dominio.
 */
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase
{
    private final ControllerGRPC controllerGRPC = ControllerGRPC.getInstance();
    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver)
    {
        System.out.println("Richiesta login per: " + request.getEmail());

        try
        {
            ClienteDTO clienteDTO = controllerGRPC.login(request.getEmail(), request.getPassword());

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

            //invio la risposta e completo
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

            DatiBancariDTO datiBancari = null;
            if (!request.getNumeroCarta().isEmpty())
            {
                datiBancari = new DatiBancariDTO();
                datiBancari.setIdCliente(clienteDTO.getId());
                datiBancari.setNomeCliente(clienteDTO.getNome());
                datiBancari.setCognome(clienteDTO.getCognome());
                datiBancari.setNumeroCarta(request.getNumeroCarta());
                datiBancari.setSaldo(1000.00); // Saldo iniziale standard
            }

            //uso il facade
            controllerGRPC.registraCliente(clienteDTO, datiBancari);;

            //invio la risposta di successo
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

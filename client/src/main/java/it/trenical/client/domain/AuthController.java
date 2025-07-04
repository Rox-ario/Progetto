package it.trenical.client.domain;

import it.trenical.client.grpc.ServerProxy;
import it.trenical.client.singleton.SessioneCliente;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.grpc.ControllerGRPC;

import java.util.UUID;

public class AuthController
{

    private final String regex_email = "^[a-zA-Z0-9._%+-]+@gmail\\.com$";
    private final String regex_password = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])[a-zA-Z0-9@#$%^&+=]{8,}$";
    //regex password indica
    /*
    ^[a-zA-Z@#$%^&+=]               → Inizia con una lettera (maiuscola o minuscola) o uno di questi simboli: @ # $ % ^ & + =
(?=.*[0-9])                     → Deve contenere almeno **una cifra** (0–9)
(?=.*[a-z])                     → Deve contenere almeno **una lettera minuscola** (a–z)
(?=.*[A-Z])                     → Deve contenere almeno **una lettera maiuscola** (A–Z)
(?=.*[@#$%^&+=])                → Deve contenere almeno **uno dei simboli speciali** @, #, $, %, ^, &, +, =
.{8,}                           → Deve essere **lunghezza minima 8** caratteri (può includere tutto)
[a-zA-Z0-9]$                    → Finisce con **una lettera** (minuscola o maiuscola) o **una cifra**
     */
    private final String regex_carta = "^[0-9]{4}[- ]?[0-9]{4}[- ]?[0-9]{4}[- ]?[0-9]{4}$";

    public AuthController()
    {}

    public boolean registrati(String nome, String cognome, String email, String password,
                              String numeroCarta,
                              boolean isFedelta, boolean wantsNotifiche, boolean wantsPromozioni)
    {
        try
        {
            if (!validaInputRegistrazione(nome, cognome, email, password, numeroCarta))
            {
                return false;
            }

            ClienteDTO nuovoCliente = new ClienteDTO();
            nuovoCliente.setId(UUID.randomUUID().toString());
            nuovoCliente.setNome(nome.trim());
            nuovoCliente.setCognome(cognome.trim());
            nuovoCliente.setEmail(email.trim());
            nuovoCliente.setPassword(password);
            nuovoCliente.setFedelta(isFedelta);
            nuovoCliente.setRiceviNotifiche(wantsNotifiche);
            nuovoCliente.setRiceviPromozioni(wantsPromozioni);

            System.out.println("Registrazione cliente: \n"
            + nuovoCliente.toString());
            ServerProxy.registraCliente(nuovoCliente, numeroCarta);

            System.out.println("Registrazione completata con successo, benvenut*!");
            return true;

        }
        catch (Exception e)
        {
            System.err.println("Errore in fase di registrazione: " + e.getMessage());
            return false;
        }
    }



    private boolean validaInputRegistrazione(String nome, String cognome, String email,
                                             String password, String numeroCarta)
    {
        if (nome == null || nome.trim().isEmpty()) {
            System.err.println("Il nome non può essere vuoto");
            return false;
        }

        if (cognome == null || cognome.trim().isEmpty()) {
            System.err.println("Il cognome non può essere vuoto");
            return false;
        }

        if (email == null || email.trim().isEmpty()) {
            System.err.println("L'email non può essere vuota");
            return false;
        }

        if (!email.matches(regex_email)) {
            System.err.println("Formato email non valido");
            return false;
        }

        if (password == null || !password.matches(regex_password)) {
            System.err.println("La password contiene degli errori di formato");
            return false;
        }

        if (numeroCarta == null || numeroCarta.trim().isEmpty()) {
            System.err.println("Il numero di carta è obbligatorio per la registrazione");
            return false;
        }

        String cartaPulita = numeroCarta.replaceAll("[- ]", "");
        if (!cartaPulita.matches("^[0-9]{16}$")) {
            System.err.println("Formato carta non valido. Deve essere di 16 cifre");
            return false;
        }

        return true;
    }


    public boolean login(String email, String password)
    {
        try
        {
            if (email == null || email.trim().isEmpty()) {
                System.err.println("Email non può essere vuota");
                return false;
            }

            if (password == null || password.trim().isEmpty()) {
                System.err.println("Password non può essere vuota");
                return false;
            }

            //controllo se è già loggat*
            if (SessioneCliente.getInstance().isLoggato())
            {
                System.out.println("Sei già loggat* come: " +
                        SessioneCliente.getInstance().getNomeClienteCorrente());
                return true;
            }

            //chiamo il server per l'autenticazione del cliente
            ClienteDTO cliente = ServerProxy.login(email.trim(), password);

            //avvio la sessione
            SessioneCliente.getInstance().login(cliente);

            System.out.println("Login effettuato con successo, bentornat* " + cliente.getNome() +"!");
            return true;

        }
        catch (Exception e)
        {
            System.err.println("Login fallito: " + e.getMessage());
            return false;
        }
    }


    public void logout()
    {
        if (!SessioneCliente.getInstance().isLoggato())
        {
            System.out.println("ℹNon sei attualmente loggato");
            return;
        }

        SessioneCliente.getInstance().logout();
        System.out.println("Logout effettuato con successo. Ci vediamo!");
    }


    public boolean aderisciAFedelta(boolean attivaNotifichePromozioni)
    {
        try
        {
            if (!verificaAccesso())
            {
                return false;
            }

            ClienteDTO cliente = SessioneCliente.getInstance().getClienteCorrente();

            //Controllo se era già iscritto a fedeltà (assumiamo sia un lui va)
            if (cliente.isFedelta())
            {
                System.out.println("Sei già iscritto al programma Fedeltà!");
                return true;
            }

            ServerProxy.aderisciAFedelta(cliente.getId(), attivaNotifichePromozioni);

            //aggiorniamo la sessione locale
            cliente.setFedelta(true);
            cliente.setRiceviPromozioni(attivaNotifichePromozioni);

            System.out.println("Complimenti! Ora fai parte del programma FedeltàTreno!");
            System.out.println("Potrai usufruire di sconti esclusivi sui tuoi viaggi!");

            return true;

        }
        catch (Exception e)
        {
            System.err.println("Errore nell'adesione a fedeltà: " + e.getMessage());
            return false;
        }
    }


    public boolean rimuoviFedelta()
    {
        try
        {
            if (!verificaAccesso())
            {
                return false;
            }

            ClienteDTO cliente = SessioneCliente.getInstance().getClienteCorrente();

            if (!cliente.isFedelta())
            {
                System.out.println("Non sei iscritto al programma Fedeltà");
                return true;
            }

            ServerProxy.rimuoviFedelta(cliente.getId());

            cliente.setFedelta(false);
            cliente.setRiceviPromozioni(false);

            System.out.println("Sei stato rimosso dal programma FedeltàTreno, speriamo tu ora sia soddisfatto \n" +
                    "della scelta compiuta e che non te ne possa pentire in futuro ;)");

            return true;

        } catch (Exception e)
        {
            System.err.println("Errore nella rimozione da fedeltà: " + e.getMessage());
            return false;
        }
    }

    private boolean verificaAccesso()
    {
        if (!SessioneCliente.getInstance().isLoggato())
        {
            System.err.println("Devi effettuare il login per questa operazione!");
            return false;
        }
        return true;
    }
}

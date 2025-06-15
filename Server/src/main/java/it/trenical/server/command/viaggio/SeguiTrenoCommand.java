package it.trenical.server.command.viaggio;

import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.gestore.GestoreClienti;
import it.trenical.server.domain.gestore.GestoreViaggi;
import it.trenical.server.domain.Viaggio;
import it.trenical.server.observer.ViaggioETreno.NotificatoreClienteTreno;
import it.trenical.server.observer.ViaggioETreno.ObserverViaggio;

public class SeguiTrenoCommand implements ComandoViaggio
{
    private final String idTreno;
    private final String idCliente;

    public SeguiTrenoCommand(String idTreno, String idCliente) {
        this.idTreno = idTreno;
        this.idCliente = idCliente;
    }

    @Override
    public void esegui() throws Exception
    {
        GestoreViaggi gv = GestoreViaggi.getInstance();
        Viaggio v = gv.getViaggioPerTreno(idTreno);

        Cliente cliente = GestoreClienti.getInstance().getClienteById(idCliente);
        ObserverViaggio obs = new NotificatoreClienteTreno(cliente);
        v.attach(obs);
    }
}

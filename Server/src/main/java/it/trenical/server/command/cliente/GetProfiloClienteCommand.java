package it.trenical.server.command.cliente;
import it.trenical.server.command.cliente.ComandoCliente;
import it.trenical.server.domain.cliente.Cliente;
import it.trenical.server.domain.gestore.GestoreClienti;
import it.trenical.server.dto.ClienteDTO;
import it.trenical.server.utils.ClienteAssembler;

public class GetProfiloClienteCommand implements ComandoCliente
{
        private final String idCliente;
        private ClienteDTO profilo;

        public GetProfiloClienteCommand(String idCliente)
        {
            this.idCliente = idCliente;
        }

        @Override
        public void esegui() throws Exception {
            GestoreClienti gc = GestoreClienti.getInstance();
            Cliente cliente = gc.getClienteById(idCliente);

            if (cliente == null) {
                throw new IllegalArgumentException("Cliente non trovato");
            }

            this.profilo = ClienteAssembler.toDTO(cliente);
        }

        public ClienteDTO getProfilo()
        {
            return profilo;
        }
}

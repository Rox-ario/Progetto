package it.trenical.server.domain.gestore;

import it.trenical.server.domain.cliente.ClienteBanca;

import java.util.HashMap;
import java.util.Map;

public final class GestoreBanca {
    private static GestoreBanca instance = null;

    private final Map<String, ClienteBanca> clienti;

    private GestoreBanca() {
        clienti = new HashMap<>();
    }

    public static synchronized GestoreBanca getInstance() {
        if (instance == null) {
            instance = new GestoreBanca();
        }
        return instance;
    }

    public void registraClienteBanca(ClienteBanca cb) {
        clienti.put(cb.getIdCliente(), cb);
    }

    public ClienteBanca getClienteBanca(String id) {
        return clienti.get(id);
    }

    public boolean eseguiPagamento(String id, double importo) {
        ClienteBanca cb = clienti.get(id);
        if (cb != null && cb.getSaldo() >= importo) {
            cb.addebita(importo);
            return true;
        }
        return false;
    }

    public void rimborsa(String id, double importo) {
        ClienteBanca cb = clienti.get(id);
        if (cb != null) {
            cb.accredita(importo);
        }
    }
}


package it.trenical.server.dto;

import it.trenical.server.domain.Cliente;

public class ClienteDTO
{
    private String id;
    private String nome;
    private String cognome;
    private String email;
    private String password;
    private boolean isFedelta;

    public ClienteDTO() {}

    public ClienteDTO(String id, String nome, String cognome, String email, boolean isFedelta) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.isFedelta = isFedelta;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isFedelta() {
        return isFedelta;
    }

    public void setFedelta(boolean fedelta) {
        isFedelta = fedelta;
    }
}

package it.trenical.server.dto;

public class ModificaClienteDTO
{
    private String id;
    private String nome;
    private String cognome;
    private String password;
    private boolean isFedelta;
    private boolean wantsNotifiche;

    public ModificaClienteDTO() {}

    public ModificaClienteDTO(String id, String nome, String cognome, String password, boolean isFedelta, boolean wantsNotifiche)
    {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.password = password;
        this.isFedelta = isFedelta;
        this.wantsNotifiche = wantsNotifiche;
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

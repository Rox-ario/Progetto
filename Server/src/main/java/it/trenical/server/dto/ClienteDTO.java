package it.trenical.server.dto;

public class ClienteDTO
{
    private String id;
    private String nome;
    private String cognome;
    private String email;
    private String password;
    private boolean isFedelta;
    private boolean riceviNotifiche;    // NUOVO
    private boolean riceviPromozioni;   // NUOVO

    public ClienteDTO() {}

    public ClienteDTO(String id, String nome, String cognome, String email,
                      boolean isFedelta, boolean riceviNotifiche, boolean riceviPromozioni) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.isFedelta = isFedelta;
        this.riceviNotifiche = riceviNotifiche;
        this.riceviPromozioni = riceviPromozioni;
    }

    public boolean isRiceviNotifiche() { return riceviNotifiche; }
    public void setRiceviNotifiche(boolean riceviNotifiche) { this.riceviNotifiche = riceviNotifiche; }

    public boolean isRiceviPromozioni() { return riceviPromozioni; }
    public void setRiceviPromozioni(boolean riceviPromozioni) { this.riceviPromozioni = riceviPromozioni; }


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

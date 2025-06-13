package it.trenical.server.domain.cliente;

import java.util.Objects;

public class Cliente
{
    private final String id;
    private final String nome;
    private final String cognome;
    private final String email;
    private final String password;
    private final boolean isFedelta;

    private static class Builder
    {
        private String id = null;
        private String nome = null;
        private String cognome = null;
        private String email = null;
        private String password = null;
        private boolean isFedelta = false;

        public Builder(){}

        public Builder ID(String val) {id = val; return this;}
        public Builder Nome(String val) {nome = val; return this;}
        public Builder Cognome(String val) {cognome = val; return this;}
        public Builder Email(String val) {email = val; return this;}
        public Builder Password(String val) {password = val; return this;}
        public Builder isFedelta(boolean val) {isFedelta = val; return this;}

        public Cliente build() {return new Cliente(this);}
    }//builder
    private Cliente(Builder b)
    {
        id = b.id; nome = b.nome; cognome = b.cognome;
        email = b.email; password = b.password;
        isFedelta = b.isFedelta;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public boolean haAdesioneFedelta() { return isFedelta; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Cliente cliente)) return false;
        return Objects.equals(id, cliente.id) &&
                Objects.equals(nome, cliente.nome) &&
                Objects.equals(cognome, cliente.cognome) &&
                Objects.equals(email, cliente.email) &&
                Objects.equals(password, cliente.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nome, cognome, email, password);
    }
}

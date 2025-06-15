package it.trenical.server.dto;

import it.trenical.server.domain.enumerations.ClasseServizio;

public class ModificaBigliettoDTO
{
    private final String IDBiglietto;
    private final ClasseServizio classeServizio;

    public ModificaBigliettoDTO(String IDBiglietto, ClasseServizio classeServizio) {
        this.IDBiglietto = IDBiglietto;
        this.classeServizio = classeServizio;
    }

    public String getIDBiglietto() {
        return IDBiglietto;
    }

    public ClasseServizio getClasseServizio() {
        return classeServizio;
    }
}

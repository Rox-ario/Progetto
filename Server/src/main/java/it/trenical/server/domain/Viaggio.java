package it.trenical.server.domain;

import it.trenical.server.domain.enumerations.ClasseServizio;
import it.trenical.server.domain.enumerations.StatoViaggio;
import it.trenical.server.domain.enumerations.TipoBinario;
import it.trenical.server.dto.NotificaDTO;
import it.trenical.server.observer.ViaggioETreno.ObserverViaggio;
import it.trenical.server.observer.ViaggioETreno.SoggettoViaggio;

import java.util.*;

public class Viaggio extends SoggettoViaggio
{
    private final String id;
    private final Calendar inizio;
    private final Calendar fine;
    private final Treno treno;
    private final Tratta tratta;
    private StatoViaggio stato;
    private Map<ClasseServizio, Integer> postiDisponibili;
    private final Map<TipoBinario, Integer> binari; //string partenza/arrivo, int binario
    private int ritardoMinuti = 0;
    private List<ObserverViaggio> osservatori;
    private double kilometri = 0;
    private final double velocitaMedia;

    public Viaggio(String id, Calendar inizio, Calendar fine, Treno treno, Tratta tratta)
    {
        this.id = id;
        this.inizio = inizio;
        this.fine = fine;
        this.treno = treno;
        this.tratta = tratta;
        this.binari = new HashMap<>();
       this.stato =  StatoViaggio.PROGRAMMATO;
        this.postiDisponibili = treno.getPosti();
        this.osservatori = new ArrayList<ObserverViaggio>();
        binari.put(TipoBinario.ARRIVO, 0);
        binari.put(TipoBinario.PARTENZA, 0);
        this.kilometri = calcolaKilometri(); //calcola la distanza in chilometri
        velocitaMedia = treno.getTipo().getVelocitaMedia();
    }

    private double getOrePassate() {
        Calendar now = Calendar.getInstance();
        long diffMillis = now.getTimeInMillis() - getInizioReale().getTimeInMillis(); // differenza in millisecondi
        return diffMillis / (1000.0 * 60 * 60); // conversione in ore
    }

    public double getPercentualeViaggio()
    {
        //Calcolo la distanza percorsa
        double orePassate = getOrePassate();
        // Calcolo la distanza percorsa nel tempo trascorso
        double distanzaPercorsa = velocitaMedia * orePassate;
        // Restituisce la percentuale: (distanza percorsa / distanza totale) * 100
        return (distanzaPercorsa / kilometri) * 100;
    }

    private double calcolaKilometri()
    {
        //Uso Heaverside per calcolare la distanza tra due punti in linea retta,
        //purtroppo non posso tenere in considerazione le varie strade che si possono intrapprendere per arrivare da punto A a punto B
        Stazione partenza = tratta.getStazionePartenza();
        Stazione arrivo = tratta.getStazioneArrivo();

        double longP = partenza.getLongitudine();
        double longA = arrivo.getLongitudine();
        double latP = partenza.getLatitudine();
        double latA = arrivo.getLatitudine();

        //Trasformo la longitudine e la latitudine in radianti
        double radLongP = Math.toRadians(longP);
        double radLongA = Math.toRadians(longA);
        double radLatP = Math.toRadians(latP);
        double radLatA = Math.toRadians(latA);

        //Definisco R il raggio della terra
        int R = 6371;

        /*
          calcolo la distanza d come
          d = 2 * R * asin(radice(sin^2((radLatA - radLatP)/2) + cos(radLatA) * cos(radLatP) * sin^2((radLongA - radLongP)/2)))
        * */
        double d = 2 * R * Math.asin(Math.sqrt(Math.pow(Math.sin((radLatA-radLatP)/2), 2) + Math.cos(radLatP)*Math.cos(radLatA)*Math.pow(Math.sin((radLongA-radLongP)/2), 2)));
        return d;
    }

    public String getId() {
        return id;
    }

    public Treno getTreno() {
        return treno;
    }

    public Tratta getTratta() {
        return tratta;
    }

    public StatoViaggio getStato() {
        return stato;
    }

    public void setStato(StatoViaggio stato)
    {
        this.stato = stato;
        notificaCambiamentoViaggio();
        if(stato.equals(StatoViaggio.TERMINATO))
        {
            osservatori.clear();
        }
    }

    public int getPostiDisponibiliPerClasse(ClasseServizio classeServizio)
    {
        if(!postiDisponibili.containsKey(classeServizio))
            throw new IllegalArgumentException("Errore: classe di servizio "+ classeServizio+" non registrata");
        return postiDisponibili.get(classeServizio);
    }

    public void riduciPostiDisponibiliPerClasse(ClasseServizio classeServizio, int n)
    {
        postiDisponibili.compute(classeServizio, (k, posti) -> posti - n);
        System.out.println("Posti di "+ classeServizio+" decrementati di "+ n);
    }

    public void setBinario(TipoBinario tipo, int numero)
    {
        binari.put(tipo, numero);
    }

    public int getBinario(TipoBinario tipo)
    {
        return binari.getOrDefault(tipo, -1);
    }

    public void aggiornaRitardo(int minuti)
    {
        if(minuti < 0)
        {
            throw new IllegalArgumentException("Il valore di ritardo non può essere negativo");
        }
        ritardoMinuti += minuti;
        this.stato = StatoViaggio.IN_RITARDO;
        notificaCambiamentoViaggio();
    }

    public Calendar getInizioReale()
    {
        Calendar clone = (Calendar) inizio.clone(); //faccio la clone dell'inizio così non lo modifico, tanto devo solo mostrare il ritardo eventuale
        clone.add(Calendar.MINUTE, ritardoMinuti);
        return clone;
    }

    public Calendar getFineReale()
    {
        Calendar clone = (Calendar) fine.clone(); //idem per inizio
        clone.add(Calendar.MINUTE, ritardoMinuti);
        return clone;
    }

    @Override
    public void attach(ObserverViaggio ob)
    {
        osservatori.add(ob);
    }

    @Override
    public void detach(ObserverViaggio ob)
    {
        osservatori.remove(ob);
    }

    @Override
    public void notificaCambiamentoViaggio()
    {
        for(ObserverViaggio obs : osservatori)
        {
            obs.aggiorna(this);
        }
    }

    public NotificaDTO getNotificaViaggio()
    {
        String messaggio = "Notifica Viaggio: [\nid = "+getId()+"\n"+ getTratta().getStazionePartenza().getCitta()+" -> " +
                getTratta().getStazioneArrivo().getCitta() + "\nSTATO: "+ getStato()+"\n";
        if(Calendar.getInstance().before(getInizio()))
        {
            messaggio += "PARTENZA: "+getInizioReale().get(Calendar.DAY_OF_MONTH) +"/"+(getInizioReale().get(Calendar.MONTH)+1)+"/"+getInizioReale().get(Calendar.YEAR) + " "+getInizioReale().get(Calendar.HOUR_OF_DAY)+":"+getInizioReale().get(Calendar.MINUTE)+getInizioReale().get(Calendar.SECOND);

        }
        messaggio += "ARRIVO: "+getInizioReale().get(Calendar.DAY_OF_MONTH) +"/"+(getInizioReale().get(Calendar.MONTH)+1)+"/"+getInizioReale().get(Calendar.YEAR) +" "+getFineReale().get(Calendar.HOUR_OF_DAY)+":"+getFineReale().get(Calendar.MINUTE)+getFineReale().get(Calendar.SECOND)
            +"\nTRENO: "+getTreno().getID()+", TIPO: "+ getTreno().getTipo()+"\n]";
        return new NotificaDTO(messaggio);
    }

    public double getKilometri() {
        return kilometri;
    }

    public void incrementaPostiDisponibiliPerClasse(ClasseServizio classeServizio, int i)
    {
        postiDisponibili.compute(classeServizio, (k, posti) -> posti + i);
        System.out.println("Posti di "+ classeServizio+" incrementati di "+ i);
    }

    public NotificaDTO getNotificaTreno()
    {
        String messaggio = "Il treno "+treno.getID()+" ha percorso il "+getPercentualeViaggio()+"% della tratta";
        NotificaDTO dto = new NotificaDTO(messaggio);
        return dto;
    }

    @Override
    public String toString()
    {
        return "[" +
                "id= " + id +
                ", inizio= " + getInizioReale().get(Calendar.DAY_OF_MONTH) +"/"+(getInizioReale().get(Calendar.MONTH)+1)+"/"+getInizioReale().get(Calendar.YEAR) + " "+getInizioReale().get(Calendar.HOUR_OF_DAY)+":"+getInizioReale().get(Calendar.MINUTE)+getInizioReale().get(Calendar.SECOND)+
                ", fine= " + getInizioReale().get(Calendar.DAY_OF_MONTH) +"/"+(getInizioReale().get(Calendar.MONTH)+1)+"/"+getInizioReale().get(Calendar.YEAR) +" "+getFineReale().get(Calendar.HOUR_OF_DAY)+":"+getFineReale().get(Calendar.MINUTE)+getFineReale().get(Calendar.SECOND)+
                ", stato= " + stato +
                ", binario di Partenza= " + getBinario(TipoBinario.PARTENZA) +
                ", binario di Arrivo= " + getBinario(TipoBinario.ARRIVO)+
                ", ritardoMinuti= " + ritardoMinuti +
                ", \nTratta: [ " + tratta.getStazionePartenza().getCitta()+", stazione: "+ tratta.getStazionePartenza().getNome()
                +" ] -> "+ "[ "+ tratta.getStazioneArrivo().getCitta()+", stazione: "+  tratta.getStazioneArrivo().getNome()+" ]"+
                ", \nTreno: [ " + treno.getID() +", "+ treno.getTipo()+
                ']';
    }

    public Calendar getInizio() {
        return inizio;
    }

    public Calendar getFine() {
        return fine;
    }

    public void setBinarioDiPartenza(int i)
    {
        binari.put(TipoBinario.PARTENZA, i);
    }

    public void setBinarioDiArrivo(int i)
    {
        binari.put(TipoBinario.ARRIVO, i);
    }

    public void setRitardoMinuti(int ritardo)
    {
        this.ritardoMinuti = ritardo;
    }
}

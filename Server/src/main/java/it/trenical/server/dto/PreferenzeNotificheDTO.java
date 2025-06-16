package it.trenical.server.dto;


public class PreferenzeNotificheDTO
{
    private boolean riceviNotifiche;
    private boolean riceviPromozioni;

    public PreferenzeNotificheDTO() {}

    public PreferenzeNotificheDTO(boolean riceviNotifiche, boolean riceviPromozioni)
    {
        this.riceviNotifiche = riceviNotifiche;
        this.riceviPromozioni = riceviPromozioni;
    }

    public boolean isRiceviNotifiche()
    {
        return riceviNotifiche;
    }
    public void setRiceviNotifiche(boolean riceviNotifiche)
    {
        this.riceviNotifiche = riceviNotifiche;
    }

    public boolean isRiceviPromozioni()
    {
        return riceviPromozioni;
    }
    public void setRiceviPromozioni(boolean riceviPromozioni)
    {
        this.riceviPromozioni = riceviPromozioni;
    }
}

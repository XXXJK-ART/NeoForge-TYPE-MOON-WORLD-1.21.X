package net.xxxjk.TYPE_MOON_WORLD.servant.artoria;

public enum ArtoriaExcaliburAction {
   FULL_NP(1, "artoria_excalibur_charge_full", ArtoriaRules.EXCALIBUR_WINDUP_TICKS + ArtoriaRules.EXCALIBUR_TICKS),
   SHORT_RELEASE(2, "artoria_excalibur_charge_short", ArtoriaRules.EXCALIBUR_SHORT_CHARGE_TICKS + ArtoriaRules.EXCALIBUR_SHORT_TICKS),
   CANCEL(3, "", 0);

   private final int id;
   private final String animation;
   private final int duration;

   ArtoriaExcaliburAction(int id, String animation, int duration) {
      this.id = id;
      this.animation = animation;
      this.duration = duration;
   }

   public int id() { return id; }
   public String animation() { return animation; }
   public int duration() { return duration; }

   public static ArtoriaExcaliburAction byId(int id) {
      for (ArtoriaExcaliburAction action : values()) if (action.id == id) return action;
      return null;
   }
}

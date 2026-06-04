package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

public enum ServantCombatPhase {
   PROBING(0),
   NORMAL(1),
   DECISIVE(2);

   private final int id;

   ServantCombatPhase(int id) {
      this.id = id;
   }

   public int id() {
      return this.id;
   }

   public static ServantCombatPhase fromId(int id) {
      for (ServantCombatPhase phase : values()) {
         if (phase.id == id) {
            return phase;
         }
      }
      return PROBING;
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.model;

public enum ServantFaction {
   HEAVEN("heaven"),
   EARTH("earth"),
   HUMAN("human"),
   STAR("star"),
   BEAST("beast");

   private final String key;

   ServantFaction(String key) {
      this.key = key;
   }

   public String key() {
      return this.key;
   }

   public static ServantFaction fromKey(String key) {
      if (key == null) {
         return HUMAN;
      }

      for (ServantFaction faction : values()) {
         if (faction.key.equals(key.toLowerCase())) {
            return faction;
         }
      }

      return HUMAN;
   }
}

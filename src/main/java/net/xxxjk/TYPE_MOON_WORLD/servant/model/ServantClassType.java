package net.xxxjk.TYPE_MOON_WORLD.servant.model;

public enum ServantClassType {
   SABER(0, "saber"),
   ARCHER(1, "archer"),
   LANCER(2, "lancer"),
   RIDER(3, "rider"),
   CASTER(4, "caster"),
   ASSASSIN(5, "assassin"),
   BERSERKER(6, "berserker"),
   RULER(7, "ruler"),
   AVENGER(8, "avenger"),
   FOREIGNER(9, "foreigner"),
   PRETENDER(10, "pretender");

   private final int id;
   private final String key;

   ServantClassType(int id, String key) {
      this.id = id;
      this.key = key;
   }

   public int id() {
      return this.id;
   }

   public String key() {
      return this.key;
   }

   public static ServantClassType fromKey(String key) {
      if (key == null) {
         return SABER;
      }

      for (ServantClassType type : values()) {
         if (type.key.equals(key.toLowerCase())) {
            return type;
         }
      }

      return SABER;
   }

   public static ServantClassType fromId(int id) {
      for (ServantClassType type : values()) {
         if (type.id == id) {
            return type;
         }
      }

      return SABER;
   }
}

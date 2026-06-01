package net.xxxjk.TYPE_MOON_WORLD.servant.personality;

public enum SpecialTargetPrinciple {
   SPARE_PIGS("spare_pigs"),
   SPARE_CANINES("spare_canines");

   private final String key;

   SpecialTargetPrinciple(String key) {
      this.key = key;
   }

   public String key() {
      return this.key;
   }

   public static SpecialTargetPrinciple fromKey(String key) {
      if (key == null) {
         return null;
      }
      for (SpecialTargetPrinciple principle : values()) {
         if (principle.key.equalsIgnoreCase(key)) {
            return principle;
         }
      }
      return null;
   }
}

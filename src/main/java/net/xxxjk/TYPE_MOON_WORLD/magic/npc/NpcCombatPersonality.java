package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.minecraft.util.RandomSource;

public enum NpcCombatPersonality {
   GOOD(0, "good"),
   NEUTRAL(1, "neutral"),
   EVIL(2, "evil");

   private final int id;
   private final String key;

   private NpcCombatPersonality(int id, String key) {
      this.id = id;
      this.key = key;
   }

   public int id() {
      return this.id;
   }

   public String key() {
      return this.key;
   }

   public static NpcCombatPersonality fromId(int id) {
      for (NpcCombatPersonality value : values()) {
         if (value.id == id) {
            return value;
         }
      }

      return NEUTRAL;
   }

   public static NpcCombatPersonality random(RandomSource random) {
      if (random == null) {
         return NEUTRAL;
      } else {
         return switch (random.nextInt(3)) {
            case 0 -> GOOD;
            case 1 -> NEUTRAL;
            default -> EVIL;
         };
      }
   }
}

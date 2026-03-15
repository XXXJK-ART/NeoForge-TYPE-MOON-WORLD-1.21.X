package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.minecraft.util.RandomSource;

public enum NpcCombatTemperament {
   TIMID(0, "timid"),
   STEADY(1, "steady"),
   BOLD(2, "bold");

   private final int id;
   private final String key;

   private NpcCombatTemperament(int id, String key) {
      this.id = id;
      this.key = key;
   }

   public int id() {
      return this.id;
   }

   public String key() {
      return this.key;
   }

   public static NpcCombatTemperament fromId(int id) {
      for (NpcCombatTemperament value : values()) {
         if (value.id == id) {
            return value;
         }
      }

      return STEADY;
   }

   public static NpcCombatTemperament random(RandomSource random) {
      if (random == null) {
         return STEADY;
      } else {
         return switch (random.nextInt(3)) {
            case 0 -> TIMID;
            case 1 -> STEADY;
            default -> BOLD;
         };
      }
   }
}

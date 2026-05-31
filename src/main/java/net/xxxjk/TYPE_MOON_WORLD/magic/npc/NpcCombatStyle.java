package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public enum NpcCombatStyle {
   CLOSE_PRESSURE(0, "close_pressure"),
   RANGED_BURST(1, "ranged_burst"),
   CONTROL_DRAIN(2, "control_drain"),
   BALANCED(3, "balanced");

   private final int id;
   private final String key;

   private NpcCombatStyle(int id, String key) {
      this.id = id;
      this.key = key;
   }

   public int id() {
      return this.id;
   }

   public String key() {
      return this.key;
   }

   public static NpcCombatStyle fromId(int id) {
      for (NpcCombatStyle value : values()) {
         if (value.id == id) {
            return value;
         }
      }

      return BALANCED;
   }

   public static NpcCombatStyle fromAttributes(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) {
         return BALANCED;
      } else if (vars.player_magic_attributes_fire || vars.player_magic_attributes_wind) {
         return RANGED_BURST;
      } else if (vars.player_magic_attributes_earth || vars.player_magic_attributes_water) {
         return CONTROL_DRAIN;
      } else {
         return !vars.player_magic_attributes_ether && !vars.player_magic_attributes_none && !vars.player_magic_attributes_imaginary_number
            ? BALANCED
            : CLOSE_PRESSURE;
      }
   }
}

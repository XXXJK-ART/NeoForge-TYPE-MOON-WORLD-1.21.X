package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class NpcMagicExecutionService {
   private NpcMagicExecutionService() {
   }

   public static boolean hasCastableMagic(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      if (vars != null && magicId != null && !magicId.isEmpty()) {
         for (int slot = 0; slot < 12; slot++) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = vars.getWheelSlotEntry(vars.active_wheel_index, slot);
            if (entry != null && !entry.isEmpty() && magicId.equals(entry.magicId) && vars.isWheelSlotEntryCastable(entry)) {
               return true;
            }
         }

         return "reinforcement".equals(magicId) && vars.learned_magics.contains("reinforcement");
      } else {
         return false;
      }
   }

   public static boolean castMagic(
      MysticMagicianEntity caster,
      LivingEntity target,
      TypeMoonWorldModVariables.PlayerVariables vars,
      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slot,
      double effectiveProficiency,
      long gameTime
   ) {
      String magicId = slot == null ? "" : slot.magicId;
      CompoundTag payload = slot == null || slot.presetPayload == null ? new CompoundTag() : slot.presetPayload;
      return switch (magicId) {
         case "gander" -> NpcMagicCastBridge.castGander(caster, target, vars, payload, effectiveProficiency);
         case "gandr_machine_gun" -> NpcMagicCastBridge.castGandrMachineGun(caster, target, vars, payload, effectiveProficiency);
         case "gravity_magic" -> NpcMagicCastBridge.castGravity(caster, target, vars, payload, effectiveProficiency);
         case "reinforcement" -> NpcMagicCastBridge.castReinforcement(caster, vars, payload, effectiveProficiency);
         case "jewel_random_shoot" -> NpcMagicCastBridge.castJewelRandomShoot(caster, target, vars, effectiveProficiency);
         case "jewel_machine_gun" -> NpcMagicCastBridge.castJewelMachineGun(caster, target, vars, payload, effectiveProficiency);
         case "ruby_flame_sword" -> NpcMagicCastBridge.castRubyFlameSword(caster, target, vars, effectiveProficiency);
         case "sapphire_winter_frost" -> NpcMagicCastBridge.castSapphireWinterFrost(caster, target, vars, effectiveProficiency);
         case "emerald_winter_river" -> NpcMagicCastBridge.castEmeraldWinterRiver(caster, target, vars, effectiveProficiency);
         case "topaz_reinforcement" -> NpcMagicCastBridge.castTopazReinforcement(caster, vars, effectiveProficiency);
         case "cyan_wind" -> NpcMagicCastBridge.castCyanWind(caster, target, vars, effectiveProficiency);
         default -> false;
      };
   }

   public static int getGlobalCooldownAfterCast(String magicId, CompoundTag payload) {
      return switch (magicId) {
         case "gander" -> 16;
         case "gandr_machine_gun" -> NpcMagicCastBridge.isBarrageMode(payload) ? 30 : 14;
         case "gravity_magic" -> 24;
         case "reinforcement" -> 30;
         case "jewel_random_shoot" -> 10;
         case "jewel_machine_gun" -> 14;
         case "ruby_flame_sword", "cyan_wind" -> 18;
         case "topaz_reinforcement" -> 20;
         case "sapphire_winter_frost", "emerald_winter_river" -> 24;
         default -> 12;
      };
   }

   public static int getPerMagicCooldown(String magicId, CompoundTag payload) {
      return switch (magicId) {
         case "gander" -> 18;
         case "gandr_machine_gun" -> NpcMagicCastBridge.isBarrageMode(payload) ? 54 : 30;
         case "gravity_magic" -> 60;
         case "reinforcement" -> 120;
         case "jewel_random_shoot" -> 10;
         case "jewel_machine_gun" -> 100;
         case "ruby_flame_sword" -> 180;
         case "sapphire_winter_frost" -> 320;
         case "emerald_winter_river" -> 340;
         case "topaz_reinforcement" -> 260;
         case "cyan_wind" -> 220;
         default -> 20;
      };
   }
}

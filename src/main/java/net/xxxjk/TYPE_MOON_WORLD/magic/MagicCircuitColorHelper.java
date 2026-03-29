package net.xxxjk.TYPE_MOON_WORLD.magic;

import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class MagicCircuitColorHelper {
   private static final int LEGACY_COLOR_SWORD = 0xD63A3A;
   private static final int LEGACY_COLOR_NONE = 0xD9D9D9;
   public static final int COLOR_SWORD = 0x39D8C8;
   public static final int COLOR_FIRE = 0xFF6A3D;
   public static final int COLOR_WATER = 0x4AA6FF;
   public static final int COLOR_WIND = 0x6FE9C5;
   public static final int COLOR_EARTH = 0x9AD14B;
   public static final int COLOR_ETHER = 0xB68CFF;
   public static final int COLOR_IMAGINARY_NUMBER = 0x6A4CFF;
   public static final int COLOR_NONE = 0x39D8C8;
   public static final int DEFAULT_COLOR = COLOR_NONE;

   private MagicCircuitColorHelper() {
   }

   public static int resolveColor(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) {
         return DEFAULT_COLOR;
      } else if (vars.player_magic_attributes_sword) {
         return COLOR_SWORD;
      } else if (vars.player_magic_attributes_fire) {
         return COLOR_FIRE;
      } else if (vars.player_magic_attributes_water) {
         return COLOR_WATER;
      } else if (vars.player_magic_attributes_wind) {
         return COLOR_WIND;
      } else if (vars.player_magic_attributes_earth) {
         return COLOR_EARTH;
      } else if (vars.player_magic_attributes_ether) {
         return COLOR_ETHER;
      } else if (vars.player_magic_attributes_imaginary_number) {
         return COLOR_IMAGINARY_NUMBER;
      } else {
         return vars.player_magic_attributes_none ? COLOR_NONE : DEFAULT_COLOR;
      }
   }

   public static int ensureColor(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) {
         return DEFAULT_COLOR;
      } else {
         if (vars.magic_circuit_color_rgb <= 0 || shouldMigrateLegacyColor(vars)) {
            vars.magic_circuit_color_rgb = resolveColor(vars);
         }

         return vars.magic_circuit_color_rgb;
      }
   }

   public static int ensureColor(Entity entity) {
      if (entity == null) {
         return DEFAULT_COLOR;
      } else {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return ensureColor(vars);
      }
   }

   public static float red(int packed) {
      return (packed >> 16 & 255) / 255.0F;
   }

   public static float green(int packed) {
      return (packed >> 8 & 255) / 255.0F;
   }

   public static float blue(int packed) {
      return (packed & 255) / 255.0F;
   }

   private static boolean shouldMigrateLegacyColor(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars.magic_circuit_color_rgb == LEGACY_COLOR_SWORD || vars.magic_circuit_color_rgb == LEGACY_COLOR_NONE;
   }
}

package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import java.util.Set;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.api.GemApiRegistry;

public final class GemCompatibilityService {
   private static final Set<String> WHITELIST = Set.of(
      "gravity_magic", "reinforcement", "projection", "gander",
      "healing_magic", "suggestion_magic", "binding_magic",
      "fire_magic", "water_magic", "wind_magic", "earth_magic",
      "flame_array", "azure_water_array", "gale_wind_array", "rock_earth_array",
      "aerial_stasis", "aerial_ascent", "flight_magic"
   );

   private GemCompatibilityService() {
   }

   public static boolean isWhitelistedMagic(String magicId) {
      return WHITELIST.contains(magicId) || GemApiRegistry.hasCustom(net.minecraft.resources.ResourceLocation.tryParse(magicId));
   }

   public static Set<String> getWhitelistedMagics() {
      return WHITELIST;
   }

   public static int calculateEngraveSuccessChance(GemQuality quality, GemType type, String magicId, double magicProficiency) {
      var id = net.minecraft.resources.ResourceLocation.tryParse(magicId);
      if (GemApiRegistry.hasCustom(id)) {
         return GemApiRegistry.calculate(id,
            net.xxxjk.typemoonworld.api.GemType.valueOf(type.name()),
            net.xxxjk.typemoonworld.api.GemQuality.valueOf(quality.name()), magicProficiency);
      }
      int base = switch (quality) {
         case POOR -> 60;
         case NORMAL -> 80;
         case HIGH -> 95;
      };
      int complexity = getComplexityModifier(magicId);
      int affinity = getAffinityModifier(type, magicId);
      int proficiency = (int)Math.round(Math.max(0.0, Math.min(100.0, magicProficiency)) * 0.15);
      int chance = base + complexity + affinity + proficiency;
      return chance < 5 ? 5 : Math.min(99, chance);
   }

   private static int getComplexityModifier(String magicId) {
      return switch (magicId) {
         case "projection" -> -20;
         case "reinforcement" -> -10;
         case "gravity_magic" -> 0;
         case "gander" -> -5;
         case "healing_magic" -> -5;
         case "suggestion_magic" -> -12;
         case "binding_magic" -> -10;
         case "fire_magic", "water_magic", "wind_magic", "earth_magic" -> -8;
         case "flame_array", "azure_water_array", "gale_wind_array", "rock_earth_array" -> -14;
         case "aerial_stasis", "aerial_ascent" -> -4;
         case "flight_magic" -> -24;
         default -> -30;
      };
   }

   private static int getAffinityModifier(GemType type, String magicId) {
      return switch (magicId) {
         case "gravity_magic" -> {
            switch (type) {
               case BLACK_SHARD:
                  yield 20;
               case CYAN:
                  yield 10;
               case SAPPHIRE:
                  yield 5;
               case WHITE_GEMSTONE:
                  yield 0;
               case TOPAZ:
               case EMERALD:
                  yield -5;
               case RUBY:
                  yield -10;
               default:
                  throw new MatchException(null, null);
            }
         }
         case "reinforcement" -> {
            switch (type) {
               case BLACK_SHARD:
                  yield 0;
               case CYAN:
               case SAPPHIRE:
                  yield -5;
               case WHITE_GEMSTONE:
                  yield 0;
               case TOPAZ:
                  yield 10;
               case EMERALD:
                  yield 5;
               case RUBY:
                  yield 3;
               default:
                  throw new MatchException(null, null);
            }
         }
         case "projection" -> {
            switch (type) {
               case BLACK_SHARD:
                  yield -10;
               case CYAN:
               case TOPAZ:
               case EMERALD:
                  yield -5;
               case SAPPHIRE:
               case RUBY:
                  yield 5;
               case WHITE_GEMSTONE:
                  yield 10;
               default:
                  throw new MatchException(null, null);
            }
         }
         case "gander" -> {
            switch (type) {
               case BLACK_SHARD:
                  yield 12;
               case CYAN:
                  yield 5;
               case SAPPHIRE:
               case WHITE_GEMSTONE:
                  yield 0;
               case TOPAZ:
               case EMERALD:
                  yield -5;
               case RUBY:
                  yield 8;
               default:
                  throw new MatchException(null, null);
            }
         }
         case "healing_magic" -> switch (type) {
            case EMERALD -> 12;
            case WHITE_GEMSTONE -> 8;
            case SAPPHIRE -> 4;
            case BLACK_SHARD -> -12;
            case RUBY -> -5;
            default -> 0;
         };
         case "suggestion_magic" -> switch (type) {
            case CYAN -> 10;
            case WHITE_GEMSTONE -> 6;
            case BLACK_SHARD -> 4;
            case RUBY -> -5;
            default -> 0;
         };
         case "binding_magic" -> switch (type) {
            case TOPAZ -> 10;
            case SAPPHIRE -> 6;
            case EMERALD -> 3;
            case CYAN -> -5;
            default -> 0;
         };
         case "fire_magic" -> switch (type) {
            case RUBY -> 15;
            case TOPAZ -> 5;
            case SAPPHIRE -> -12;
            default -> 0;
         };
         case "water_magic" -> switch (type) {
            case SAPPHIRE -> 15;
            case EMERALD -> 5;
            case RUBY -> -12;
            default -> 0;
         };
         case "wind_magic" -> switch (type) {
            case CYAN -> 15;
            case WHITE_GEMSTONE -> 5;
            case TOPAZ -> -5;
            default -> 0;
         };
         case "earth_magic" -> switch (type) {
            case TOPAZ -> 12;
            case EMERALD -> 8;
            case CYAN -> -5;
            default -> 0;
         };
         case "flame_array" -> switch (type) {
            case RUBY -> 15;
            case TOPAZ -> 5;
            case SAPPHIRE -> -12;
            default -> 0;
         };
         case "azure_water_array" -> switch (type) {
            case SAPPHIRE -> 15;
            case EMERALD -> 5;
            case RUBY -> -12;
            default -> 0;
         };
         case "gale_wind_array", "aerial_stasis", "aerial_ascent", "flight_magic" -> switch (type) {
            case CYAN -> 15;
            case WHITE_GEMSTONE -> 5;
            case TOPAZ -> -5;
            default -> 0;
         };
         case "rock_earth_array" -> switch (type) {
            case TOPAZ -> 12;
            case EMERALD -> 8;
            case CYAN -> -5;
            default -> 0;
         };
         default -> -10;
      };
   }
}

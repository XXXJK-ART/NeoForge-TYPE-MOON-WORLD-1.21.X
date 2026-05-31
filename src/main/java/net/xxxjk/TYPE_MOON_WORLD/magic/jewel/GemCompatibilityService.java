package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import java.util.Set;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;

public final class GemCompatibilityService {
   private static final Set<String> WHITELIST = Set.of("gravity_magic", "reinforcement", "projection", "gander");

   private GemCompatibilityService() {
   }

   public static boolean isWhitelistedMagic(String magicId) {
      return WHITELIST.contains(magicId);
   }

   public static Set<String> getWhitelistedMagics() {
      return WHITELIST;
   }

   public static int calculateEngraveSuccessChance(GemQuality quality, GemType type, String magicId, double magicProficiency) {
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
         default -> -10;
      };
   }
}

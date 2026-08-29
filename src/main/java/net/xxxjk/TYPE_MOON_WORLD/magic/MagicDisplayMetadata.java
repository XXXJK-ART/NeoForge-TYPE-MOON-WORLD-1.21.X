package net.xxxjk.TYPE_MOON_WORLD.magic;

import java.util.Set;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;

public final class MagicDisplayMetadata {
   public static final String CATEGORY_ALL = "all";
   public static final String CATEGORY_BASIC = "basic";
   public static final String CATEGORY_ELEMENTAL = "elemental";
   public static final String CATEGORY_CHURCH = "church";
   public static final String CATEGORY_SPECIAL = "special";
   public static final String CATEGORY_JEWEL = "jewel";
   public static final String CATEGORY_UBW = "unlimited_blade_works";
   public static final String CATEGORY_OTHER = "other";
   public static final String CATEGORY_NORDIC = "nordic";
   public static final String CATEGORY_RUNE = "rune";
   public static final String CATEGORY_MARTIAL = "martial";
   public static final String CATEGORY_TALENT = "talent";
   public static final String CATEGORY_IMAGINARY = "imaginary";
   public static final String CATEGORY_SOLOMON = "solomon";
   public static final String CATEGORY_SPAWN = "spawn";
   public static final String CATEGORY_WORM = "worm";
   public static final String CATEGORY_BOUNDARY = "boundary";
   private static final Set<String> CHURCH_MAGICS = Set.of("theology", "black_key_making", "iron_armor_action", "cremation_rite", "baptism_rite", "stigma");
   private static final Set<String> SPAWN_MAGICS = Set.of("spirit_summoning", "wraith_servitude", "evil_spirit_summoning");
   private static final Set<String> WORM_MAGICS = Set.of("worm_magic", "worm_control", "engraved_worm_operation");
   private static final Set<String> BOUNDARY_MAGICS = Set.of(
      "boundary_art", "sensing_boundary", "warning_boundary", "defense_boundary",
      "suggestion_boundary", "anti_magic_boundary", "guard_boundary", "interference_boundary"
   );
   private static final Set<String> IMAGINARY_MAGICS = Set.of(
      "absorption", "storage", "imaginary_displacement", "imaginary_dive", "imaginary_space"
   );
   private static final Set<String> SOLOMON_MAGICS = Set.of(
      "andrasias", "andrephius", "antores", "demon_god_gaze", "kimaris", "nega_summon", "orias", "storm", "zagan"
   );
   private static final Set<String> CREST_FORBIDDEN_MAGICS = Set.of(
      "theology", "black_key_making", "iron_armor_action", "cremation_rite", "baptism_rite", "stigma", "bajiquan", "ganryu", "hokushin_ittoryu", "tennen_rishin_ryu",
      "imaginary_absorption", "imaginary_absorption_evolved", "shadow_materialization", "black_mud_control", "summon_black_mud",
      "shadow_binding", "shadow_transfer", "heroic_spirit_devourer", "forbidden_magic", "shadow_art"
   );

   private MagicDisplayMetadata() {
   }

   public static boolean isChurchMagic(String magicId) {
      return CHURCH_MAGICS.contains(magicPath(magicId));
   }

   public static boolean isMartialMagic(String magicId) {
      String path = magicPath(magicId);
      return "bajiquan".equals(path) || "ganryu".equals(path) || "hokushin_ittoryu".equals(path) || "tennen_rishin_ryu".equals(path);
   }

   public static boolean isTalent(String magicId) {
      return TalentService.isTalent(magicPath(magicId));
   }

   public static boolean isImaginaryMagic(String magicId) {
      return IMAGINARY_MAGICS.contains(magicPath(magicId));
   }

   public static boolean isSolomonMagic(String magicId) {
      return SOLOMON_MAGICS.contains(magicPath(magicId));
   }

   public static boolean canEnterMagicCrest(String magicId) {
      String path = magicPath(magicId);
      return !path.isEmpty() && !isTalent(path) && !CREST_FORBIDDEN_MAGICS.contains(path) && MagicDefinitionRegistry.isCrestAllowed(magicId);
   }

   public static boolean isSpecialMagic(String magicId) {
      String path = magicPath(magicId);
      return "time_alter".equals(path) || "baptism_rite".equals(path);
   }

   public static String categoryOf(String magicId) {
      String path = magicPath(magicId);
      if (path.isEmpty()) {
         return CATEGORY_OTHER;
      } else if (isTalent(magicId)) {
         return CATEGORY_TALENT;
      } else if (isImaginaryMagic(magicId)) {
         return CATEGORY_IMAGINARY;
      } else if (isSolomonMagic(magicId)) {
         return CATEGORY_SOLOMON;
      } else if (isSpawnMagic(magicId)) {
         return CATEGORY_SPAWN;
      } else if (isWormMagic(magicId)) {
         return CATEGORY_WORM;
      } else if (isBoundaryMagic(magicId)) {
         return CATEGORY_BOUNDARY;
      } else if ("rune_origin".equals(path)) {
         return CATEGORY_RUNE + "," + CATEGORY_NORDIC;
      } else if (path.startsWith("jewel_") || path.startsWith("ruby") || path.startsWith("sapphire")
         || path.startsWith("emerald") || path.startsWith("topaz") || path.startsWith("cyan")) {
         return CATEGORY_JEWEL;
      } else if ("projection".equals(path) || "structural_analysis".equals(path) || "broken_phantasm".equals(path)
         || "ubw_sword_control".equals(path)) {
         return CATEGORY_UBW;
      } else if ("gander".equals(path) || "gandr_machine_gun".equals(path)) {
         return CATEGORY_NORDIC;
      } else if ("fire_magic".equals(path) || "water_magic".equals(path) || "wind_magic".equals(path) || "earth_magic".equals(path)) {
         return CATEGORY_ELEMENTAL;
      } else if (isChurchMagic(magicId)) {
         return CATEGORY_CHURCH;
      } else if (isMartialMagic(magicId)) {
         return CATEGORY_MARTIAL;
      } else if ("time_alter".equals(path)) {
         return CATEGORY_SPECIAL;
      } else if (PlayerMagicSelectionService.isElementalMagic(path)) {
         return CATEGORY_ELEMENTAL;
      } else if (MagicDefinitionRegistry.contains(magicId)) {
         return MagicDefinitionRegistry.get(magicId).category().getPath();
      } else {
         return CATEGORY_BASIC;
      }
   }

   private static boolean isSpawnMagic(String magicId) {
      return SPAWN_MAGICS.contains(magicPath(magicId));
   }

   private static boolean isWormMagic(String magicId) {
      return WORM_MAGICS.contains(magicPath(magicId));
   }

   private static boolean isBoundaryMagic(String magicId) {
      return BOUNDARY_MAGICS.contains(magicPath(magicId));
   }

   private static String magicPath(String magicId) {
      if (magicId == null) {
         return "";
      }
      int split = magicId.indexOf(':');
      return split >= 0 ? magicId.substring(split + 1) : magicId;
   }
}

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
   public static final String CATEGORY_MARTIAL = "martial";
   public static final String CATEGORY_TALENT = "talent";
   public static final String CATEGORY_IMAGINARY = "imaginary";
   public static final String CATEGORY_SOLOMON = "solomon";
   private static final Set<String> CHURCH_MAGICS = Set.of("baptism_rite", "black_key_fire_engraving", "stigma");
   private static final Set<String> IMAGINARY_MAGICS = Set.of(
      "absorption", "storage", "imaginary_displacement", "imaginary_dive", "imaginary_space"
   );
   private static final Set<String> SOLOMON_MAGICS = Set.of(
      "andrasias", "andrephius", "antores", "demon_god_gaze", "kimaris", "nega_summon", "orias", "storm", "zagan"
   );
   private static final Set<String> CREST_FORBIDDEN_MAGICS = Set.of(
      "baptism_rite", "black_key_fire_engraving", "stigma", "bajiquan", "ganryu", "hokushin_ittoryu", "tennen_rishin_ryu"
   );

   private MagicDisplayMetadata() {
   }

   public static boolean isChurchMagic(String magicId) {
      return magicId != null && CHURCH_MAGICS.contains(magicId);
   }

   public static boolean isMartialMagic(String magicId) {
      return "bajiquan".equals(magicId) || "ganryu".equals(magicId) || "hokushin_ittoryu".equals(magicId) || "tennen_rishin_ryu".equals(magicId);
   }

   public static boolean isTalent(String magicId) {
      return TalentService.isTalent(magicId);
   }

   public static boolean isImaginaryMagic(String magicId) {
      return magicId != null && IMAGINARY_MAGICS.contains(magicId);
   }

   public static boolean isSolomonMagic(String magicId) {
      return magicId != null && SOLOMON_MAGICS.contains(magicId);
   }

   public static boolean canEnterMagicCrest(String magicId) {
      return magicId != null && !isTalent(magicId) && !CREST_FORBIDDEN_MAGICS.contains(magicId) && MagicDefinitionRegistry.isCrestAllowed(magicId);
   }

   public static boolean isSpecialMagic(String magicId) {
      return "time_alter".equals(magicId) || "baptism_rite".equals(magicId);
   }

   public static String categoryOf(String magicId) {
      if (magicId == null || magicId.isEmpty()) {
         return CATEGORY_OTHER;
      } else if (isTalent(magicId)) {
         return CATEGORY_TALENT;
      } else if (isImaginaryMagic(magicId)) {
         return CATEGORY_IMAGINARY;
      } else if (isSolomonMagic(magicId)) {
         return CATEGORY_SOLOMON;
      } else if (magicId.startsWith("jewel_") || magicId.startsWith("ruby") || magicId.startsWith("sapphire")
         || magicId.startsWith("emerald") || magicId.startsWith("topaz") || magicId.startsWith("cyan")) {
         return CATEGORY_JEWEL;
      } else if ("projection".equals(magicId) || "structural_analysis".equals(magicId) || "broken_phantasm".equals(magicId)
         || "ubw_sword_control".equals(magicId)) {
         return CATEGORY_UBW;
      } else if ("gander".equals(magicId) || "gandr_machine_gun".equals(magicId)) {
         return CATEGORY_NORDIC;
      } else if ("fire_magic".equals(magicId) || "water_magic".equals(magicId) || "wind_magic".equals(magicId) || "earth_magic".equals(magicId)) {
         return CATEGORY_ELEMENTAL;
      } else if (isChurchMagic(magicId)) {
         return CATEGORY_CHURCH;
      } else if (isMartialMagic(magicId)) {
         return CATEGORY_MARTIAL;
      } else if ("time_alter".equals(magicId)) {
         return CATEGORY_SPECIAL;
      } else if (PlayerMagicSelectionService.isElementalMagic(magicId)) {
         return CATEGORY_ELEMENTAL;
      } else if (MagicDefinitionRegistry.contains(magicId)) {
         return MagicDefinitionRegistry.get(magicId).category().getPath();
      } else {
         return CATEGORY_BASIC;
      }
   }
}

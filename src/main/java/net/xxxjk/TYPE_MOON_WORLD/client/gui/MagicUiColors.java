package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;

public final class MagicUiColors {
   public static final int NORMAL = 0xFF2E74D2;
   public static final int CREST = 0xFFE8505C;
   public static final int CHURCH = 0xFFF0C63C;
   public static final int MARTIAL = 0xFF2EB872;
   public static final int TALENT = 0xFFB66CFF;

   private MagicUiColors() {
   }

   public static int colorFor(String magicId, boolean crestSource) {
      if (MagicDisplayMetadata.isTalent(magicId)) {
         return TALENT;
      }
      if (crestSource) {
         return CREST;
      }
      if (MagicDisplayMetadata.isChurchMagic(magicId)) {
         return CHURCH;
      }
      return MagicDisplayMetadata.isMartialMagic(magicId) ? MARTIAL : NORMAL;
   }

   public static int withAlpha(int argb, int alpha) {
      return (argb & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
   }

   public static int red(int argb) {
      return argb >> 16 & 0xFF;
   }

   public static int green(int argb) {
      return argb >> 8 & 0xFF;
   }

   public static int blue(int argb) {
      return argb & 0xFF;
   }
}

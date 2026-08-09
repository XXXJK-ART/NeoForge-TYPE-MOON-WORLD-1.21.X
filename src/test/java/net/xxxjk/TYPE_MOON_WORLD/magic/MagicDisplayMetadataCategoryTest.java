package net.xxxjk.TYPE_MOON_WORLD.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class MagicDisplayMetadataCategoryTest {
   @Test
   void imaginaryMagicsHaveTheirOwnCategory() {
      Set<String> ids = Set.of(
         "absorption", "storage", "imaginary_displacement", "imaginary_dive", "imaginary_space"
      );
      for (String id : ids) {
         assertTrue(MagicDisplayMetadata.isImaginaryMagic(id), id);
         assertEquals(MagicDisplayMetadata.CATEGORY_IMAGINARY, MagicDisplayMetadata.categoryOf(id), id);
      }
   }

   @Test
   void solomonMagicsIncludeDemonNamesGazeAndNegaSummon() {
      Set<String> ids = Set.of(
         "andrasias", "andrephius", "antores", "demon_god_gaze", "kimaris",
         "nega_summon", "orias", "storm", "zagan"
      );
      for (String id : ids) {
         assertTrue(MagicDisplayMetadata.isSolomonMagic(id), id);
         assertEquals(MagicDisplayMetadata.CATEGORY_SOLOMON, MagicDisplayMetadata.categoryOf(id), id);
      }
   }
}

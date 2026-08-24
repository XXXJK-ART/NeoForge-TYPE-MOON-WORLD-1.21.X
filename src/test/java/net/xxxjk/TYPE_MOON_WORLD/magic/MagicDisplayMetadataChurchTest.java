package net.xxxjk.TYPE_MOON_WORLD.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MagicDisplayMetadataChurchTest {
   @Test
   void newChurchMagicsUseChurchKnowledgeMetadata() {
      for (String id : new String[]{"theology", "black_key_making", "iron_armor_action", "cremation_rite", "baptism_rite", "stigma"}) {
         assertTrue(MagicDisplayMetadata.isChurchMagic(id));
         assertEquals(MagicDisplayMetadata.CATEGORY_CHURCH, MagicDisplayMetadata.categoryOf(id));
         assertFalse(MagicDisplayMetadata.canEnterMagicCrest(id));
      }
   }

   @Test
   void namespacedChurchMagicsCannotEnterMagicCrest() {
      for (String id : new String[]{
         "typemoonworld:theology",
         "typemoonworld:black_key_making",
         "typemoonworld:iron_armor_action",
         "typemoonworld:cremation_rite",
         "typemoonworld:baptism_rite",
         "typemoonworld:stigma"
      }) {
         assertTrue(MagicDisplayMetadata.isChurchMagic(id));
         assertEquals(MagicDisplayMetadata.CATEGORY_CHURCH, MagicDisplayMetadata.categoryOf(id));
         assertFalse(MagicDisplayMetadata.canEnterMagicCrest(id));
      }
   }

   @Test
   void manaBurstCanEnterMagicCrest() {
      assertTrue(MagicDisplayMetadata.canEnterMagicCrest("mana_burst"));
   }
}

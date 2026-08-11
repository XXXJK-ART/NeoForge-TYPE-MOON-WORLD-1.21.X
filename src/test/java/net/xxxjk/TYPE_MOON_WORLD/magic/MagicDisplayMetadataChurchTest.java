package net.xxxjk.TYPE_MOON_WORLD.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MagicDisplayMetadataChurchTest {
   @Test
   void newChurchMagicsUseChurchKnowledgeMetadata() {
      for (String id : new String[]{"black_key_fire_engraving", "stigma"}) {
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

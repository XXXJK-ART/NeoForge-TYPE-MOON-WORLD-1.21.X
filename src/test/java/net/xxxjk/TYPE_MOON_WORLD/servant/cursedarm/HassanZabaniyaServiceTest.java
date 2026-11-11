package net.xxxjk.TYPE_MOON_WORLD.servant.cursedarm;

import static org.junit.jupiter.api.Assertions.assertFalse;

import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HassanZabaniyaService;
import org.junit.jupiter.api.Test;

class HassanZabaniyaServiceTest {
   @Test
   void missingHeldCoreDoesNotCountAsExtractedCore() {
      assertFalse(HassanZabaniyaService.isExtractedItem(null));
   }
}

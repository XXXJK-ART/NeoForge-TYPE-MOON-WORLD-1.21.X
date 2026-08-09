package net.xxxjk.TYPE_MOON_WORLD.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.UbwSwordControlService;
import org.junit.jupiter.api.Test;

class UbwSwordControlServiceTest {
   @Test
   void swordLimitScalesFromUnlockedMinimumToConfiguredMaximum() {
      assertEquals(8, UbwSwordControlService.swordLimit(10.0D));
      assertEquals(36, UbwSwordControlService.swordLimit(55.0D));
      assertEquals(64, UbwSwordControlService.swordLimit(100.0D));
      assertEquals(64, UbwSwordControlService.swordLimit(150.0D));
   }
}

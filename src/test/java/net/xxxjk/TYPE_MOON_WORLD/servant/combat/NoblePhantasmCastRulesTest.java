package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class NoblePhantasmCastRulesTest {
   @Test void onlyConfiguredNpsCanBeInterrupted() {
      assertTrue(NoblePhantasmCastRules.interruptible("artoria_excalibur"));
      assertFalse(NoblePhantasmCastRules.interruptible("arash_stella"));
      assertFalse(NoblePhantasmCastRules.interruptible("enkidu_enuma_elish"));
   }
   @Test void interruptedCastUsesHalfSettlement() {
      assertEquals(50.0, NoblePhantasmCastRules.interruptedCost(100.0));
      assertEquals(60, NoblePhantasmCastRules.interruptedCooldown(120));
      assertFalse(NoblePhantasmCastRules.effectReleased(99, 100));
      assertTrue(NoblePhantasmCastRules.effectReleased(100, 100));
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class NoblePhantasmCastRulesTest {
   @Test void onlyConfiguredNpsCanBeInterrupted() {
      assertTrue(NoblePhantasmCastRules.interruptible("artoria_excalibur"));
      assertFalse(NoblePhantasmCastRules.interruptible("okita_mumyoudan_zuki"));
      assertFalse(NoblePhantasmCastRules.interruptible("arash_stella"));
      assertFalse(NoblePhantasmCastRules.interruptible("enkidu_enuma_elish"));
   }
   @Test void instantNpsNeverEnterTheInterruptWindow() {
      for (String id : new String[]{"okita_mumyoudan_zuki", "cursed_arm_hassan_zabaniya", "li_shuwen_wu_er_da", "lancelot_arondight"}) {
         assertTrue(NoblePhantasmCastRules.instant(id), id);
         assertFalse(NoblePhantasmCastRules.interruptible(id), id);
      }
      assertFalse(NoblePhantasmCastRules.instant("artoria_excalibur"));
      assertFalse(NoblePhantasmCastRules.instant("gilgamesh_ea"));
      assertFalse(NoblePhantasmCastRules.instant("muramasa_tsumukari"));
   }
   @Test void interruptedCastUsesHalfSettlement() {
      assertEquals(50.0, NoblePhantasmCastRules.interruptedCost(100.0));
      assertEquals(60, NoblePhantasmCastRules.interruptedCooldown(120));
      assertFalse(NoblePhantasmCastRules.effectReleased(159, 220));
      assertTrue(NoblePhantasmCastRules.effectReleased(160, 220));
      assertEquals(20, NoblePhantasmCastRules.windowTicks());
      assertFalse(NoblePhantasmCastRules.interruptibleAt(139, 0, 220));
      assertTrue(NoblePhantasmCastRules.interruptibleAt(140, 0, 220));
      assertTrue(NoblePhantasmCastRules.interruptibleAt(159, 0, 220));
      assertFalse(NoblePhantasmCastRules.interruptibleAt(160, 0, 220));
      assertFalse(NoblePhantasmCastRules.interruptibleAt(180, 0, 220));
      assertFalse(NoblePhantasmCastRules.interruptibleAt(0, 0, 20));
   }
}

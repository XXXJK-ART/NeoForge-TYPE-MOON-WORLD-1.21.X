package net.xxxjk.typemoonworld.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class AdvancedAiTacticProfileTest {
   @Test
   void compatibleProfilePreservesV1BaseAndAddsBoundedDefaults() {
      AiTacticProfile base = new AiTacticProfile(24, 12, 0.2, 10, List.of());
      AdvancedAiTacticProfile advanced = AdvancedAiTacticProfile.compatible(base);
      assertSame(base, advanced.base());
      assertEquals(AiCombatStyle.BALANCED, advanced.style());
      assertEquals(12.0, advanced.preferredRange());
      assertEquals(24.0, advanced.maximumRange());
   }

   @Test
   void advancedRangesCannotEscapeNormalCombatBoundary() {
      AiTacticProfile base = new AiTacticProfile(24, 12, 0.2, 10, List.of());
      AdvancedAiTacticProfile advanced = new AdvancedAiTacticProfile(base, AiCombatStyle.SNIPER,
         80.0, 96.0, 128.0, 30.0, 2.0, 20.0, 2.0, 2.0, -1.0, "heavy");
      assertEquals(48.0, advanced.minimumRange());
      assertEquals(48.0, advanced.preferredRange());
      assertEquals(48.0, advanced.maximumRange());
      assertEquals(24.0, advanced.repositionDistance());
      assertEquals(1.0, advanced.pursuitAggression());
      assertEquals(8.0, advanced.interceptBias());
      assertEquals(1.0, advanced.verticalMobility());
      assertEquals(1.0, advanced.recoveryTendency());
      assertEquals(0.0, advanced.collateralCaution());
   }
}

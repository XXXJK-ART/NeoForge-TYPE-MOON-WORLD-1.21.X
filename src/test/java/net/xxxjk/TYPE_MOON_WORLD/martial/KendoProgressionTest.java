package net.xxxjk.TYPE_MOON_WORLD.martial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class KendoProgressionTest {
   @Test
   void schoolThresholdsAreDifferent() {
      assertEquals(5.0, KendoCombatService.requiredProficiency(KendoSchool.HOKUSHIN, KendoMove.PRIMARY_TWO));
      assertEquals(10.0, KendoCombatService.requiredProficiency(KendoSchool.TENNEN, KendoMove.PRIMARY_ONE));
      assertEquals(20.0, KendoCombatService.requiredProficiency(KendoSchool.TENNEN, KendoMove.ARC));
   }

   @Test
   void progressionCapsStayAtTheConfiguredValues() {
      assertEquals(50.0, KendoSchool.HOKUSHIN.preMasterCap());
      assertEquals(80.0, KendoSchool.TENNEN.preMasterCap());
      assertEquals(100.0, KendoCombatService.requiredProficiency(KendoSchool.TENNEN, KendoMove.PERFECT_SWORD));
   }
}

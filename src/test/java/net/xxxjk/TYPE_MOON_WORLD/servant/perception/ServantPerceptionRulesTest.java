package net.xxxjk.TYPE_MOON_WORLD.servant.perception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServantPerceptionRulesTest {
   @Test void concealmentRankOverridesObservationAndExConcealmentHides() {
      assertEquals(9.0, ServantPerceptionRules.radius(List.of("presence_concealment_e", "clairvoyance_a")));
      assertEquals(20.0, ServantPerceptionRules.radius(List.of("presence_concealment_e", "instinct_a"), false));
      assertEquals(9.0, ServantPerceptionRules.radius(List.of("presence_concealment_e", "instinct_a")));
      assertEquals(0.0, ServantPerceptionRules.radius(List.of("presence_concealment_ex", "instinct_a")));
   }

   @Test void observerSkillRanksExpandRadiusAndCapAboveA() {
      assertEquals(18.0, ServantPerceptionRules.radius(List.of("true_mind_eye_b")), 1.0e-9);
      assertEquals(20.0, ServantPerceptionRules.radius(List.of("artoria_instinct_a")), 1.0e-9);
      assertEquals(20.0, ServantPerceptionRules.radius(List.of("clairvoyance_ex")), 1.0e-9);
   }

   @Test void pulseUsesTwentyTicksOnAndTwentyTicksOff() {
      assertEquals(20, ServantPerceptionRules.BRIGHT_TICKS);
      assertEquals(20, ServantPerceptionRules.DARK_TICKS);
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.concealment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConcealmentRankTest {
   @Test
   void parsesEverySupportedSkillRank() {
      assertEquals(ConcealmentRank.EX,
         ConcealmentRank.fromSkillId("presence_concealment_ex_shadow_hassan"));
      assertEquals(ConcealmentRank.A_PLUS,
         ConcealmentRank.fromSkillId("presence_concealment_a_plus"));
      assertEquals(ConcealmentRank.A_MINUS,
         ConcealmentRank.fromSkillId("presence_concealment_a_minus_fanatic"));
      assertEquals(ConcealmentRank.A, ConcealmentRank.fromSkillId("presence_concealment_a"));
      assertEquals(ConcealmentRank.B, ConcealmentRank.fromSkillId("presence_concealment_b"));
      assertEquals(ConcealmentRank.C, ConcealmentRank.fromSkillId("presence_concealment_c"));
      assertEquals(ConcealmentRank.D, ConcealmentRank.fromSkillId("stealth_d"));
      assertEquals(ConcealmentRank.E, ConcealmentRank.fromSkillId("stealth_e"));
      assertEquals(ConcealmentRank.NONE, ConcealmentRank.fromSkillId("invisible_air"));
   }

   @Test
   void particleCapsFollowRankWithLargeGaps() {
      assertEquals(0, ConcealmentRank.EX.maxParticles());
      assertEquals(0, ConcealmentRank.A_PLUS.maxParticles());
      assertEquals(4, ConcealmentRank.A.maxParticles());
      assertEquals(8, ConcealmentRank.A_MINUS.maxParticles());
      assertEquals(12, ConcealmentRank.B.maxParticles());
      assertEquals(20, ConcealmentRank.C.maxParticles());
      assertEquals(30, ConcealmentRank.D.maxParticles());
      assertEquals(40, ConcealmentRank.E.maxParticles());
      assertTrue(ConcealmentRank.EX.isPerfect());
      assertTrue(ConcealmentRank.A_PLUS.isPerfect());
      assertFalse(ConcealmentRank.A.isPerfect());
   }

   @Test
   void weakerRanksLeakAtShorterIntervals() {
      assertTrue(ConcealmentRank.E.maximumDelay() < ConcealmentRank.D.maximumDelay());
      assertTrue(ConcealmentRank.D.maximumDelay() < ConcealmentRank.C.maximumDelay());
      assertTrue(ConcealmentRank.C.maximumDelay() < ConcealmentRank.B.maximumDelay());
      assertTrue(ConcealmentRank.B.maximumDelay() < ConcealmentRank.A_MINUS.maximumDelay());
      assertTrue(ConcealmentRank.A_MINUS.maximumDelay() < ConcealmentRank.A.maximumDelay());
   }
}

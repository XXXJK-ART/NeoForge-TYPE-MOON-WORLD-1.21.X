package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RatSwarmRulesTest {
   @Test
   void healthMapsToRatAnimationAndMaximum() {
      assertEquals(1, RatSwarmRules.ratsForHealth(0.1F));
      assertEquals(1, RatSwarmRules.ratsForHealth(5.0F));
      assertEquals(2, RatSwarmRules.ratsForHealth(10.0F));
      assertEquals(3, RatSwarmRules.ratsForHealth(11.0F));
      assertEquals(20, RatSwarmRules.ratsForHealth(100.0F));
      assertEquals("2", RatSwarmRules.animationForHealth(10.0F));
      assertEquals("3", RatSwarmRules.animationForHealth(11.0F));
      assertEquals(10.0F, RatSwarmRules.maximumForHealth(10.0F));
      assertEquals(15.0F, RatSwarmRules.maximumForHealth(11.0F));
   }

   @Test
   void mergeFillsOneSwarmAndLeavesRemainder() {
      RatSwarmRules.MergeResult simple = RatSwarmRules.merge(35.0F, 40.0F);
      assertEquals(75.0F, simple.primaryHealth());
      assertEquals(0.0F, simple.remainderHealth());

      RatSwarmRules.MergeResult overflow = RatSwarmRules.merge(80.0F, 55.0F);
      assertEquals(100.0F, overflow.primaryHealth());
      assertEquals(35.0F, overflow.remainderHealth());
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;

class ShadowHassanRulesTest {
   @Test
   void shadowLightExcludesTotalDarknessAndStrongLight() {
      assertFalse(ShadowHassanRules.isShadowLight(0));
      assertTrue(ShadowHassanRules.isShadowLight(1));
      assertTrue(ShadowHassanRules.isShadowLight(11));
      assertFalse(ShadowHassanRules.isShadowLight(12));
      assertFalse(ShadowHassanRules.isShadowLight(15));
      assertTrue(ShadowHassanRules.isTotalDarkness(0));
      assertFalse(ShadowHassanRules.isTotalDarkness(1));
   }

   @Test
   void entitiesInDirectSunlightCastUsableShadows() {
      assertTrue(ShadowHassanRules.isSunlightShadow(true, true, false, 15));
      assertTrue(ShadowHassanRules.isSunlightShadow(true, true, false, 1));
      assertFalse(ShadowHassanRules.isSunlightShadow(false, true, false, 15));
      assertFalse(ShadowHassanRules.isSunlightShadow(true, false, false, 15));
      assertFalse(ShadowHassanRules.isSunlightShadow(true, true, true, 15));
      assertFalse(ShadowHassanRules.isSunlightShadow(true, true, false, 0));
   }

   @Test
   void noblePhantasmThresholdIsInclusiveAndRequiresLife() {
      assertTrue(ShadowHassanRules.shouldTriggerNoblePhantasm(180.0F, 300.0F));
      assertTrue(ShadowHassanRules.shouldTriggerNoblePhantasm(179.99F, 300.0F));
      assertFalse(ShadowHassanRules.shouldTriggerNoblePhantasm(180.01F, 300.0F));
      assertFalse(ShadowHassanRules.shouldTriggerNoblePhantasm(0.0F, 300.0F));
   }

   @Test
   void timingsAndPursuitSpeedMatchTheDesign() {
      assertEquals(60, ShadowHassanRules.CONCEALMENT_EXPOSURE_TICKS);
      assertEquals(40, ShadowHassanRules.SHADOW_STEP_COOLDOWN_TICKS);
      assertEquals(20, ShadowHassanRules.MANA_RESTORE_INTERVAL_TICKS);
      assertEquals(5.0, ShadowHassanRules.MANA_RESTORE_PER_SECOND);
      assertEquals(0.70, ShadowHassanRules.DEATH_SHADOW_SPEED_PER_TICK);
      assertEquals(14.0, ShadowHassanRules.DEATH_SHADOW_SPEED_PER_TICK * 20.0);
      assertEquals(1.0, ShadowHassanRules.DEATH_SHADOW_CONTACT_DISTANCE);
   }

   @Test
   void pursuitAdvancesExactlyPointSevenBlocksWithoutOvershooting() {
      Vec3 origin = Vec3.ZERO;
      Vec3 next = ShadowHassanRules.advanceDeathShadow(origin, new Vec3(3.0, 4.0, 0.0));
      assertEquals(0.70, next.length(), 1.0E-9);
      Vec3 destination = new Vec3(0.2, 0.0, 0.0);
      assertEquals(destination, ShadowHassanRules.advanceDeathShadow(origin, destination));
   }

   @Test
   void servantRanksProduceThePlannedBaseStats() {
      ServantParams params = ServantParams.of("C", false, "D", false, "C", false, "D", false, "E", false);
      assertEquals(300.0, params.maxHealth());
      assertEquals(10.0, params.attackDamage());
      assertEquals(0.28, params.movementSpeed());
      assertEquals(4.5, params.armor());
      assertEquals(400.0, params.manaPool());
   }
}

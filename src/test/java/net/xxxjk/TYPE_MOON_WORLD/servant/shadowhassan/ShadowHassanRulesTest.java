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
   void activeConcealmentRequiresDirectSunlight() {
      assertTrue(ShadowHassanRules.isDirectSunlight(true, true, false, 15));
      assertFalse(ShadowHassanRules.isDirectSunlight(false, true, false, 15));
      assertFalse(ShadowHassanRules.isDirectSunlight(true, false, false, 15));
      assertFalse(ShadowHassanRules.isDirectSunlight(true, true, true, 15));
      assertFalse(ShadowHassanRules.isDirectSunlight(true, true, false, 0));
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
      assertEquals(10, ShadowHassanRules.CONCEALMENT_EXPOSURE_TICKS);
      assertEquals(100, ShadowHassanRules.SHADOW_STEP_COOLDOWN_TICKS);
      assertEquals(20, ShadowHassanRules.MANA_RESTORE_INTERVAL_TICKS);
      assertEquals(5.0, ShadowHassanRules.MANA_RESTORE_PER_SECOND);
      assertEquals(0.70, ShadowHassanRules.DEATH_SHADOW_SPEED_PER_TICK);
      assertEquals(14.0, ShadowHassanRules.DEATH_SHADOW_SPEED_PER_TICK * 20.0);
      assertEquals(1.0, ShadowHassanRules.DEATH_SHADOW_CONTACT_DISTANCE);
      assertEquals(12, ShadowHassanRules.SHADOW_STEP_MAX_DISTANCE);
      assertEquals(5, ShadowHassanRules.SHADOW_POINT_SEARCH_RADIUS);
      assertEquals(18, ShadowHassanRules.POSSESSION_RANGE);
      assertEquals(0.05, ShadowHassanRules.TOTAL_DARKNESS_MOVEMENT_MULTIPLIER);
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
   void totalDarknessRequiresBothLightLayersToBeZero() {
      assertTrue(ShadowHassanRules.isTotalDarkness(0, 0));
      assertFalse(ShadowHassanRules.isTotalDarkness(1, 0));
      assertFalse(ShadowHassanRules.isTotalDarkness(0, 1));
      assertEquals(0.05, ShadowHassanRules.movementMultiplier(0, 0));
      assertEquals(1.0, ShadowHassanRules.movementMultiplier(0, 1));
   }

   @Test
   void shadowZoneAcceptsLowLightCoverOrProjectionAndRejectsStrongLight() {
      assertTrue(ShadowHassanRules.isUsableShadowZone(5, 0, false, false));
      assertTrue(ShadowHassanRules.isUsableShadowZone(15, 15, true, false));
      assertTrue(ShadowHassanRules.isUsableShadowZone(15, 15, false, true));
      assertFalse(ShadowHassanRules.isUsableShadowZone(15, 15, false, false));
      assertFalse(ShadowHassanRules.isUsableShadowZone(0, 0, true, true));
   }

   @Test
   void possessionRequiresAVisibleLegalHostWithAUsableShadowAnchor() {
      assertTrue(ShadowHassanRules.canBeginPossession(false, true, true, true, true));
      assertFalse(ShadowHassanRules.canBeginPossession(true, true, true, true, true));
      assertFalse(ShadowHassanRules.canBeginPossession(false, false, true, true, true));
      assertFalse(ShadowHassanRules.canBeginPossession(false, true, false, true, true));
      assertFalse(ShadowHassanRules.canBeginPossession(false, true, true, false, true));
      assertFalse(ShadowHassanRules.canBeginPossession(false, true, true, true, false));
   }

   @Test
   void possessionManaUsesTheShadowLanternBeforeItsMaintenanceCost() {
      assertEquals(5.0, ShadowHassanRules.possessionManaAfterInterval(1.0, 400.0, true));
      assertEquals(0.0, ShadowHassanRules.possessionManaAfterInterval(1.0, 400.0, false));
      assertEquals(399.0, ShadowHassanRules.possessionManaAfterInterval(400.0, 400.0, true));
      assertEquals(0.0, ShadowHassanRules.possessionManaAfterInterval(0.0, 400.0, true));
   }

   @Test
   void normalCombatWaitsUntilShadowAndAttackExposureAreBothGone() {
      assertFalse(ShadowHassanRules.isFullyExposed(true, false));
      assertFalse(ShadowHassanRules.isFullyExposed(false, true));
      assertTrue(ShadowHassanRules.isFullyExposed(false, false));
   }

   @Test
   void servantRanksProduceThePlannedBaseStats() {
      ServantParams params = ServantParams.of("C", false, "D", false, "C", false, "D", false, "E", false);
      assertEquals(300.0, params.maxHealth());
      assertEquals(10.0, params.attackDamage());
      assertEquals(0.28, params.movementSpeed());
      assertEquals(3.0, params.armor());
      assertEquals(400.0, params.manaPool());
   }
}

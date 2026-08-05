package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosBeastLogic;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("typemoonworld")
@PrefixGameTestTemplate(false)
public final class NeroChaosGameTests {
   private NeroChaosGameTests() {
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void neroStartsWithIndependentLivesAndAttributes(GameTestHelper helper) {
      NeroChaosEntity nero = helper.spawn(ModEntities.NERO_CHAOS.get(), new BlockPos(5, 8, 3));

      helper.assertTrue(nero.getRemainingLives() == 666, "Nero should start with 666 lives");
      helper.assertTrue(Math.abs(nero.getMaxHealth() - 400.0F) < 0.01F, "Nero should have 400 health");
      helper.assertTrue(nero.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == 20.0,
         "Nero should have 20 attack damage");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 60)
   public static void neroReleasesOwnedBeasts(GameTestHelper helper) {
      NeroChaosEntity nero = helper.spawn(ModEntities.NERO_CHAOS.get(), new BlockPos(5, 8, 3));
      helper.runAfterDelay(35, () -> {
         long beasts = helper.getLevel().getEntitiesOfClass(
            LivingEntity.class,
            nero.getBoundingBox().inflate(96.0),
            NeroChaosBeastLogic::isBeast
         ).stream().filter(beast -> nero.getUUID().equals(NeroChaosBeastLogic.ownerUuid(beast))).count();
         helper.assertTrue(beasts >= 2 && beasts <= 5, "Non-combat Nero should release 2-5 scout beasts");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void beastDeathDebitsExactlyOneLifeAndLastLifeKillsNero(GameTestHelper helper) {
      NeroChaosEntity nero = helper.spawn(ModEntities.NERO_CHAOS.get(), new BlockPos(5, 8, 3));
      var beast = helper.spawn(ModEntities.NERO_CHAOS_HOUND.get(), new BlockPos(6, 8, 3));
      NeroChaosBeastLogic.setOwner(beast, nero);
      nero.setRemainingLives(2);
      beast.kill();

      helper.runAfterDelay(1, () -> {
         helper.assertTrue(nero.getRemainingLives() == 1, "Beast death should debit one life");
         helper.assertTrue(nero.isAlive(), "Nero should remain alive while one life remains");

         var lastBeast = helper.spawn(ModEntities.NERO_CHAOS_HOUND.get(), new BlockPos(6, 8, 4));
         NeroChaosBeastLogic.setOwner(lastBeast, nero);
         lastBeast.kill();
         helper.runAfterDelay(1, () -> {
            helper.assertTrue(nero.getRemainingLives() == 0, "Last beast should consume the last life");
            helper.assertTrue(!nero.isAlive(), "Nero should truly die at zero lives");
            helper.succeed();
         });
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void ordinaryLethalDamageRevivesWithoutAnInvulnerabilityWindow(GameTestHelper helper) {
      NeroChaosEntity nero = helper.spawn(ModEntities.NERO_CHAOS.get(), new BlockPos(5, 8, 3));
      nero.setRemainingLives(3);
      nero.die(nero.damageSources().mobAttack(nero));

      helper.assertTrue(nero.isAlive(), "Ordinary lethal damage should revive Nero");
      helper.assertTrue(nero.getRemainingLives() == 2, "Ordinary lethal damage should consume one life");
      helper.assertTrue(nero.getHealth() == nero.getMaxHealth(), "Revived Nero should be full health");
      helper.assertTrue(nero.invulnerableTime == 0, "Revival must not add invulnerability frames");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void forcedKillBypassesRevival(GameTestHelper helper) {
      NeroChaosEntity nero = helper.spawn(ModEntities.NERO_CHAOS.get(), new BlockPos(5, 8, 3));
      nero.kill();

      helper.runAfterDelay(1, () -> {
         helper.assertTrue(!nero.isAlive(), "Forced kill should permanently kill Nero");
         helper.assertTrue(nero.getRemainingLives() == 666, "Forced kill should not spend ordinary lives");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void chaosFormShrinksAndIgnoresControlEffects(GameTestHelper helper) {
      NeroChaosEntity nero = helper.spawn(ModEntities.NERO_CHAOS.get(), new BlockPos(5, 8, 3));
      float normalWidth = nero.getBbWidth();
      float normalHeight = nero.getBbHeight();
      nero.setChaosForm(true);
      nero.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
      nero.setDeltaMovement(1.0, 0.0, 0.0);
      nero.knockback(10.0, 1.0, 0.0);
      helper.assertTrue(nero.getDeltaMovement().x > 0.99, "Chaos form should ignore knockback immediately");

      helper.runAfterDelay(2, () -> {
         helper.assertTrue(nero.getBbWidth() < normalWidth && nero.getBbHeight() < normalHeight,
            "Chaos form should use a narrow collision box");
         helper.assertTrue(!nero.isPushable(), "Chaos form should not be pushable");
         helper.assertTrue(!nero.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
            "Chaos form should ignore movement slowdown");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 60)
   public static void devourDamagesForTwentyTicksAndHealsOnDeath(GameTestHelper helper) {
      NeroChaosEntity nero = helper.spawn(ModEntities.NERO_CHAOS.get(), new BlockPos(5, 8, 3));
      var prey = helper.spawn(EntityType.ZOMBIE, new BlockPos(6, 8, 3));
      nero.setHealth(300.0F);
      nero.setChaosForm(true);
      prey.setNoAi(true);
      nero.setTarget(prey);

      helper.runAfterDelay(30, () -> {
         helper.assertTrue(!prey.isAlive(),
            "Devour should kill a nearby ordinary target; health=" + prey.getHealth()
               + ", distance=" + nero.distanceTo(prey));
         helper.assertTrue(nero.getHealth() >= 349.0F,
            "Devour should restore 50 health after the target dies; Nero health=" + nero.getHealth()
               + ", prey health=" + prey.getHealth());
         helper.succeed();
      });
   }
}

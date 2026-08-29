package net.xxxjk.TYPE_MOON_WORLD.servant;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CombatBalanceRegressionTest {
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void liShuwenWuErDaUsesCursedArmInstantDeathLevel() throws IOException {
      String helper = Files.readString(JAVA.resolve("servant/entity/LiShuwenCombatHelper.java"));

      assertTrue(helper.contains("return 0.60F;"));
      assertTrue(helper.contains("return CursedArmHassanCombatHelper.isHumanoidInstantDeathTarget(target)"));
   }

   @Test
   void thrownGaeBolgStopsHomingAfterThreeSeconds() throws IOException {
      String projectile = Files.readString(JAVA.resolve("entity/GaeBulgProjectileEntity.java"));

      assertTrue(projectile.contains("SINGLE_HOMING_TICKS = 60"));
      assertTrue(projectile.contains("this.lifeTime <= SINGLE_HOMING_TICKS"));
      assertTrue(projectile.contains("this.setTrackedTarget(null);"));
   }

   @Test
   void deathThornDoesNotOverrideGodHandRevival() throws IOException {
      String combat = Files.readString(JAVA.resolve("servant/ai/module/CombatModule.java"));
      String playerNp = Files.readString(JAVA.resolve("item/custom/PlayerNoblePhantasmHelper.java"));
      String godHand = Files.readString(JAVA.resolve("servant/entity/HeraclesGodHandHelper.java"));

      assertTrue(godHand.contains("consumedLifeAfterLethalHit"));
      assertTrue(combat.contains("HeraclesGodHandHelper.consumedLifeAfterLethalHit(target, livesBeforeHit)"));
      assertTrue(playerNp.contains("HeraclesGodHandHelper.consumedLifeAfterLethalHit(target, livesBeforeDeathThorn)"));
   }

   @Test
   void iskandarMountsAlwaysReleasePassengersBeforeRemoval() throws IOException {
      String mount = Files.readString(JAVA.resolve("entity/IskandarMountEntity.java"));

      assertTrue(mount.contains("releasePassengersBeforeRemoval();"));
      assertTrue(mount.contains("public void die(DamageSource source)"));
      assertTrue(mount.contains("public void remove(RemovalReason reason)"));
      assertTrue(mount.contains("passenger.stopRiding();"));
   }
}

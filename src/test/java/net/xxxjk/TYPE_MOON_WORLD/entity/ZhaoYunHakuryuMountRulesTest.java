package net.xxxjk.TYPE_MOON_WORLD.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ZhaoYunHakuryuMountRulesTest {
   private static final Path SOURCE = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/entity/ZhaoYunHakuryuEntity.java");

   @Test
   void boundMasterSeatIsInFrontOfAndLowerThanZhaoYunSeat() throws IOException {
      String source = Files.readString(SOURCE);

      assertTrue(source.contains("RIDER_SEAT_FORWARD = 0.18"));
      assertTrue(source.contains("RIDER_SEAT_HEIGHT = 0.82"));
      assertTrue(source.contains("MASTER_SEAT_FORWARD = 0.72"));
      assertTrue(source.contains("MASTER_SEAT_HEIGHT = 0.68"));
      assertTrue(source.contains("boolean masterSeat = isMasterPassenger(passenger)"));
   }

   @Test
   void boundMasterPassengerDamageRedirectsToHakuryu() throws IOException {
      String source = Files.readString(SOURCE);

      assertTrue(source.contains("masterUuid != null && masterUuid.equals(passengerUuid)"));
      assertTrue(source.contains("skillOwnerUuid != null && skillOwnerUuid.equals(passengerUuid)"));
      assertTrue(source.contains("riderUuid != null && riderUuid.equals(passengerUuid)"));
   }

   @Test
   void npcHakuryuToleratesBriefRiderPassengerDesync() throws IOException {
      String source = Files.readString(SOURCE);
      String riderSource = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/ZhaoYunRiderEntity.java"));
      String entitiesSource = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModEntities.java"));
      String rendererSource = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/renderer/ZhaoYunHakuryuRenderer.java"));

      assertTrue(source.contains("RIDER_RELINK_GRACE_TICKS = 20"));
      assertTrue(source.contains("tryRelinkRider(level, rider)"));
      assertTrue(source.contains("new ClientboundSetPassengersPacket(this)"));
      assertTrue(riderSource.contains("mount.requestRiderRelinkGrace()"));
      assertTrue(entitiesSource.contains(".sized(2.2F, 2.4F).clientTrackingRange(96).updateInterval(1).build(\"zhao_yun_hakuryu\")"));
      assertTrue(rendererSource.contains("entity.getPassengers().size() > 0"));
   }

   @Test
   void changbanpoTerrainBreaksForwardWithoutDiggingBelowHakuryu() throws IOException {
      String riderSource = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/ZhaoYunRiderEntity.java"));

      assertTrue(riderSource.contains("check.offset(-2, 0, -2), check.offset(2, 4, 2)"));
      assertTrue(riderSource.contains("never dig downward"));
      assertTrue(riderSource.contains("if (pos.getY() < base.getY()) continue;"));
   }

   @Test
   void npcHakuryuUsesCombatIntentInsteadOfOnlyChasingTargetPosition() throws IOException {
      String source = Files.readString(SOURCE);

      assertTrue(source.contains("resolveRiderCombatTarget"));
      assertTrue(source.contains("rider.getLastHurtByMob()"));
      assertTrue(source.contains("COMBAT_DIRECT_TICKS = 36"));
      assertTrue(source.contains("moveAroundCombatTarget"));
      assertTrue(source.contains("COMBAT_STOP_DISTANCE = 4.6"));
      assertTrue(source.contains("recoverBlockedCombatMove"));
      assertTrue(source.contains("blockedCombatMoveTicks"));
      assertTrue(source.contains("this.horizontalCollision"));
      assertTrue(source.contains("this.isInWall()"));
   }

   @Test
   void ordinaryMountedMobsReceiveRiderCombatIntent() throws IOException {
      String source = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/combat/ai/MountedCombatIntentService.java"));
      String servant = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/combat/ServantCombatSystem.java"));
      String npc = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/combat/ai/NpcTacticalController.java"));
      String humanNpc = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/entity/HumanNpcEntity.java"));

      assertTrue(source.contains("mount.setTarget(target)"));
      assertTrue(source.contains("pathfinder.getNavigation().moveTo(target, speed)"));
      assertTrue(source.contains("hasPlayerPassenger"));
      assertTrue(servant.contains("MountedCombatIntentService.tick(entity)"));
      assertTrue(npc.contains("MountedCombatIntentService.tick(entity)"));
      assertTrue(humanNpc.contains("MountedCombatIntentService.tick(this)"));
   }
}

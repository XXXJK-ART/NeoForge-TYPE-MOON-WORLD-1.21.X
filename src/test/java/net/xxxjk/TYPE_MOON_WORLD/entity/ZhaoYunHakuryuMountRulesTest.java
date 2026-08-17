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
}

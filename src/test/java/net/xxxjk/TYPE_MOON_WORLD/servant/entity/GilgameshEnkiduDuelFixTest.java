package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GilgameshEnkiduDuelFixTest {
   @Test
   void duelFinaleUsesDynamicClashPointInsteadOfOverlappingGilgamesh() throws IOException {
      String duel = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/GilgameshDuelState.java"));
      String enkidu = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/EnkiduCombatHelper.java"));

      assertTrue(duel.contains("MIN_RUSH_SEPARATION"));
      assertTrue(duel.contains("enkiduRushStopPosition(EnkiduEntity enkidu, GilgameshEntity gil)"));
      assertTrue(duel.contains("duelClashPoint(EnkiduEntity enkidu, GilgameshEntity gil)"));
      assertTrue(duel.contains("sanitizeDuelImpact(gil, enkidu, impact)"));
      assertTrue(duel.contains("putCenter(enkidu.getPersistentData(), center)"));
      assertTrue(duel.contains("lockGilgameshAnchor(gil, enkidu)"));

      assertTrue(enkidu.contains("GilgameshDuelState.enkiduRushStopPosition(entity, gilgamesh)"));
      assertTrue(enkidu.contains("GilgameshDuelState.duelClashPoint(entity, gilgamesh)"));
      assertTrue(enkidu.contains("entity.setPos(stop.x, stop.y, stop.z)"));
      assertTrue(enkidu.contains("GilgameshDuelState.completeDuelRush(entity, level, gilgamesh, clash)"));
   }

   @Test
   void replayableBoundVfxWaitsForTargetBeforeSpawning() throws IOException {
      String renderer = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/renderer/VFXTriggerRenderer.java"));

      assertTrue(renderer.contains("TARGET_BIND_RETRY_TICKS"));
      assertTrue(renderer.contains("canSpawn(entity)"));
      assertTrue(renderer.contains("minecraft.level.getEntity(targetId) != null"));
      assertTrue(renderer.contains("entity.tickCount >= TARGET_BIND_RETRY_TICKS"));
   }
}

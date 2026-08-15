package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BoundaryReactionResourcesTest {
   @Test
   void gilgameshAnswersHajunAndIonioiWithOneEaPerEntry() throws IOException {
      String source = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/GilgameshCombatHelper.java"
      ));
      assertTrue(source.contains("tryTriggerBoundaryEa(entity, level, now)"));
      assertTrue(source.contains("ModDimensions.isHajunDimension(location)"));
      assertTrue(source.contains("ModDimensions.isIonioiHetairoiDimension(location)"));
      assertTrue(source.contains("GilgameshBoundaryEaDimension"));
      assertTrue(source.contains("beginNpcEaSummon(entity, level, target, now)"));
      assertTrue(source.contains("EA_SHIELD_FORCED"));
      assertTrue(source.contains("forceEaShield(entity)"));
      assertTrue(source.contains("maintainEaShield(entity, data)"));
      assertTrue(source.contains("GilgameshDivineShield.clearCooldown(entity)"));
      assertTrue(source.contains("releaseEaShield(entity)"));
   }

   @Test
   void emiyaAnswersHajunAndIonioiWithImmediateUbw() throws IOException {
      String source = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/EmiyaArcherCombatHelper.java"
      ));
      assertTrue(source.contains("tryTriggerUbwOnEnemyBoundary(entity, level, now)"));
      assertTrue(source.contains("EmiyaUbwBoundaryDimension"));
      assertTrue(source.contains("ModDimensions.isHajunDimension(location)"));
      assertTrue(source.contains("ModDimensions.isIonioiHetairoiDimension(location)"));
      assertTrue(source.contains("activateUbw(entity, level, target, now)"));
   }
}

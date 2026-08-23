package net.xxxjk.TYPE_MOON_WORLD.servant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BaobhanSithDistanceRegressionTest {
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void curseAndNoblePhantasmPathsStayDistanceAgnostic() throws IOException {
      String skill = Files.readString(JAVA.resolve("servant/skill/BaobhanSithServantSkills.java"));
      assertFalse(skill.contains("context.distance() > 64.0"));
      assertFalse(skill.contains("distance >= 10.0 && distance <= 34.0"));
      assertFalse(skill.contains("distance >= 4.0 && distance <= 18.0"));
      assertFalse(skill.contains("distance >= 6.0 && distance <= 42.0"));
      assertTrue(skill.contains("validatedCurseServant(context, 4.0)"));
      assertTrue(skill.contains("validatedCurseServant(context, 6.0)"));
      assertTrue(skill.contains("validatedCurseServant(context, 12.0)"));
      assertTrue(skill.contains("validatedCurseServant(context, 14.0)"));

      String card = Files.readString(JAVA.resolve("servant/card/ServantCardBaobhanSithSkills.java"));
      assertTrue(card.contains("findCurseTarget(player, 18.0, 1.2)"));
      assertTrue(card.contains("findCurseTarget(player, 22.0, 1.6)"));
      assertTrue(card.contains("findCurseTarget(player, 38.0, 1.6)"));

      String projectile = Files.readString(JAVA.resolve("entity/GanderProjectileEntity.java"));
      assertTrue(projectile.contains("!isUnlimitedCurseProjectile() && this.tickCount > PROJECTILE_MAX_LIFETIME_TICKS"));
      assertTrue(projectile.contains("!isUnlimitedCurseProjectile() && this.distanceToSqr(owner) > PROJECTILE_MAX_OWNER_DISTANCE_SQR"));
   }

   @Test
   void trackedCurseTickUsesDedicatedTargetIndex() throws IOException {
      String service = Files.readString(JAVA.resolve("servant/baobhan/BaobhanSithCurseService.java"));
      assertTrue(service.contains("TAG_CURSE_TARGETS"));
      assertTrue(service.contains("tickTrackedCurses(LivingEntity owner)"));
      assertTrue(service.contains("for (UUID uuid : curseTargetUuids(owner))"));
      assertTrue(service.contains("trackCurseTarget(owner, target);"));
   }
}

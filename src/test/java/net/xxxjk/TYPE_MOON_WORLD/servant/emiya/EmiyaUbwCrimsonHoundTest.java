package net.xxxjk.TYPE_MOON_WORLD.servant.emiya;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EmiyaUbwCrimsonHoundTest {
   private static final Path NPC = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/EmiyaArcherCombatHelper.java");
   private static final Path CARD = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardEmiyaSkills.java");
   private static final Path HOUND = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/entity/CrimsonHoundProjectileEntity.java");

   @Test
   void npcUnlimitedBladeWorksLaunchesBrokenPhantasmCrimsonHoundsEveryTenSeconds() throws IOException {
      String source = Files.readString(NPC);

      assertTrue(source.contains("UBW_NEXT_CRIMSON_HOUND"));
      assertTrue(source.contains("UBW_CRIMSON_HOUND_INTERVAL = 10 * 20"));
      assertTrue(source.contains("launchUbwCrimsonHounds(entity, level)"));
      assertTrue(source.contains("activeUbwEnemies(entity, level)"));
      assertTrue(source.contains("for (LivingEntity target : targets)"));
      assertTrue(source.contains("new CrimsonHoundProjectileEntity(level, entity)"));
      assertTrue(source.contains("projectile.setTrackedTarget(target)"));
   }

   @Test
   void servantCardUnlimitedBladeWorksMirrorsNpcCrimsonHoundSupport() throws IOException {
      String source = Files.readString(CARD);

      assertTrue(source.contains("EMIYA_UBW_NEXT_CRIMSON_HOUND"));
      assertTrue(source.contains("EMIYA_UBW_CRIMSON_HOUND_INTERVAL = 10 * 20"));
      assertTrue(source.contains("launchServantCardUbwCrimsonHounds(player, level)"));
      assertTrue(source.contains("activeServantCardUbwEnemies(player, level)"));
      assertTrue(source.contains("isServantCardUbwEnemy(player, living)"));
      assertTrue(source.contains("for (LivingEntity target : targets)"));
      assertTrue(source.contains("new CrimsonHoundProjectileEntity(level, player)"));
      assertTrue(source.contains("projectile.setTrackedTarget(target)"));
   }

   @Test
   void crimsonHoundProjectileStillTriggersBrokenPhantasmExplosion() throws IOException {
      String source = Files.readString(HOUND);

      assertTrue(source.contains("UBWBrokenPhantasmExplosion.explode"));
      assertTrue(source.contains("triggerBrokenPhantasm"));
   }
}

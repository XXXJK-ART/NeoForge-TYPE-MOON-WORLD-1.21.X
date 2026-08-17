package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.world.entity.projectile.Projectile;
import net.xxxjk.TYPE_MOON_WORLD.entity.MacedonianSpearProjectileEntity;
import org.junit.jupiter.api.Test;

class ProjectileThreatClassifierTest {
   @Test
   void MacedonianSpearIsARealProjectileForEveryThreatSystem() throws IOException {
      assertTrue(Projectile.class.isAssignableFrom(MacedonianSpearProjectileEntity.class));

      String source = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/combat/ai/ProjectileThreatClassifier.java"
      ));
      assertTrue(source.contains("projectileLike instanceof MacedonianSpearProjectileEntity"));
      assertTrue(source.contains("entity instanceof MacedonianSpearProjectileEntity"));
   }
}

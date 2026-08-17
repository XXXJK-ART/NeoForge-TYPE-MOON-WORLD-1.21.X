package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ManaBurstJetFxTest {
   private static final Path SOURCE = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/magic/basic/ManaBurstService.java");

   @Test
   void bodyJetFxUsesReactiveExhaustDirections() throws IOException {
      String source = Files.readString(SOURCE);

      assertTrue(source.contains("JUMP_EXHAUST_DIRECTION = new Vec3(0.0, -1.0, 0.0)"));
      assertTrue(source.contains("desired.normalize().scale(-1.0)"));
      assertTrue(source.contains("spawnJetStream(level, base, horizontalExhaust, levelRank, false)"));
      assertTrue(source.contains("spawnJetStream(level, player.position().add(0.0, 0.12, 0.0), JUMP_EXHAUST_DIRECTION, levelRank, true)"));
   }

   @Test
   void bodyJetLiftCanHopButStillConsumesShortBurstTicks() throws IOException {
      String source = Files.readString(SOURCE);

      assertTrue(source.contains("JET_GROUND_LIFT = {0.32, 0.36, 0.40, 0.44, 0.48}"));
      assertTrue(source.contains("JET_AIR_LIFT = {0.050, 0.060, 0.070, 0.080, 0.090}"));
      assertTrue(source.contains("JET_UP_CAP = {0.38, 0.43, 0.48, 0.53, 0.58}"));
      assertTrue(source.contains("double lift = grounded ? JET_GROUND_LIFT[level - 1] : JET_AIR_LIFT[level - 1]"));
      assertTrue(source.contains("data.putInt(JET_TICKS_TAG, jetTicks - 1)"));
   }
}

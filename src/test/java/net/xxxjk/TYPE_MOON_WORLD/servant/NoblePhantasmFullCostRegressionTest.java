package net.xxxjk.TYPE_MOON_WORLD.servant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NoblePhantasmFullCostRegressionTest {
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void resourceServiceNoLongerAllowsHalfCostNpCasting() throws IOException {
      String source = Files.readString(JAVA.resolve("servant/skill/ServantNoblePhantasmResourceService.java"));

      assertTrue(source.contains("available + 1.0E-6 >= cost"));
      assertFalse(source.contains("cost * 0.5"));
      assertFalse(source.contains("available / cost"));
   }

   @Test
   void servantCardNpConsumptionRequiresFullCost() throws IOException {
      String source = Files.readString(JAVA.resolve("servant/card/ServantCardManaService.java"));

      assertTrue(source.contains("available + 1.0E-6 < effectiveCost"));
      assertTrue(source.contains("putDouble(NOBLE_PHANTASM_POWER_SCALE_TAG, 1.0)"));
      assertFalse(source.contains("effectiveCost * 0.5"));
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MedeaFlightArmorStateTest {
   private static final Path ROOT = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void armorAnimationUsesSyncedNpcFlightState() throws Exception {
      String source = Files.readString(ROOT.resolve("item/custom/ServantCardArmorItem.java"));

      assertTrue(source.contains("living instanceof MedeaEntity medea"));
      assertTrue(source.contains("return medea.isFlyingMode()"));
   }
}

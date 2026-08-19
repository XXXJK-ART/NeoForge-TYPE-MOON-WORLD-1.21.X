package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MasterCardRyunosukeLoadoutTest {
   @Test
   void ryunosukeHasNoMagicNoAttributesAndOnlyAnIronSword() throws IOException {
      String source = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/MasterCardProfile.java"));
      int start = source.indexOf("case \"uryu_ryunosuke\"");
      int nextProfile = source.indexOf("case \"waver\"", start);
      String profile = source.substring(start, nextProfile);

      assertTrue(profile.contains("\"default\", 10.0, 1.0, 1, Attributes.NONE"));
      assertTrue(profile.contains("give(player, new ItemStack(Items.IRON_SWORD));"));
      assertTrue(profile.contains("vars -> {"));
      assertTrue(source.contains("!\"uryu_ryunosuke\".equals(masterId)"));
   }
}

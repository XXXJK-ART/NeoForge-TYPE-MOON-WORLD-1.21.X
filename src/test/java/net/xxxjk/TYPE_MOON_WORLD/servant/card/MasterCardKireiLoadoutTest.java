package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MasterCardKireiLoadoutTest {
   @Test
   void kireiReceivesExpandedBlackKeysAndFireEngravingKnowledge() throws IOException {
      String source = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/MasterCardProfile.java"));
      int kireiStart = source.indexOf("case \"kotomine_kirei\"");
      int nextProfile = source.indexOf("case \"luvia\"", kireiStart);
      String kireiProfile = source.substring(kireiStart, nextProfile);

      assertTrue(kireiProfile.contains("learn(vars, \"black_key_fire_engraving\")"));
      assertTrue(kireiProfile.contains("magic_proficiencies.put(\"black_key_fire_engraving\", 80.0)"));
      assertTrue(kireiProfile.contains("for (int i = 0; i < 20; i++)"));
      assertTrue(kireiProfile.contains("new ItemStack(ModItems.BLACK_KEY.get(), 3)"));
      assertTrue(kireiProfile.contains("BlackKeyItem.setExpanded(blackKeys, true)"));
   }
}

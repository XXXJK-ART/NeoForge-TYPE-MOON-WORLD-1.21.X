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

      assertTrue(source.contains("learn(vars, \"black_key_fire_engraving\")"));
      assertTrue(source.contains("magic_proficiencies.put(\"black_key_fire_engraving\", 80.0)"));
      assertTrue(source.contains("for (int i = 0; i < 5; i++)"));
      assertTrue(source.contains("new ItemStack(ModItems.BLACK_KEY.get(), 3)"));
      assertTrue(source.contains("BlackKeyItem.setExpanded(blackKeys, true)"));
   }
}

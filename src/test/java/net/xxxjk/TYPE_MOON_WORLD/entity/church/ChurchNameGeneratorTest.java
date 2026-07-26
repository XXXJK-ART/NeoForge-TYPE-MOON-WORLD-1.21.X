package net.xxxjk.TYPE_MOON_WORLD.entity.church;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChurchNameGeneratorTest {
   @Test
   void churchNamesDelegateToTheMagicianChinesePool() throws IOException {
      String church = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/entity/church/ChurchNameGenerator.java"));
      String magician = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/entity/MysticMagicianEntity.java"));

      assertTrue(church.contains("MysticMagicianEntity.generateChurchNameChinese(random, female)"));
      assertTrue(magician.contains("return generated.chinese();"));
      assertFalse(church.contains("Kotomine"));
      assertFalse(church.contains("Anderson"));
   }
}

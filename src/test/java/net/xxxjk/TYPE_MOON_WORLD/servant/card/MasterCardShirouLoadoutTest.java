package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MasterCardShirouLoadoutTest {
   @Test
   void shirouReceivesUbwSwordControlMagic() throws IOException {
      String source = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/MasterCardProfile.java"));
      int shirouStart = source.indexOf("case \"emiya_shirou\"");
      int nextProfile = source.indexOf("case \"kotomine_kirei\"", shirouStart);
      String shirouProfile = source.substring(shirouStart, nextProfile);

      assertTrue(shirouProfile.contains("learn(vars, \"unlimited_blade_works\")"));
      assertTrue(shirouProfile.contains("learn(vars, \"ubw_sword_control\")"));
   }
}

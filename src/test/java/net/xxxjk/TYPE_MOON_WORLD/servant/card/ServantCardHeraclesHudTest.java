package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ServantCardHeraclesHudTest {
   private static final Path VARS = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/network/TypeMoonWorldModVariables.java");
   private static final Path HERACLES_SKILLS = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardHeraclesSkills.java");
   private static final Path HUD = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/screens/ServantCardHud.java");
   private static final Path ZH_LANG = Path.of("src/main/resources/assets/typemoonworld/lang/zh_cn.json");
   private static final Path EN_LANG = Path.of("src/main/resources/assets/typemoonworld/lang/en_us.json");

   @Test
   void heraclesGodHandLivesAreSyncedAndRenderedOnServantCardHud() throws IOException {
      String vars = Files.readString(VARS);
      String skills = Files.readString(HERACLES_SKILLS);
      String hud = Files.readString(HUD);
      String zh = Files.readString(ZH_LANG);
      String en = Files.readString(EN_LANG);

      assertTrue(vars.contains("servant_card_heracles_god_hand_lives"));
      assertTrue(vars.contains("heraclesGodHandLives"));
      assertTrue(vars.contains("Mth.clamp(message.heraclesGodHandLives, 0, 12)"));
      assertTrue(skills.contains("syncGodHandLives"));
      assertTrue(skills.contains("data.getInt(\"GodHandLives\") + 1"));
      assertTrue(skills.contains("vars.syncServantCardRuntime(player)"));
      assertTrue(hud.contains("drawHeraclesGodHandLives"));
      assertTrue(hud.contains("hud.typemoonworld.servant_card.heracles_god_hand_lives"));
      assertTrue(zh.contains("\"hud.typemoonworld.servant_card.heracles_god_hand_lives\""));
      assertTrue(en.contains("\"hud.typemoonworld.servant_card.heracles_god_hand_lives\""));
   }
}

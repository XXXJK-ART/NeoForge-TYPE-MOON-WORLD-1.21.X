package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ServantCardCombatTargetingTest {
   private static final Path JAVA_ROOT = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void npcServantsRecognizeActiveServantCardPlayersAsHostileTargets() throws IOException {
      String targeting = Files.readString(JAVA_ROOT.resolve("servant/card/ServantMasterTargeting.java"));
      String hostile = Files.readString(JAVA_ROOT.resolve("servant/ai/module/HostileTargetingModule.java"));

      assertTrue(targeting.contains("isServantCardPlayer"));
      assertTrue(targeting.contains("vars.servant_card_transformed"));
      assertTrue(hostile.contains("ServantMasterTargeting.isServantCardPlayer(target)"));
      assertTrue(hostile.contains("ServantMasterTargeting.isServantCardPlayer(other)"));
   }

   @Test
   void playerCardDamageKeepsUsingNormalPlayerAttackDamageSources() throws IOException {
      String skillUtils = Files.readString(JAVA_ROOT.resolve("servant/card/ServantCardSkillUtils.java"));
      String commonEvents = Files.readString(JAVA_ROOT.resolve("event/CommonEvents.java"));
      String targeting = Files.readString(JAVA_ROOT.resolve("servant/card/ServantMasterTargeting.java"));
      String mixin = Files.readString(JAVA_ROOT.resolve("mixin/ServerPlayerCombatMixin.java"));

      assertTrue(skillUtils.contains("player.damageSources().playerAttack(player)"));
      assertTrue(commonEvents.contains("ServantCardDefenseHandler.handleIncomingDamage(player, vars, event)"));
      assertTrue(commonEvents.contains("ServantMasterProtection.isProtectedMasterDamage(event.getSource(), event.getEntity())"));
      assertTrue(targeting.contains("canServantCardPlayersHarm"));
      assertTrue(targeting.contains("isAllowFriendlyFire"));
      assertTrue(mixin.contains("method = \"canHarmPlayer\""));
      assertTrue(mixin.contains("ServantMasterTargeting.canServantCardPlayersHarm"));
   }
}

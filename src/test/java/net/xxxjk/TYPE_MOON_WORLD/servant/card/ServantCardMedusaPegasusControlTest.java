package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ServantCardMedusaPegasusControlTest {
   private static final Path ROOT = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void servantCardBellerophonControlsPegasusInsteadOfLettingFallbackTakeOver() throws Exception {
      String source = Files.readString(ROOT.resolve("servant/card/ServantCardMedusaSkills.java"));

      assertTrue(source.contains("pegasus.markFlightControlled(now);"));
      assertTrue(source.contains("? computeMedusaPegasusVelocity(player, pegasus, now)"));
      assertTrue(source.contains(": computeMedusaControlledPegasusVelocity(player, pegasus, false)"));
   }

   @Test
   void servantCardBellerophonStartsAboveGroundAndKeepsAnUpwardLaunchBias() throws Exception {
      String source = Files.readString(ROOT.resolve("servant/card/ServantCardMedusaSkills.java"));

      assertTrue(source.contains("player.getY() + 1.0"));
      assertTrue(source.contains("player.position().add(0.0, 1.1, 0.0)"));
      assertTrue(source.contains("Math.max(0.25, launch.y * 0.35)"));
      assertTrue(source.contains("aim.y < 0.12"));
      assertTrue(source.contains("aim.y < 0.04"));
   }

   @Test
   void servantCardPegasusFlightCanClimbAndRecoversFromGroundContact() throws Exception {
      String source = Files.readString(ROOT.resolve("servant/card/ServantCardMedusaSkills.java"));

      assertTrue(source.contains("vertical = 1.0;"));
      assertTrue(source.contains("* 0.72"));
      assertTrue(source.contains("Mth.clamp(-Math.sin(player.getXRot() * Math.PI / 180.0) * Math.abs(forwardInput) * 0.72, -0.24, 0.62)"));
      assertTrue(source.contains("if (!descending && pegasus.onGround())"));
      assertTrue(source.contains("vertical = Math.max(vertical, 0.35);"));
   }

   @Test
   void servantCardPegasusCollisionUsesMotionDirectionAndLargerVerticalSweep() throws Exception {
      String source = Files.readString(ROOT.resolve("servant/card/ServantCardMedusaSkills.java"));

      assertTrue(source.contains("Vec3 forward = medusaPegasusMotionForward(pegasus);"));
      assertTrue(source.contains("pegasus.getBoundingBox().expandTowards(forward.scale(3.2)).inflate(2.3, 1.8, 2.3)"));
      assertTrue(source.contains("pegasus.getDeltaMovement().multiply(1.5, 1.0, 1.5)"));
      assertTrue(source.contains("pegasus.getBoundingBox().expandTowards(sweep).inflate(2.0, 1.6, 2.0)"));
   }

   @Test
   void pegasusEntityStillHasFallbackGuardForUncontrolledRides() throws Exception {
      String source = Files.readString(ROOT.resolve("entity/MedusaPegasusEntity.java"));

      assertTrue(source.contains("now - this.getPersistentData().getLong(TAG_FLIGHT_CONTROL_TICK) <= 2L"));
   }
}

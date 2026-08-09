package net.xxxjk.TYPE_MOON_WORLD.combat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CombatModeProtectionResourcesTest {
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void centralProtectionCoversModDamageAndTargetChanges() throws IOException {
      String events = read("event/CombatModeProtectionEvents.java");
      assertTrue(events.contains("onLivingChangeTarget(LivingChangeTargetEvent event)"));
      assertTrue(events.contains("EntityUtils.isImmunePlayerTarget(target)"));
      assertTrue(events.contains("event.setNewAboutToBeSetTarget(null)"));
      assertTrue(events.contains("onIncomingDamage(LivingIncomingDamageEvent event)"));
      assertTrue(events.contains("!isMysticEyesAttack(event)"));
      assertTrue(events.contains("event.setCanceled(true)"));
      assertTrue(events.contains("onProjectileImpact(ProjectileImpactEvent event)"));
   }

   @Test
   void servantTargetsAndDelayedExecutionsRecheckPlayerMode() throws IOException {
      String servant = read("servant/entity/ServantEntity.java");
      String cursedArm = read("servant/entity/CursedArmHassanCombatHelper.java");
      String gaeBolg = read("entity/GaeBulgProjectileEntity.java");
      assertTrue(servant.contains("super.setTarget(EntityUtils.isImmunePlayerTarget(target) ? null : target)"));
      assertTrue(cursedArm.contains("private static void resolveZabaniya"));
      assertTrue(cursedArm.contains("if (EntityUtils.isImmunePlayerTarget(target))"));
      assertTrue(gaeBolg.contains("private void applyDeathThorn"));
      assertTrue(gaeBolg.contains("private void applyGuaranteedDamage"));
   }

   @Test
   void homingEntitiesDropPlayersWhoChangeToImmuneModes() throws IOException {
      String gateWeapon = read("entity/GilgameshGateWeaponProjectileEntity.java");
      String royalCannon = read("entity/RoyalCannonProjectileEntity.java");
      String earthWeapon = read("entity/EnkiduEarthWeaponProjectileEntity.java");
      String binding = read("entity/ChainsOfHeavenBindingEntity.java");
      assertTrue(gateWeapon.contains("EntityUtils.isImmunePlayerTarget(target)"));
      assertTrue(royalCannon.contains("!EntityUtils.isImmunePlayerTarget(victim)"));
      assertTrue(earthWeapon.contains("EntityUtils.isImmunePlayerTarget(living)"));
      assertTrue(binding.contains("EntityUtils.isImmunePlayerTarget(target)"));
   }

   @Test
   void mysticEyesOfDeathPerceptionRetainsCreativeModeException() throws IOException {
      String events = read("event/CombatModeProtectionEvents.java");
      String shiki = read("entity/RyougiShikiEntity.java");
      assertTrue(events.contains("!(attacker instanceof RyougiShikiEntity)"));
      assertTrue(shiki.contains("if (target instanceof Player player && player.isCreative())"));
      assertTrue(shiki.contains("player.setHealth(0.0F)"));
   }

   private static String read(String relativePath) throws IOException {
      return Files.readString(JAVA.resolve(relativePath));
   }
}

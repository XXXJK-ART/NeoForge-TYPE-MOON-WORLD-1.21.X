package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import org.junit.jupiter.api.Test;

class ServantCombatAssessmentTest {
   @Test
   void offenseAndMagicCanRevealAnOverwhelmingOpponentAtEqualHealth() {
      double own = ServantCombatAssessment.combatPower(300, 10, 5, 10, 0.3);
      double warrior = ServantCombatAssessment.combatPower(300, 60, 5, 10, 0.3);
      double caster = ServantCombatAssessment.combatPower(300, 5, 60, 10, 0.3);
      assertTrue(warrior / own >= ServantPhaseService.OVERWHELMING_STRENGTH_RATIO);
      assertEquals(warrior, caster, 1.0E-6);
   }

   @Test
   void exposedOpponentFavorsCastingWhileAnIncomingAttackFavorsDefense() {
      var noble = action(Set.of(AiActionDescriptor.Tag.NOBLE_PHANTASM), 60);
      var guard = action(Set.of(AiActionDescriptor.Tag.GUARD), 4);
      var exposed = new ServantCombatAssessment.Opportunity(true, false, false, 1, 1);
      var threatened = new ServantCombatAssessment.Opportunity(false, false, true, 1, 1);
      assertTrue(ServantActionPlanner.timingUtility(noble, exposed, 4)
         > ServantActionPlanner.timingUtility(guard, exposed, 4));
      assertTrue(ServantActionPlanner.timingUtility(guard, threatened, 4)
         > ServantActionPlanner.timingUtility(noble, threatened, 4));
      assertFalse(threatened.favorsNoblePhantasm(4, 60));
      assertTrue(exposed.favorsNoblePhantasm(4, 60));
   }

   @Test
   void enemyChantFavorsInterruptAndCriticalHealthFavorsHealing() {
      var interrupt = action(Set.of(AiActionDescriptor.Tag.INTERRUPT), 8);
      var heal = action(Set.of(AiActionDescriptor.Tag.HEAL), 8);
      var chant = new ServantCombatAssessment.Opportunity(false, true, false, 1, 1);
      var injured = new ServantCombatAssessment.Opportunity(false, false, false, 0.3F, 1);
      var healthy = new ServantCombatAssessment.Opportunity(false, false, false, 1, 1);
      assertTrue(ServantActionPlanner.timingUtility(interrupt, chant, 8)
         > ServantActionPlanner.timingUtility(interrupt, healthy, 8));
      assertTrue(ServantActionPlanner.timingUtility(heal, injured, 8)
         > ServantActionPlanner.timingUtility(heal, healthy, 8));
   }

   private static AiActionDescriptor action(Set<AiActionDescriptor.Tag> tags, int windup) {
      return new AiActionDescriptor(ResourceLocation.fromNamespaceAndPath("typemoonworld", "test/opportunity"),
         tags, 0, 40, 0, 0, new AiActionDescriptor.Timing(windup, 1, 4),
         AiActionDescriptor.ThreatSpec.NONE, TerrainImpactProfile.Tier.NONE, AiActionDescriptor.ManeuverSpec.NONE);
   }
}

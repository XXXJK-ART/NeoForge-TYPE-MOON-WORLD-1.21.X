package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NpcMagicFilterServiceTest {
   @Test
   void entityDisplacementIsAvailableToMysticMagicians() {
      assertTrue(NpcMagicFilterService.candidateMagicPool().contains("entity_displacement"));
      assertTrue(NpcMagicFilterService.isMagicAllowedForNpc("entity_displacement"));
   }
}

package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class RuneEffectSpecTest {
   @Test
   void everyRuneHasCompleteFourPositionSpecification() {
      assertTrue(RuneRegistry.all().size() == 24);
      for (RuneDefinition definition : RuneRegistry.all()) {
         for (RunePosition position : RunePosition.values()) {
            RuneEffectSpec spec = definition.effectSpec(position);
            assertFalse(spec.name().isBlank(), definition.idPath() + ":" + position);
            assertFalse(spec.description().isBlank(), definition.idPath() + ":" + position);
            assertFalse(RuneRegistry.actionKey(definition.idPath(), position).isBlank(), definition.idPath() + ":" + position);
         }
      }
   }
}

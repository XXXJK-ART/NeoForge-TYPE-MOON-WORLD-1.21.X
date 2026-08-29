package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuneEffectDispatcherProfileTest {
   @Test
   void fixedProfilesExposeStableDirectionAndAreaLanguage() {
      var kenaz = RuneEffectDispatcher.fixedProfile("kenaz");
      assertEquals(200.0D, kenaz.length());
      assertEquals(5.0D, kenaz.radius());
      assertEquals("flame_burst", kenaz.effectId());

      var hagalaz = RuneEffectDispatcher.fixedProfile("hagalaz");
      assertEquals(10.0D, hagalaz.radius());
      assertTrue(hagalaz.color() != kenaz.color());

      var sowilo = RuneEffectDispatcher.fixedProfile("sowilo");
      assertEquals(50.0D, sowilo.length());
      assertEquals(0.75D, sowilo.radius());
   }

   @Test
   void fixedProfilesCoverAllNamedRunes() {
      for (String id : new String[] {"kenaz", "hagalaz", "thurisaz", "sowilo", "nauthiz", "isa", "algiz", "berkano", "raidho", "uruz"}) {
         assertTrue(!RuneEffectDispatcher.fixedProfile(id).effectId().isBlank(), id);
      }
   }
}

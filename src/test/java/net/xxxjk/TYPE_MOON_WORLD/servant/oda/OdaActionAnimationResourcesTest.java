package net.xxxjk.TYPE_MOON_WORLD.servant.oda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.minecraftApi.codec.AnimationCodecs;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import org.junit.jupiter.api.Test;

class OdaActionAnimationResourcesTest {
   @Test
   void playerAnimatorCanLoadEachOdaActionAnimation() throws IOException {
      for (OdaNobunagaActionAnimations.Action action : OdaNobunagaActionAnimations.Action.values()) {
         String path = "/assets/typemoonworld/player_animations/" + action.playerAnimationPath() + ".json";
         try (InputStream resource = getClass().getResourceAsStream(path)) {
            assertNotNull(resource, path);
            Collection<IPlayable> animations = AnimationCodecs.deserialize("json", resource);
            assertFalse(animations.isEmpty(), path);
            assertEquals(action.playerAnimationPath(), animations.iterator().next().getName().replace("\"", ""), path);
         }
      }
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.muramasa;

import static org.junit.jupiter.api.Assertions.*;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.codec.AnimationCodecs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MuramasaSwordTechnique;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;
import org.junit.jupiter.api.Test;

class MuramasaSwordAnimationTest {
   @Test void swordTechniquesDecodeAndAnimateWithoutOverridingWalkingOrHeadLook() throws Exception {
      var ids = new HashSet<Integer>();
      for (var technique : MuramasaSwordTechnique.values()) {
         int action = technique.animationAction();
         assertTrue(ids.add(action), "Techniques must have distinct presentation IDs");
         assertTrue(ServantBaseAction.isAttack(action));
         assertFalse(ServantBaseAction.isContinuous(action));
         String name = ServantBaseAction.animationId(action, 0);
         try (var stream = Files.newInputStream(Path.of("src/main/resources/assets/typemoonworld/player_animations", name + ".json"))) {
            var clip = (KeyframeAnimation)AnimationCodecs.deserialize("json", stream).iterator().next();
            assertFalse(clip.isInfinite, name);
            Vec3f first = null;
            boolean moved = false;
            for (int tick = 0; tick < clip.endTick; tick++) {
               var sample = new KeyframeAnimationPlayer(clip, tick);
               sample.setupAnim(0.5F);
               Vec3f arm = sample.get3DTransform("rightArm", TransformType.ROTATION, 0.5F, Vec3f.ZERO);
               if (first == null) first = arm;
               moved |= Math.abs(arm.getX() - first.getX()) + Math.abs(arm.getY() - first.getY()) > 0.1F;
               for (String bone : new String[]{"head", "leftLeg", "rightLeg"}) {
                  Vec3f base = new Vec3f(0.24F, -0.35F, 0.46F);
                  Vec3f result = sample.get3DTransform(bone, TransformType.ROTATION, 0.5F, base);
                  assertEquals(base.getX(), result.getX(), 0.0001F, name + ":" + bone);
                  assertEquals(base.getY(), result.getY(), 0.0001F, name + ":" + bone);
                  assertEquals(base.getZ(), result.getZ(), 0.0001F, name + ":" + bone);
               }
               Vec3f root = sample.get3DTransform("body", TransformType.POSITION, 0.5F, Vec3f.ZERO);
               assertEquals(0, root.getX(), 0.0001F, name);
               assertEquals(0, root.getY(), 0.0001F, name);
               assertEquals(0, root.getZ(), 0.0001F, name);
            }
            assertTrue(moved, name + " contains no sword motion");
         }
      }
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SasakiTsubameSlashRendererTest {
   @Test
   void threeCurvedRibbonsAdvanceThroughDepthAndMeetAtOneForwardTip() {
      Vec3 swordTipAtStart = new Vec3(0.35, 1.25, -0.75);
      Vec3 sharedEnd = null;
      Vec3[] starts = new Vec3[3];

      for (int blade = 1; blade <= 3; blade++) {
         List<Vec3> path = SasakiTsubameSlashRenderer.trailPath(blade, 11.0F, swordTipAtStart);
         assertEquals(49, path.size());
         starts[blade - 1] = path.getFirst();
         Vec3 end = path.getLast();
         if (sharedEnd == null) sharedEnd = end;
         else assertTrue(end.distanceTo(sharedEnd) < 1.0E-9);

         assertTrue(end.z < path.getFirst().z - 7.0, "Each stroke must travel forward through depth");
         assertTrue(path.get(24).distanceTo(path.getFirst()) > 2.0, "Each stroke must bend away from its start");
         for (int i = 1; i < path.size(); i++) {
            assertTrue(path.get(i).z < path.get(i - 1).z, "Each stroke must keep advancing forward");
         }
      }

      assertTrue(starts[0].distanceTo(starts[1]) > 0.5);
      assertTrue(starts[1].distanceTo(starts[2]) > 0.5);
      assertTrue(starts[2].distanceTo(starts[0]) > 0.5);
   }
}

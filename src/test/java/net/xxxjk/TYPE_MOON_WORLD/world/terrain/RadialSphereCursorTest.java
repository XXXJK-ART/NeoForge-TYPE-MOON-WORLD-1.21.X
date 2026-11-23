package net.xxxjk.TYPE_MOON_WORLD.world.terrain;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadialSphereCursorTest {
   @Test
   void visitsEverySphereVoxelExactlyOnceInNondecreasingDistance() {
      for (int radius : new int[]{0, 1, 2, 5, 18}) {
         var cursor = new RadialSphereCursor(radius);
         var visited = new HashSet<RadialSphereCursor.Offset>();
         long previousDistance = -1;
         while (cursor.hasNext()) {
            var offset = cursor.next();
            assertTrue(offset.distanceSqr() >= previousDistance, "Blast swept past an uncleared inner voxel");
            assertTrue(offset.distanceSqr() <= (long)radius * radius, "Blast exceeded its radius");
            assertTrue(visited.add(offset), "Blast revisited a voxel");
            previousDistance = offset.distanceSqr();
         }
         int expected = 0;
         for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
               for (int z = -radius; z <= radius; z++) {
                  var offset = new RadialSphereCursor.Offset(x, y, z);
                  if (offset.distanceSqr() <= (long)radius * radius) {
                     assertTrue(visited.contains(offset), "Blast left a hole in its sphere");
                     expected++;
                  }
               }
            }
         }
         assertEquals(expected, visited.size());
         assertThrows(java.util.NoSuchElementException.class, cursor::next);
      }
   }

   @Test
   void clearsCenterThenAllSixNeighborsBeforeDiagonals() {
      var cursor = new RadialSphereCursor(18);
      assertEquals(new RadialSphereCursor.Offset(0, 0, 0), cursor.next());
      for (int i = 0; i < 6; i++) assertEquals(1, cursor.next().distanceSqr());
      assertEquals(2, cursor.next().distanceSqr());
   }
}

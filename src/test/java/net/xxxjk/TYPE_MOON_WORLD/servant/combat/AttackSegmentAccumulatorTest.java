package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttackSegmentAccumulatorTest {
   @Test void maximumDoesNotDependOnTargetOrder() {
      for (boolean masterFirst : new boolean[]{false, true}) {
         var hits = new AttackSegmentAccumulator<String, Integer>();
         if (masterFirst) hits.add("servant", "master", 30, 30);
         hits.add("servant", "servant", 40, 40);
         if (!masterFirst) hits.add("servant", "master", 30, 30);
         assertEquals(List.of(40), hits.values());
      }
   }
   @Test void distinctSegmentsNeverMerge() {
      var first = new AttackSegmentAccumulator<String, Integer>();
      var second = new AttackSegmentAccumulator<String, Integer>();
      first.add("s", "m", 40, 40); second.add("s", "m", 30, 30);
      assertEquals(List.of(40), first.values()); assertEquals(List.of(30), second.values());
   }
   @Test void repeatedDamageComponentsRemainIndependent() {
      var hits = new AttackSegmentAccumulator<String, Integer>();
      hits.add("s", "m", 30, 30); hits.add("s", "m", 15, 15);
      hits.add("s", "s", 40, 40); hits.add("s", "s", 10, 10);
      assertEquals(List.of(40, 15), hits.values());
   }
   @Test void differentPairsNeverMerge() {
      var hits = new AttackSegmentAccumulator<String, Integer>();
      hits.add("first", "m1", 30, 30); hits.add("second", "m2", 40, 40);
      assertEquals(List.of(30, 40), hits.values());
   }
}

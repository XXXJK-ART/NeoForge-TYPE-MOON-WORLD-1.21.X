package net.xxxjk.TYPE_MOON_WORLD.servant.iskandar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;
import org.junit.jupiter.api.Test;

class IonioiHetairoiRankPoolTest {
   @Test
   void keepsExactTotalRankQuotas() {
      IonioiHetairoiRankPool pool = IonioiHetairoiRankPool.create(12345L);
      Map<StatRank, Integer> counts = pool.counts();

      assertEquals(IonioiHetairoiRankPool.TOTAL_SIZE, pool.size());
      assertEquals(10, counts.get(StatRank.A));
      assertEquals(100, counts.get(StatRank.B));
      assertEquals(1000, counts.get(StatRank.C));
      assertEquals(3000, counts.get(StatRank.D));
      assertEquals(5890, counts.get(StatRank.E));
   }

   @Test
   void everyWaveContainsTwoHundredRanks() {
      IonioiHetairoiRankPool pool = IonioiHetairoiRankPool.create(67890L);

      for (int wave = 0; wave < IonioiHetairoiRankPool.WAVE_COUNT; wave++) {
         int start = wave * IonioiHetairoiRankPool.WAVE_SIZE;
         int end = start + IonioiHetairoiRankPool.WAVE_SIZE;
         int present = 0;
         for (int index = start; index < end; index++) {
            if (pool.rankAt(index) != null) {
               present++;
            }
         }
         assertEquals(IonioiHetairoiRankPool.WAVE_SIZE, present);
      }
   }

   @Test
   void seedChangesWaveOrderButNotQuotas() {
      IonioiHetairoiRankPool first = IonioiHetairoiRankPool.create(1L);
      IonioiHetairoiRankPool second = IonioiHetairoiRankPool.create(2L);

      assertEquals(first.counts(), second.counts());
      StringBuilder firstHead = new StringBuilder();
      StringBuilder secondHead = new StringBuilder();
      for (int i = 0; i < IonioiHetairoiRankPool.WAVE_SIZE; i++) {
         firstHead.append(first.rankAt(i).name());
         secondHead.append(second.rankAt(i).name());
      }
      assertNotEquals(firstHead.toString(), secondHead.toString());
   }
}

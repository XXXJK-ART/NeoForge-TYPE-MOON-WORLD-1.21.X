package net.xxxjk.TYPE_MOON_WORLD.servant.iskandar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;

public final class IonioiHetairoiRankPool {
   public static final int TOTAL_SIZE = 10_000;
   public static final int WAVE_COUNT = 50;
   public static final int WAVE_SIZE = 200;
   public static final int A_COUNT = 10;
   public static final int B_COUNT = 100;
   public static final int C_COUNT = 1_000;
   public static final int D_COUNT = 3_000;
   public static final int E_COUNT = 5_890;

   private final long seed;
   private final List<StatRank> ranks;

   private IonioiHetairoiRankPool(long seed, List<StatRank> ranks) {
      this.seed = seed;
      this.ranks = List.copyOf(ranks);
   }

   public static IonioiHetairoiRankPool create(long seed) {
      Random random = new Random(seed);
      List<List<StatRank>> waves = new ArrayList<>(WAVE_COUNT);
      for (int i = 0; i < WAVE_COUNT; i++) {
         waves.add(new ArrayList<>(WAVE_SIZE));
      }

      Set<Integer> aWaves = new HashSet<>();
      while (aWaves.size() < A_COUNT) {
         aWaves.add(random.nextInt(WAVE_COUNT));
      }
      for (int wave : aWaves) {
         waves.get(wave).add(StatRank.A);
      }

      distribute(waves, StatRank.B, B_COUNT);
      distribute(waves, StatRank.C, C_COUNT);
      distribute(waves, StatRank.D, D_COUNT);
      distribute(waves, StatRank.E, E_COUNT);

      List<StatRank> result = new ArrayList<>(TOTAL_SIZE);
      for (List<StatRank> wave : waves) {
         Collections.shuffle(wave, random);
         result.addAll(wave);
      }
      return new IonioiHetairoiRankPool(seed, result);
   }

   private static void distribute(List<List<StatRank>> waves, StatRank rank, int count) {
      int base = count / WAVE_COUNT;
      int remainder = count % WAVE_COUNT;
      for (int wave = 0; wave < WAVE_COUNT; wave++) {
         int amount = base + (wave < remainder ? 1 : 0);
         for (int i = 0; i < amount; i++) {
            waves.get(wave).add(rank);
         }
      }
   }

   public long seed() {
      return this.seed;
   }

   public StatRank rankAt(int index) {
      if (index < 0 || index >= this.ranks.size()) {
         return StatRank.E;
      }
      return this.ranks.get(index);
   }

   public int size() {
      return this.ranks.size();
   }

   public Map<StatRank, Integer> counts() {
      Map<StatRank, Integer> counts = new EnumMap<>(StatRank.class);
      for (StatRank rank : StatRank.values()) {
         counts.put(rank, 0);
      }
      for (StatRank rank : this.ranks) {
         counts.put(rank, counts.get(rank) + 1);
      }
      return counts;
   }
}

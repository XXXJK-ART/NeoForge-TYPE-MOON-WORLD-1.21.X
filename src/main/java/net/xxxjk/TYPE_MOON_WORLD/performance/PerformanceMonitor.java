package net.xxxjk.TYPE_MOON_WORLD.performance;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

/**
 * Low-overhead server load signal used only to budget deferrable work.
 * It deliberately does not alter gameplay decisions or damage processing.
 */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class PerformanceMonitor {
   public enum Pressure { NORMAL, PRESSURED, CRITICAL }

   private static final int WINDOW_SIZE = 1_200;
   private static final AtomicLong LAST_TICK_START = new AtomicLong();
   private static final long[] RECENT_TICK_NANOS = new long[WINDOW_SIZE];
   private static volatile double ewmaMillis;
   private static volatile long sampleCount;
   private static int recentIndex;
   private static int recentCount;
   private static long normalTicks;
   private static long pressuredTicks;
   private static long criticalTicks;
   private static long playerFullSyncAttempts;
   private static long playerFullSyncChanges;
   private static long playerFullSyncNanos;

   private PerformanceMonitor() { }

   @SubscribeEvent
   public static void onServerTickPre(ServerTickEvent.Pre event) {
      LAST_TICK_START.set(System.nanoTime());
   }

   @SubscribeEvent
   public static synchronized void onServerTickPost(ServerTickEvent.Post event) {
      long started = LAST_TICK_START.getAndSet(0L);
      if (started == 0L) return;
      long elapsedNanos = Math.max(0L, System.nanoTime() - started);
      double millis = elapsedNanos / 1_000_000.0;
      double previous = ewmaMillis;
      ewmaMillis = previous <= 0.0 ? millis : previous * 0.9 + millis * 0.1;
      RECENT_TICK_NANOS[recentIndex] = elapsedNanos;
      recentIndex = (recentIndex + 1) % WINDOW_SIZE;
      recentCount = Math.min(WINDOW_SIZE, recentCount + 1);
      sampleCount++;
      switch (pressure()) {
         case NORMAL -> normalTicks++;
         case PRESSURED -> pressuredTicks++;
         case CRITICAL -> criticalTicks++;
      }
   }

   public static double ewmaMillis() {
      return ewmaMillis;
   }

   public static long sampleCount() {
      return sampleCount;
   }

   public static Pressure pressure() {
      if (!Config.adaptivePerformance) return Pressure.NORMAL;
      return classify(ewmaMillis, Config.serverPressureMspt, Config.serverCriticalMspt);
   }

   public static Pressure classify(double mspt, int pressureMspt, int criticalMspt) {
      int critical = Math.max(pressureMspt, criticalMspt);
      if (mspt >= critical) return Pressure.CRITICAL;
      if (mspt >= pressureMspt) return Pressure.PRESSURED;
      return Pressure.NORMAL;
   }

   /** Fraction of the normal budget that deferrable work may consume. */
   public static double backgroundBudgetScale() {
      return scaleFor(pressure());
   }

   public static double scaleFor(Pressure pressure) {
      return switch (pressure) {
         case NORMAL -> 1.0;
         case PRESSURED -> 0.5;
         case CRITICAL -> 0.25;
      };
   }

   public static synchronized void recordPlayerFullSync(long elapsedNanos, boolean snapshotChanged) {
      playerFullSyncAttempts++;
      if (snapshotChanged) playerFullSyncChanges++;
      playerFullSyncNanos += Math.max(0L, elapsedNanos);
   }

   /** Builds a diagnostic snapshot on demand; the tick hot path performs no allocations. */
   public static synchronized Snapshot snapshot() {
      long[] sorted = Arrays.copyOf(RECENT_TICK_NANOS, recentCount);
      Arrays.sort(sorted);
      long total = 0L;
      long maximum = 0L;
      for (long nanos : sorted) {
         total += nanos;
         maximum = Math.max(maximum, nanos);
      }
      int p95Index = sorted.length == 0 ? 0 : Math.min(sorted.length - 1, (int)Math.ceil(sorted.length * 0.95) - 1);
      double averageMillis = sorted.length == 0 ? 0.0 : total / (double)sorted.length / 1_000_000.0;
      double maxMillis = maximum / 1_000_000.0;
      double p95Millis = sorted.length == 0 ? 0.0 : sorted[p95Index] / 1_000_000.0;
      return new Snapshot(ewmaMillis, averageMillis, maxMillis, p95Millis, sampleCount, recentCount,
         normalTicks, pressuredTicks, criticalTicks, playerFullSyncAttempts, playerFullSyncChanges,
         playerFullSyncNanos / 1_000_000.0);
   }

   public static synchronized void reset() {
      Arrays.fill(RECENT_TICK_NANOS, 0L);
      ewmaMillis = 0.0;
      sampleCount = 0L;
      recentIndex = 0;
      recentCount = 0;
      normalTicks = 0L;
      pressuredTicks = 0L;
      criticalTicks = 0L;
      playerFullSyncAttempts = 0L;
      playerFullSyncChanges = 0L;
      playerFullSyncNanos = 0L;
   }

   public record Snapshot(double ewmaMillis, double averageMillis, double maxMillis, double p95Millis,
                          long samples, int windowSamples, long normalTicks, long pressuredTicks,
                          long criticalTicks, long playerFullSyncAttempts, long playerFullSyncChanges,
                          double playerFullSyncMillis) { }
}

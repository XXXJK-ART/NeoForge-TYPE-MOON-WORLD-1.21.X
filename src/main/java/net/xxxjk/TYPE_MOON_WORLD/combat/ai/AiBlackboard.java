package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactType;

public final class AiBlackboard {
   public static final int MAX_OPPONENTS = 8;
   public static final int MAX_ACTIONS_PER_OPPONENT = 8;
   public static final int MAX_FACTS_PER_OPPONENT = 24;
   public static final long MEMORY_TICKS = 600L;

   private final LinkedHashMap<UUID, OpponentMemory> opponents = new LinkedHashMap<>(16, 0.75F, true);
   private ResourceLocation committedIntent;
   private int committedPriority;
   private long committedUntil;
   private boolean committedInterruptible = true;
   private long lastResolvedTick = Long.MIN_VALUE;

   public void beginTick(long now) {
      opponents.values().removeIf(memory -> now - memory.lastSeenTick > MEMORY_TICKS);
      if (now > committedUntil) clearCommitment();
   }

   public void observe(UUID opponent, ResourceLocation action, double distance, double damage,
                       boolean defended, long now) {
      if (opponent == null) return;
      OpponentMemory memory = opponents.computeIfAbsent(opponent, ignored -> new OpponentMemory());
      memory.lastSeenTick = now;
      memory.lastDistance = Math.max(0.0, distance);
      memory.totalObservedDamage += Math.max(0.0, damage);
      if (defended) memory.defendedActions++;
      if (action != null) {
         memory.actions.addLast(new ActionSample(action, now, distance, damage, defended));
         while (memory.actions.size() > MAX_ACTIONS_PER_OPPONENT) memory.actions.removeFirst();
      }
      trimOpponents();
   }

   public OpponentSnapshot opponent(UUID id) {
      OpponentMemory memory = opponents.get(id);
      if (memory == null) return OpponentSnapshot.EMPTY;
      return new OpponentSnapshot(memory.lastSeenTick, memory.lastDistance, memory.totalObservedDamage,
         memory.defendedActions, memory.actions.size(), memory.repeatCount(), memory.knownFacts);
   }

   public void revealFact(UUID opponent, FactType type, double strength, long now) {
      if (opponent == null || type == null || type == FactType.UNKNOWN) return;
      OpponentMemory memory = opponents.computeIfAbsent(opponent, ignored -> new OpponentMemory());
      memory.lastSeenTick = now;
      memory.knownFacts.remove(type);
      memory.knownFacts.put(type, Math.max(0.0, Math.min(1.0, strength)));
      while (memory.knownFacts.size() > MAX_FACTS_PER_OPPONENT) {
         Iterator<FactType> iterator = memory.knownFacts.keySet().iterator();
         if (!iterator.hasNext()) break;
         iterator.next();
         iterator.remove();
      }
      trimOpponents();
   }

   public double knownFactStrength(UUID opponent, FactType type) {
      OpponentMemory memory = opponents.get(opponent);
      return memory == null || type == null ? 0.0 : memory.knownFacts.getOrDefault(type, 0.0);
   }

   boolean commitmentBlocks(AiIntent candidate, long now) {
      return committedIntent != null && now <= committedUntil
         && !committedIntent.equals(candidate.id())
         && (!committedInterruptible || candidate.priority() <= committedPriority);
   }

   void commit(AiIntent intent, long now) {
      this.committedIntent = intent.id();
      this.committedPriority = intent.priority();
      this.committedUntil = now + intent.commitmentTicks();
      this.committedInterruptible = intent.interruptible();
      this.lastResolvedTick = now;
   }

   public boolean hasActiveCommitment(long now) {
      return committedIntent != null && now <= committedUntil;
   }

   public ResourceLocation committedIntent() {
      return committedIntent;
   }

   public long lastResolvedTick() {
      return lastResolvedTick;
   }

   private void clearCommitment() {
      committedIntent = null;
      committedPriority = 0;
      committedUntil = 0L;
      committedInterruptible = true;
   }

   private void trimOpponents() {
      while (opponents.size() > MAX_OPPONENTS) {
         Iterator<Map.Entry<UUID, OpponentMemory>> iterator = opponents.entrySet().iterator();
         if (!iterator.hasNext()) return;
         iterator.next();
         iterator.remove();
      }
   }

   private static final class OpponentMemory {
      long lastSeenTick;
      double lastDistance;
      double totalObservedDamage;
      int defendedActions;
      final Deque<ActionSample> actions = new ArrayDeque<>();
      final LinkedHashMap<FactType, Double> knownFacts = new LinkedHashMap<>(32, 0.75F, true);

      int repeatCount() {
         ResourceLocation last = null;
         int repeats = 0;
         Iterator<ActionSample> iterator = actions.descendingIterator();
         while (iterator.hasNext()) {
            ResourceLocation current = iterator.next().action();
            if (last == null) last = current;
            if (!last.equals(current)) break;
            repeats++;
         }
         return repeats;
      }
   }

   private record ActionSample(ResourceLocation action, long tick, double distance, double damage, boolean defended) { }

   public record OpponentSnapshot(long lastSeenTick, double lastDistance, double observedDamage,
                                  int defendedActions, int observedActions, int repeatCount,
                                  Map<FactType, Double> knownFacts) {
      public static final OpponentSnapshot EMPTY = new OpponentSnapshot(0L, 0.0, 0.0, 0, 0, 0, Map.of());

      public OpponentSnapshot(long lastSeenTick, double lastDistance, double observedDamage,
                              int defendedActions, int observedActions, int repeatCount) {
         this(lastSeenTick, lastDistance, observedDamage, defendedActions, observedActions, repeatCount, Map.of());
      }

      public OpponentSnapshot {
         knownFacts = knownFacts == null ? Map.of() : Map.copyOf(knownFacts);
      }

      public double knownFactStrength(FactType type) {
         return type == null ? 0.0 : knownFacts.getOrDefault(type, 0.0);
      }

      public boolean knows(FactType type) {
         return knownFactStrength(type) > 0.0;
      }
   }
}

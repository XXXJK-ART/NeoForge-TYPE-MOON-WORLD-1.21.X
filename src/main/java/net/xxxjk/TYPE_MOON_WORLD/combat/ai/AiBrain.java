package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Mob;

/** Server-thread intent collector and single-primary-action arbiter. */
public final class AiBrain {
   private static final Map<Mob, AiBlackboard> BLACKBOARDS = new WeakHashMap<>();
   private final Mob entity;
   private final AiBlackboard blackboard;
   private final List<AiIntent> intents = new ArrayList<>();
   private final long now;

   private AiBrain(Mob entity) {
      this.entity = entity;
      this.blackboard = blackboard(entity);
      this.now = entity.level().getGameTime();
      this.blackboard.beginTick(now);
   }

   public static AiBrain begin(Mob entity) {
      return new AiBrain(entity);
   }

   public static AiBlackboard blackboard(Mob entity) {
      synchronized (BLACKBOARDS) {
         return BLACKBOARDS.computeIfAbsent(entity, ignored -> new AiBlackboard());
      }
   }

   public AiBrain submit(AiIntent intent) {
      if (intent != null) intents.add(intent);
      return this;
   }

   public Resolution resolve() {
      return resolve(intents, blackboard, now);
   }

   static Resolution resolve(List<AiIntent> intents, AiBlackboard blackboard, long now) {
      List<AiIntent> remaining = new ArrayList<>(intents);
      while (!remaining.isEmpty()) {
         AiIntentArbitrator.Selection selection = AiIntentArbitrator.select(remaining,
            intent -> blackboard.commitmentBlocks(intent, now));
         if (!selection.hasPrimary()) break;
         if (!selection.primary().executor().getAsBoolean()) {
            remaining.remove(selection.primary());
            continue;
         }
         List<AiIntent> executedAuxiliaries = new ArrayList<>();
         for (AiIntent auxiliary : selection.auxiliaries()) {
            if (auxiliary.executor().getAsBoolean()) executedAuxiliaries.add(auxiliary);
         }
         blackboard.commit(selection.primary(), now);
         return new Resolution(true, selection.primary(), List.copyOf(executedAuxiliaries), false);
      }
      return new Resolution(false, null, List.of(), blackboard.hasActiveCommitment(now));
   }

   public Mob entity() {
      return entity;
   }

   public AiBlackboard blackboard() {
      return blackboard;
   }

   public record Resolution(boolean executed, AiIntent intent, List<AiIntent> auxiliaries, boolean committed) {
      public boolean consumesLegacyControl() {
         return executed || committed;
      }
   }
}

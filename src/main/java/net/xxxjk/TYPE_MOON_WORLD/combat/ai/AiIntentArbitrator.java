package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Pure intent selection used by the server brain and unit tests. */
public final class AiIntentArbitrator {
   private static final Comparator<AiIntent> ORDER = Comparator.comparingInt(AiIntent::priority).reversed()
      .thenComparing(Comparator.comparingDouble(AiIntent::utility).reversed())
      .thenComparing(intent -> intent.id().toString());

   private AiIntentArbitrator() { }

   public static Selection select(List<AiIntent> submitted, Predicate<AiIntent> blocked) {
      List<AiIntent> ordered = submitted.stream().sorted(ORDER).filter(blocked.negate()).toList();
      if (ordered.isEmpty()) return Selection.EMPTY;

      AiIntent primary = ordered.getFirst();
      Set<AiControl> occupied = primary.controls().isEmpty()
         ? EnumSet.noneOf(AiControl.class)
         : EnumSet.copyOf(primary.controls());
      List<AiIntent> auxiliaries = new ArrayList<>();
      boolean usedControlFreeSlot = false;
      for (int index = 1; index < ordered.size(); index++) {
         AiIntent candidate = ordered.get(index);
         if (candidate.controls().isEmpty()) {
            if (usedControlFreeSlot) continue;
            usedControlFreeSlot = true;
         } else if (!java.util.Collections.disjoint(occupied, candidate.controls())) {
            continue;
         }
         auxiliaries.add(candidate);
         occupied.addAll(candidate.controls());
      }
      return new Selection(primary, List.copyOf(auxiliaries));
   }

   public record Selection(AiIntent primary, List<AiIntent> auxiliaries) {
      private static final Selection EMPTY = new Selection(null, List.of());

      public boolean hasPrimary() {
         return primary != null;
      }
   }
}

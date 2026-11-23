package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/** One accumulator belongs to exactly one attack segment, never to a server tick or skill id. */
public final class AttackSegmentAccumulator<K, V> {
   private record Candidate<K>(K recipient, K original) {}
   private record Slot<K>(K recipient, int ordinal) {}
   private record Hit<V>(float amount, V value) {}
   private final Map<Candidate<K>, Integer> ordinals = new LinkedHashMap<>();
   private final Map<Slot<K>, Hit<V>> hits = new LinkedHashMap<>();

   public void add(K recipient, K original, float amount, V value) {
      var candidate = new Candidate<>(recipient, original);
      int ordinal = ordinals.merge(candidate, 1, Integer::sum) - 1;
      var slot = new Slot<>(recipient, ordinal);
      Hit<V> previous = hits.get(slot);
      if (previous == null || amount > previous.amount()) hits.put(slot, new Hit<>(amount, value));
   }
   public List<V> values() { return hits.values().stream().map(Hit::value).toList(); }
}

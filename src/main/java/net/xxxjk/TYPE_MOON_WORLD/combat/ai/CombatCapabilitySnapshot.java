package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.Map;
import java.util.Set;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactType;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactBypass;

/** Immutable active combat facts for one entity at one decision point. */
public record CombatCapabilitySnapshot(Map<FactType, Double> facts, Map<FactType, Set<FactBypass>> bypasses) {
   public static final CombatCapabilitySnapshot EMPTY = new CombatCapabilitySnapshot(Map.of(), Map.of());

   public CombatCapabilitySnapshot(Map<FactType, Double> facts) {
      this(facts, Map.of());
   }

   public CombatCapabilitySnapshot {
      facts = facts == null ? Map.of() : Map.copyOf(facts);
      if (bypasses == null || bypasses.isEmpty()) {
         bypasses = Map.of();
      } else {
         java.util.EnumMap<FactType, Set<FactBypass>> copied = new java.util.EnumMap<>(FactType.class);
         bypasses.forEach((type, values) -> copied.put(type, values == null ? Set.of() : Set.copyOf(values)));
         bypasses = Map.copyOf(copied);
      }
   }

   public double strength(FactType type) {
      return type == null ? 0.0 : facts.getOrDefault(type, 0.0);
   }

   public boolean has(FactType type) {
      return strength(type) > 0.0;
   }

   public boolean isBypassedBy(FactType type, FactBypass bypass) {
      return type != null && bypass != null && bypasses.getOrDefault(type, Set.of()).contains(bypass);
   }
}

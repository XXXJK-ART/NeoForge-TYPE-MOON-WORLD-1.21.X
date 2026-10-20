package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.Map;
import java.util.WeakHashMap;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Weak container keys do not confuse same-tick hits or retain canceled events. */
public final class DamageContexts {
   private DamageContexts() {}
   public record Hit(AttackOrigin origin, DamageParts parts) {}
   private static final Map<DamageContainer, Hit> HITS = new WeakHashMap<>();
   public static void put(LivingIncomingDamageEvent event, DamageParts parts) { HITS.put(event.getContainer(), new Hit(AttackOrigin.of(event.getSource()), parts)); }
   public static Hit capture(LivingIncomingDamageEvent event) {
      Hit old = HITS.get(event.getContainer());
      if (old == null) { old = new Hit(AttackOrigin.of(event.getSource()), new DamageParts(event.getAmount(), 0)); }
      else if (old.parts().total() > 0) {
         float ratio = event.getAmount() / old.parts().total();
         old = new Hit(old.origin(), new DamageParts(old.parts().normal() * ratio, old.parts().antiDivine() * ratio));
      }
      HITS.put(event.getContainer(), old);
      return old;
   }
   public static Hit get(DamageContainer container) { return HITS.get(container); }
}

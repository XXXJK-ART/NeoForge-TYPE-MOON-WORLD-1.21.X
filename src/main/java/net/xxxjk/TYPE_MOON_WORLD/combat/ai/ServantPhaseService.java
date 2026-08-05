package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class ServantPhaseService {
   /** Long engagements eventually force the third, decisive phase even at full health. */
   public static final long PROLONGED_COMBAT_DECISIVE_TICKS = 3000L;
   private static final String TAG_PHASE = "TypeMoonAiCombatPhase";
   private static final String TAG_PHASE_CHANGED = "TypeMoonAiPhaseChangedAt";
   private static final String TAG_COMBAT_STARTED = "TypeMoonAiCombatStartedAt";
   private static final String TAG_LAST_COMBAT = "TypeMoonAiLastCombatAt";

   private ServantPhaseService() { }

   public static Update tick(ServantEntity entity, LivingEntity target, ServantActionProfile profile, long now) {
      var data = entity.getPersistentData();
      ServantCombatPhase current = ServantCombatPhase.byName(data.getString(TAG_PHASE));
      ServantCombatPhase desired = ServantCombatPhase.PROBING;
      if (target == null || !target.isAlive()) {
         if (now - data.getLong(TAG_LAST_COMBAT) < 120L) desired = current;
         else data.remove(TAG_COMBAT_STARTED);
      } else {
         long lastCombat = data.getLong(TAG_LAST_COMBAT);
         if (!data.contains(TAG_COMBAT_STARTED) || now - lastCombat > 120L) data.putLong(TAG_COMBAT_STARTED, now);
         data.putLong(TAG_LAST_COMBAT, now);
         long elapsed = now - data.getLong(TAG_COMBAT_STARTED);
         float healthRatio = entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
         desired = desiredPhase(healthRatio, elapsed, target.getMaxHealth() / Math.max(1.0F, entity.getMaxHealth()));
         desired = applyRivalMinimum(desired, target, profile);
      }

      boolean canDowngrade = now - data.getLong(TAG_PHASE_CHANGED) >= 100L;
      boolean changed = desired.ordinal() > current.ordinal() || desired.ordinal() < current.ordinal() && canDowngrade;
      if (changed) {
         current = desired;
         data.putString(TAG_PHASE, current.name());
         data.putLong(TAG_PHASE_CHANGED, now);
      } else if (!data.contains(TAG_PHASE)) {
         data.putString(TAG_PHASE, current.name());
      }
      return new Update(current, changed);
   }

   public static ServantCombatPhase desiredPhase(float healthRatio, long combatTicks, float targetStrengthRatio) {
      if (healthRatio <= 0.25F) return ServantCombatPhase.LAST_STAND;
      if (healthRatio <= 0.55F || targetStrengthRatio > 1.35F || combatTicks >= PROLONGED_COMBAT_DECISIVE_TICKS) {
         return ServantCombatPhase.DECISIVE;
      }
      if (combatTicks >= 60L) return ServantCombatPhase.NORMAL;
      return ServantCombatPhase.PROBING;
   }

   private static ServantCombatPhase applyRivalMinimum(ServantCombatPhase phase, LivingEntity target, ServantActionProfile profile) {
      if (!(target instanceof ServantEntity rival) || profile == null) return phase;
      for (ServantActionProfile.RivalRule rule : profile.rivals()) {
         if (rule.opponentServant().equals(rival.getServantId()) && rule.minimumPhase().ordinal() > phase.ordinal()) return rule.minimumPhase();
      }
      return phase;
   }

   public record Update(ServantCombatPhase phase, boolean changed) { }
}

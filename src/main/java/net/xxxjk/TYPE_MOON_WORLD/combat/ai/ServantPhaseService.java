package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class ServantPhaseService {
   public static final long MAX_PROBING_TICKS = 30L * 20L;
   /** A full-health stalemate must also reach the noble phantasm phase. */
   public static final long PROLONGED_COMBAT_DECISIVE_TICKS = 60L * 20L;
   public static final float OVERWHELMING_STRENGTH_RATIO = 1.65F;
   private static final String TAG_PHASE = "TypeMoonAiCombatPhase";
   private static final String TAG_PHASE_CHANGED = "TypeMoonAiPhaseChangedAt";
   private static final String TAG_COMBAT_STARTED = "TypeMoonAiCombatStartedAt";
   private static final String TAG_LAST_COMBAT = "TypeMoonAiLastCombatAt";

   private ServantPhaseService() { }

   public static Update tick(ServantEntity entity, LivingEntity target, ServantActionProfile profile, long now) {
      var data = entity.getPersistentData();
      ServantCombatPhase current = ServantCombatPhase.byName(data.getString(TAG_PHASE));
      ServantCombatPhase desired = ServantCombatPhase.PROBING;
      boolean inCombat = target != null && target.isAlive() && target.level() == entity.level();
      if (!inCombat) {
         if (now - data.getLong(TAG_LAST_COMBAT) < 120L) desired = current;
         else data.remove(TAG_COMBAT_STARTED);
      } else {
         long lastCombat = data.getLong(TAG_LAST_COMBAT);
         if (!data.contains(TAG_COMBAT_STARTED) || now - lastCombat > 120L) data.putLong(TAG_COMBAT_STARTED, now);
         data.putLong(TAG_LAST_COMBAT, now);
         long elapsed = now - data.getLong(TAG_COMBAT_STARTED);
         float healthRatio = entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
         desired = desiredPhase(healthRatio, elapsed, (float)ServantCombatAssessment.strengthRatio(entity, target));
         if (elapsed >= MAX_PROBING_TICKS
            && (healthRatio <= 0.80F || target.getHealth() <= target.getMaxHealth() * 0.40F)
            && desired.ordinal() < ServantCombatPhase.DECISIVE.ordinal()) {
            desired = ServantCombatPhase.DECISIVE;
         }
         desired = applyRivalMinimum(desired, target, profile);
         int legacyMinimum = Math.min(ServantCombatPhase.DECISIVE.ordinal(), data.getInt("TypeMoonCombatPhase"));
         if (entity.getDefinition() != null
            && (entity.getDefinition().classType() == net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType.BERSERKER
               || entity.getDefinition().classType() == net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType.AVENGER)) {
            legacyMinimum = ServantCombatPhase.DECISIVE.ordinal();
         }
         if (desired.ordinal() < legacyMinimum) desired = ServantCombatPhase.values()[legacyMinimum];
         // Healing, temporary target loss and buffs cannot restart probing mid-fight.
         if (desired.ordinal() < current.ordinal()) desired = current;
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
      if (healthRatio <= 0.55F || targetStrengthRatio >= OVERWHELMING_STRENGTH_RATIO
         || combatTicks >= PROLONGED_COMBAT_DECISIVE_TICKS
         || combatTicks >= MAX_PROBING_TICKS && targetStrengthRatio >= 1.15F) {
         return ServantCombatPhase.DECISIVE;
      }
      if (combatTicks >= MAX_PROBING_TICKS || healthRatio <= 0.80F || targetStrengthRatio >= 1.30F) return ServantCombatPhase.NORMAL;
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

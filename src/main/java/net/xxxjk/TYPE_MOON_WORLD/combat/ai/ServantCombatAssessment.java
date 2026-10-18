package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

/** Shared, observable combat conditions for phase and action decisions. */
public final class ServantCombatAssessment {
   private ServantCombatAssessment() { }

   public static double strengthRatio(ServantEntity self, LivingEntity target) {
      return power(target) / Math.max(1.0, power(self));
   }

   private static double power(LivingEntity entity) {
      double attack = entity.getAttribute(Attributes.ATTACK_DAMAGE) == null ? 2.0
         : entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
      double magic = 0.0;
      double speed = entity.getAttribute(Attributes.MOVEMENT_SPEED) == null ? 0.2
         : entity.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
      if (entity instanceof ServantEntity servant && servant.getDefinition() != null) {
         var params = servant.getDefinition().parameters();
         if (params != null) {
            magic = params.manaPool() / 40.0;
            // Combat sprint ramping must not change the apparent matchup.
            speed = params.movementSpeed();
         }
      }
      return combatPower(entity.getMaxHealth(), attack, magic, entity.getArmorValue(), speed);
   }

   public static double combatPower(double health, double attack, double magic, double armor, double speed) {
      double offense = Math.max(2.0, Math.max(attack, magic));
      double defense = 1.0 + Math.min(30.0, Math.max(0.0, armor)) * 0.025;
      double mobility = Math.max(0.8, Math.min(1.3, 0.85 + speed * 0.6));
      return Math.sqrt(Math.max(1.0, health) * offense * defense) * mobility;
   }

   public static Opportunity opportunity(ServantEntity self, LivingEntity target) {
      boolean exposed = ServantCombatSystem.cannotAct(target)
         || ServantCombatMotionService.isImpactStaggered(target)
         || !ServantCapabilityResolver.isMobile(target);
      boolean targetCasting = target instanceof ServantEntity rival
         && (NoblePhantasmCastService.chanting(rival)
            || net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantDuelPacing.nativeNpActive(rival));
      boolean threatened = false;
      if (self.level() instanceof ServerLevel level) {
         CombatThreat incoming = CombatThreatService.incoming(level, self, level.getGameTime(), 16L,
            threat -> threat.danger() >= 3
               && level.getEntity(threat.sourceUuid()) instanceof LivingEntity source && !self.isAlliedTo(source));
         threatened = incoming != null;
      }
      return new Opportunity(exposed, targetCasting, threatened,
         self.getHealth() / Math.max(1.0F, self.getMaxHealth()),
         target.getHealth() / Math.max(1.0F, target.getMaxHealth()));
   }

   public record Opportunity(boolean exposed, boolean targetCasting, boolean threatened,
                             float healthRatio, float targetHealthRatio) {
      public boolean urgent() { return exposed || targetCasting || threatened || healthRatio <= 0.35F; }

      public boolean favorsNoblePhantasm(double distance, int windupTicks) {
         return exposed || targetCasting || healthRatio <= 0.35F
            || !threatened && distance >= Math.max(6.0, windupTicks * 0.12);
      }
   }
}

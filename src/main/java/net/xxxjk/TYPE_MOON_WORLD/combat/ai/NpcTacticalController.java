package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/** Shared defensive sensor/arbiter for non-Servant combat NPCs. */
public final class NpcTacticalController {
   private static final ResourceLocation EVADE_PROJECTILE = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ai/npc_evade_projectile");
   private static final ResourceLocation EVADE_ACTION = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ai/npc_evade_action");

   private NpcTacticalController() { }

   public static boolean tick(Mob entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive() || !AiMigrationPolicy.isArbitrated(entity)) return false;
      EvasionMovementService.tickAirState(entity);
      long now = level.getGameTime();
      AiBrain brain = AiBrain.begin(entity);
      LivingEntity currentTarget = entity.getTarget();
      LivingEntity target = EntityUtils.redirectMountedCombatTarget(entity, currentTarget);
      if (target != currentTarget) {
         entity.setTarget(target);
      }
      if (target != null && target.isAlive()) {
         brain.blackboard().observe(target.getUUID(), null, entity.distanceTo(target), 0.0, false, now);
      }

      int agility = agility(entity);
      if (entity.tickCount % 3 == Math.floorMod(entity.getId(), 3)) {
         entity.getPersistentData().putLong("TypeMoonAiProjectileScanTick", now);
         ProjectileThreatSensor.IncomingProjectile projectile = ProjectileThreatSensor.nearest(entity, 10.0, 8.0);
         if (projectile != null) {
            brain.submit(AiIntent.of(EVADE_PROJECTILE, AiIntent.PRIORITY_LETHAL_DEFENSE,
               100.0 - projectile.impactTicks() * 8.0, 4, false,
               () -> EvasionMovementService.tryEvade(entity, projectile.projectile().position(), agility, agility >= 5),
               AiControl.DEFEND, AiControl.MOVE, AiControl.LOOK));
         }
      }

      for (CombatThreat threat : CombatThreatService.nearby(level, entity.position(), 48.0, now)) {
         if (!hostile(entity, level, threat.sourceUuid()) || threat.ticksToImpact(now) > 20L || !threat.dodgeable()) continue;
         if (!entity.getUUID().equals(threat.targetUuid()) && !threat.threatens(entity.getEyePosition(), entity.getBbWidth() * 0.65)) continue;
         brain.blackboard().observe(threat.sourceUuid(), threat.actionId(), entity.position().distanceTo(threat.origin()), 0.0, true, now);
         brain.submit(AiIntent.of(EVADE_ACTION, AiIntent.PRIORITY_LETHAL_DEFENSE,
            threat.danger() * 20.0 + Math.max(0.0, 20.0 - threat.ticksToImpact(now)), 5, false,
            () -> EvasionMovementService.tryEvade(entity, threat.origin(), agility, agility >= 5),
            AiControl.DEFEND, AiControl.MOVE, AiControl.LOOK));
      }
      return brain.resolve().consumesLegacyControl();
   }

   private static int agility(Mob entity) {
      double speed = entity.getAttributeValue(Attributes.MOVEMENT_SPEED);
      if (speed >= 0.38) return 5;
      if (speed >= 0.31) return 3;
      return 1;
   }

   private static boolean hostile(Mob observer, ServerLevel level, UUID sourceUuid) {
      Entity source = level.getEntity(sourceUuid);
      return source instanceof LivingEntity living && living != observer && !observer.isAlliedTo(living);
   }
}

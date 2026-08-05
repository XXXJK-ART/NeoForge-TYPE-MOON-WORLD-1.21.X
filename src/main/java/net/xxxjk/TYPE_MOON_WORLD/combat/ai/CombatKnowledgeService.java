package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactType;

/** Records facts only when combat makes them observable. */
public final class CombatKnowledgeService {
   private CombatKnowledgeService() { }

   public static void observeWindup(LivingEntity observer, LivingEntity actor, AiActionDescriptor action, long now) {
      if (!(observer instanceof Mob mob) || actor == null || action == null) return;
      AiBlackboard board = AiBrain.blackboard(mob);
      if (action.tags().contains(AiActionDescriptor.Tag.PROJECTILE)) board.revealFact(actor.getUUID(), FactType.PROJECTILE_PRESSURE, 0.7, now);
      if (action.tags().contains(AiActionDescriptor.Tag.MELEE)) board.revealFact(actor.getUUID(), FactType.MELEE_PRESSURE, 0.7, now);
      if (action.tags().contains(AiActionDescriptor.Tag.AREA)) board.revealFact(actor.getUUID(), FactType.AREA_CONTROL, 0.7, now);
      if (action.tags().contains(AiActionDescriptor.Tag.CONTROL)) board.revealFact(actor.getUUID(), FactType.CONTROL, 0.7, now);
      if (action.tags().contains(AiActionDescriptor.Tag.HEAL)) board.revealFact(actor.getUUID(), FactType.HEAL, 0.7, now);
   }

   public static void observeProjectileNegation(LivingEntity defender, Projectile projectile) {
      observeProjectileNegation(defender, (Entity) projectile);
   }

   public static void observeProjectileNegation(LivingEntity defender, Entity projectileLike) {
      if (defender == null || projectileLike == null) return;
      Entity owner = projectileOwner(projectileLike);
      if (!(owner instanceof Mob observer) || observer.isAlliedTo(defender)) return;
      long now = defender.level().getGameTime();
      AiBrain.blackboard(observer).revealFact(defender.getUUID(), FactType.PROJECTILE_NEGATION, 1.0, now);
      AiBrain.blackboard(observer).observe(defender.getUUID(), null, observer.distanceTo(defender), 0.0, true, now);
   }

   private static Entity projectileOwner(Entity projectileLike) {
      if (projectileLike instanceof Projectile projectile) return projectile.getOwner();
      if (projectileLike instanceof GilgameshGateWeaponProjectileEntity gate) return gate.getOwnerEntity();
      return null;
   }
}

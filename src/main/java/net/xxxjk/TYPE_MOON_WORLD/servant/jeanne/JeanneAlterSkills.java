package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.typemoonworld.api.CardActionContext;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.ServantContext;

/** Server-authoritative Jeanne Alter actions kept independent of addon internals. */
public final class JeanneAlterSkills {
   private JeanneAlterSkills() {}
   public static ExecutionResult avengerB(ServantContext context) { return ExecutionResult.SUCCESS; }
   public static ExecutionResult oblivionCorrectionA(ServantContext context) { return ExecutionResult.SUCCESS; }
   public static ExecutionResult selfReplenishmentAPlus(ServantContext context) { return ExecutionResult.SUCCESS; }
   public static ExecutionResult firePillar(CardActionContext c) { return targetAction(c, 72.0F, 22, 140); }
   public static ExecutionResult cursedLance(CardActionContext c) { return targetAction(c, 125.0F, 35, 240); }
   public static ExecutionResult selfModification(CardActionContext c) {
      c.player().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
      return ExecutionResult.SUCCESS.withCost(30).withCooldown(500);
   }
   public static ExecutionResult dragonWitch(CardActionContext c) {
      for (LivingEntity ally : c.player().level().getEntitiesOfClass(LivingEntity.class,
            c.player().getBoundingBox().inflate(24.0D), e -> e != c.player() && e.isAlliedTo(c.player())))
         ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0));
      return ExecutionResult.SUCCESS.withCost(42).withCooldown(600);
   }
   public static ExecutionResult infernoBody(CardActionContext c) {
      c.player().addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300, 0));
      return ExecutionResult.SUCCESS.withCost(45).withCooldown(600);
   }
   public static ExecutionResult ephemeralDream(CardActionContext c) {
      c.player().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 4));
      return ExecutionResult.SUCCESS.withCost(55).withCooldown(800);
   }
   public static ExecutionResult noblePhantasm(CardActionContext c) {
      ServerPlayer p = c.player();
      for (LivingEntity target : p.level().getEntitiesOfClass(LivingEntity.class,
            p.getBoundingBox().inflate(18.0D), e -> e != p && !e.isAlliedTo(p))) {
         target.hurt(p.damageSources().playerAttack(p), 80.0F);
         target.setRemainingFireTicks(160);
      }
      return ExecutionResult.SUCCESS.withCost(220).withCooldown(2400);
   }
   public static ExecutionResult combatFirePillar(ServantContext c) { return ExecutionResult.SUCCESS; }
   public static ExecutionResult combatCursedLance(ServantContext c) { return ExecutionResult.SUCCESS; }
   public static ExecutionResult combatNoblePhantasm(ServantContext c) { return ExecutionResult.SUCCESS; }
   private static ExecutionResult targetAction(CardActionContext c, float damage, int cost, int cooldown) {
      ServerPlayer p = c.player();
      LivingEntity target = p.level().getEntitiesOfClass(LivingEntity.class,
            p.getBoundingBox().inflate(18.0D), e -> e != p && !e.isAlliedTo(p)).stream().findFirst().orElse(null);
      if (target == null) return ExecutionResult.FAILED;
      target.hurt(p.damageSources().playerAttack(p), damage);
      target.setRemainingFireTicks(100);
      return ExecutionResult.SUCCESS.withCost(cost).withCooldown(cooldown);
   }
}

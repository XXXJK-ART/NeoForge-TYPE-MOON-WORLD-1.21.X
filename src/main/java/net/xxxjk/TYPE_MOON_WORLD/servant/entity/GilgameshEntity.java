package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.minecraft.world.level.Level;

/** The Archer-class Hero King. This is intentionally an NPC-only servant. */
public class GilgameshEntity extends ServantEntity {
   public static final String SERVANT_KEY = "gilgamesh";

   public GilgameshEntity(EntityType<GilgameshEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (!this.level().isClientSide() && this.isAlive() && !this.isSpiritualDissolving()) {
         GilgameshCombatHelper.tick(this);
      }
   }

   @Override protected boolean useFloatingAnimation() { return GilgameshCombatHelper.isFlying(this); }

   @Override protected String getLoopAnimationOverride(net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations animations, boolean moving) {
      return GilgameshCombatHelper.isFlying(this) ? animations.actionAnimation("fly").orElse(animations.actionAnimation("idle").orElse(null)) : null;
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
         && attacker != this && attacker.position().subtract(this.position()).dot(this.getLookAngle()) < -0.25) {
         this.setTarget(attacker);
         this.setYRot(attacker.getYRot() + 180.0F);
         this.yRotO = this.getYRot();
      }
      return super.hurt(source, amount);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      boolean hit = super.doHurtTarget(target);
      if (hit && target instanceof net.minecraft.world.entity.LivingEntity living) {
         living.invulnerableTime = 0;
         living.hurt(this.damageSources().magic(), 5.0F);
         if (this.getRandom().nextFloat() < 0.15F) {
            living.invulnerableTime = 0;
            living.hurt(this.damageSources().mobAttack(this), (float)this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.5F);
         }
      }
      return hit;
   }

   @Override
   public boolean killedEntity(ServerLevel level, net.minecraft.world.entity.LivingEntity victim) {
      boolean result = super.killedEntity(level, victim);
      if (this.getRandom().nextFloat() >= 0.15F) return result;
      net.minecraft.world.item.Item[] pool = {
         ModItems.DRAGON_FANG.get(), ModItems.DRAGONS_REVERSE_SCALE.get(), ModItems.PHOENIX_FEATHER.get(),
         ModItems.PROOF_OF_HERO.get(), ModItems.VOIDS_DUST.get(), ModItems.REMNANTS_OF_MADNESS.get(),
         ModItems.SEED_OF_YGGDRASIL.get(), ModItems.QP.get()
      };
      ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 0.3, victim.getZ(), new ItemStack(pool[this.getRandom().nextInt(pool.length)]));
      level.addFreshEntity(drop);
      return result;
   }
}

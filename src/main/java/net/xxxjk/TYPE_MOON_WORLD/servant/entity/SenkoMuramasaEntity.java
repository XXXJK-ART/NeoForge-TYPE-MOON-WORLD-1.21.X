package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;

public class SenkoMuramasaEntity extends ServantEntity {
   public static final String SERVANT_KEY = "senko_muramasa";

   public SenkoMuramasaEntity(EntityType<SenkoMuramasaEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   public static AttributeSupplier.Builder createMuramasaAttributes() {
      return ServantEntity.createAttributes()
         .add(Attributes.ARMOR_TOUGHNESS, 4.0);
   }

   @Override
   protected void customServerAiStep() {
      if (!this.level().isClientSide() && MuramasaCombatHelper.isCharging(this)) {
         MuramasaCombatHelper.tickCharge(this);
         return;
      }
      super.customServerAiStep();
      if (!this.level().isClientSide()) {
         MuramasaCombatHelper.tickPassive(this);
      }
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      MuramasaCombatHelper.prepareAttack(this, target);
      boolean hit = target instanceof net.minecraft.world.entity.LivingEntity living
         && MuramasaCombatHelper.shouldBypassDefense(this, living)
         ? MuramasaCombatHelper.performPrecisionStrike(this, living)
         : super.doHurtTarget(target);
      MuramasaCombatHelper.afterAttack(this, target, hit);
      return hit;
   }

   @Override
   public void die(net.minecraft.world.damagesource.DamageSource cause) {
      MuramasaCombatHelper.cleanup(this);
      super.die(cause);
   }

   @Override
   public void remove(RemovalReason reason) {
      if (!this.level().isClientSide()) {
         MuramasaCombatHelper.cleanup(this);
      }
      super.remove(reason);
   }
}

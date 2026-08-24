package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantTrueSweepService;

public class HeraclesEntity extends ServantEntity {
   public static final String SERVANT_KEY = "heracles";

   public HeraclesEntity(EntityType<HeraclesEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   public void knockback(double strength, double x, double z) {
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (target instanceof LivingEntity living && ServantMasterProtection.isProtectedMaster(this, living)) {
         return false;
      }
      return super.doHurtTarget(target);
   }

   @Override
   public boolean doBasicHurtTarget(LivingEntity target) {
      if (target == null || ServantMasterProtection.isProtectedMaster(this, target)) return false;
      if (ServantTrueSweepService.triggerNpcAttack(this, target, this.level().getGameTime())) {
         return true;
      }
      return super.doHurtTarget(target);
   }
}

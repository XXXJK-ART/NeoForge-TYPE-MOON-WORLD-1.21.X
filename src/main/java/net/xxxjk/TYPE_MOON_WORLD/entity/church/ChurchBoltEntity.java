package net.xxxjk.TYPE_MOON_WORLD.entity.church;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.entity.BlackKeyProjectileEntity;

public class ChurchBoltEntity extends Arrow {
   public ChurchBoltEntity(EntityType<? extends Arrow> type, Level level) { super(type, level); }
   public ChurchBoltEntity(Level level, LivingEntity owner) { super(level, owner, net.minecraft.world.item.Items.ARROW.getDefaultInstance(), null); }

   @Override
   protected void doPostHurtEffects(LivingEntity target) {
      super.doPostHurtEffects(target);
      if (!level().isClientSide && target.isAlive() && BlackKeyProjectileEntity.isUndead(target)) {
         target.invulnerableTime = 0;
         target.hurt(damageSources().arrow(this, getOwner()), (float)getBaseDamage());
      }
   }
}

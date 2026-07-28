package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class OdaNobunagaEntity extends ServantEntity {
   public static final String SERVANT_KEY = "oda_nobunaga";

   public OdaNobunagaEntity(EntityType<OdaNobunagaEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA) || source.is(DamageTypes.HOT_FLOOR)) {
         this.clearFire();
         return false;
      }
      return super.hurt(source, amount);
   }

   @Override
   public void die(DamageSource cause) {
      if (!this.level().isClientSide()) {
         OdaNobunagaCombatHelper.cleanupHajun(this);
      }
      super.die(cause);
   }

   @Override
   public void remove(RemovalReason reason) {
      if (!this.level().isClientSide() && reason != RemovalReason.CHANGED_DIMENSION) {
         OdaNobunagaCombatHelper.cleanupHajun(this);
      }
      super.remove(reason);
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class GawainEntity extends ServantEntity {
   public static final String SERVANT_KEY = "gawain";

   public GawainEntity(EntityType<GawainEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   public void die(DamageSource cause) {
      if (GawainCombatHelper.tryConsumeGuts(this)) {
         return;
      }
      super.die(cause);
   }
}

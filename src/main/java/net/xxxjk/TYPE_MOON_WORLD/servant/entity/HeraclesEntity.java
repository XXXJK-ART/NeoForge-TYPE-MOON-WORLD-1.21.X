package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class HeraclesEntity extends ServantEntity {
   public static final String SERVANT_KEY = "heracles";

   public HeraclesEntity(EntityType<HeraclesEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   public void knockback(double strength, double x, double z) {
   }
}

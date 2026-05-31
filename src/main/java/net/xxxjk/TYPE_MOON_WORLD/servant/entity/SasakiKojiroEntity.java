package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SasakiKojiroEntity extends ServantEntity {
   public static final String SERVANT_KEY = "sasaki_kojiro";

   public SasakiKojiroEntity(EntityType<SasakiKojiroEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }
}

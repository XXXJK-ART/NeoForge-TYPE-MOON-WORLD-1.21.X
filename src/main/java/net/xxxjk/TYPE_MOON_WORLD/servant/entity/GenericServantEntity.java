package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Data-driven servant entity for addon definitions that do not need a custom entity class. */
public final class GenericServantEntity extends ServantEntity {
   public GenericServantEntity(EntityType<? extends GenericServantEntity> type, Level level) {
      super(type, level, "");
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class CuChulainnEntity extends ServantEntity {
   public static final String SERVANT_KEY = "cu_chulainn";

   public CuChulainnEntity(EntityType<CuChulainnEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }
}

package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

public final class BucephalusEntity extends IskandarMountEntity {
   public BucephalusEntity(EntityType<? extends BucephalusEntity> type, Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createMountAttributes(1000.0, 0.48);
   }

   @Override
   protected String getLoopAnimation() {
      return "standing";
   }
}

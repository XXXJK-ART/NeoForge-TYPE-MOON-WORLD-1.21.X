package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

public final class BucephalusEntity extends IskandarMountEntity {
   public BucephalusEntity(EntityType<? extends BucephalusEntity> type, Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createMountAttributes(2000.0, 0.48);
   }

   @Override
   protected String getLoopAnimation() {
      return "standing";
   }

   @Override
   protected String getMovingAnimation() {
      return "walk";
   }

   @Override
   protected String getChargeAnimation() {
      return "gallop";
   }

   @Override
   protected double getChargeSpeed() {
      return Math.max(0.74, getCombatSpeed() * 1.32);
   }

   @Override
   protected double getChargeKnockback() {
      return 1.45;
   }
}

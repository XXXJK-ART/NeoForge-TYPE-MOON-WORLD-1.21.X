package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class BucephalusEntity extends IskandarMountEntity {
   public BucephalusEntity(EntityType<? extends BucephalusEntity> type, Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createMountAttributes(2000.0, 0.48);
   }

   @Override
   protected void followCardOwnerInput(ServerPlayer owner) {
      float forwardInput = owner.zza;
      float strafeInput = owner.xxa;
      float yaw = Mth.rotLerp(0.32F, this.getYRot(), owner.getYRot());
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);

      Vec3 forward = owner.getLookAngle();
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(-Math.sin(Math.toRadians(yaw)), 0.0, Math.cos(Math.toRadians(yaw)));
      }
      forward = forward.normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 intent = forward.scale(forwardInput).add(right.scale(-strafeInput));
      if (intent.lengthSqr() > 1.0E-4) {
         intent = intent.normalize();
         double speed = Math.max(0.0, this.getCombatSpeed() * (owner.isSprinting() ? 1.35 : 1.0));
         this.move(MoverType.SELF, intent.scale(speed));
         this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
      } else {
         this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
      }
      this.hasImpulse = true;
      this.fallDistance = 0.0F;
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

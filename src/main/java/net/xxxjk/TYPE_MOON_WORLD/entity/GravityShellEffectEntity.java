package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravityEffectHandler;

public class GravityShellEffectEntity extends Entity {
   private static final EntityDataAccessor<Float> RADIUS_XZ = SynchedEntityData.defineId(GravityShellEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> RADIUS_Y = SynchedEntityData.defineId(GravityShellEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> STACKS = SynchedEntityData.defineId(GravityShellEffectEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> ALPHA = SynchedEntityData.defineId(GravityShellEffectEntity.class, EntityDataSerializers.FLOAT);
   private UUID targetUUID;
   private LivingEntity cachedTarget;

   public GravityShellEffectEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public GravityShellEffectEntity(Level level, LivingEntity target) {
      this(ModEntities.GRAVITY_SHELL_EFFECT.get(), level);
      this.setTarget(target);
      this.refreshFromTarget();
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(RADIUS_XZ, 0.8F);
      builder.define(RADIUS_Y, 1.2F);
      builder.define(STACKS, 1);
      builder.define(ALPHA, 0.35F);
   }

   public void setTarget(LivingEntity target) {
      this.cachedTarget = target;
      this.targetUUID = target == null ? null : target.getUUID();
   }

   public LivingEntity getTargetEntity() {
      if (this.cachedTarget == null && this.targetUUID != null && this.level() instanceof ServerLevel serverLevel) {
         Entity entity = serverLevel.getEntity(this.targetUUID);
         if (entity instanceof LivingEntity living) {
            this.cachedTarget = living;
         }
      }

      return this.cachedTarget;
   }

   public float getRadiusXZ() {
      return this.entityData.get(RADIUS_XZ);
   }

   public float getRadiusY() {
      return this.entityData.get(RADIUS_Y);
   }

   public int getStacks() {
      return this.entityData.get(STACKS);
   }

   public float getAlphaStrength() {
      return this.entityData.get(ALPHA);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (!this.refreshFromTarget()) {
            this.discard();
         }
      }
   }

   private boolean refreshFromTarget() {
      LivingEntity target = this.getTargetEntity();
      if (target == null || !target.isAlive() || target.isRemoved()) {
         return false;
      } else {
         int mode = MagicGravityEffectHandler.getCurrentMode(target);
         if (mode < 1) {
            return false;
         } else {
            int stacks = MagicGravityEffectHandler.getCurrentStacks(target);
            float width = target.getBbWidth() * 0.575F + 0.08F * (stacks - 1);
            float height = target.getBbHeight() * 0.575F + 0.1F * (stacks - 1);
            this.setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
            this.entityData.set(RADIUS_XZ, width);
            this.entityData.set(RADIUS_Y, height);
            this.entityData.set(STACKS, stacks);
            this.entityData.set(ALPHA, Math.min(0.72F, 0.26F + 0.08F * stacks + (mode == 2 ? 0.08F : 0.0F)));
            return true;
         }
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Target")) {
         this.targetUUID = tag.getUUID("Target");
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.targetUUID != null) {
         tag.putUUID("Target", this.targetUUID);
      }
   }
}

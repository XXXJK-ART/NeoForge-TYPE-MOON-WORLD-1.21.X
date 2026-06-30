package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RedSkeletonHajunEntity extends Entity implements GeoEntity {
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(RedSkeletonHajunEntity.class, EntityDataSerializers.INT);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private UUID ownerUuid;

   public RedSkeletonHajunEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public RedSkeletonHajunEntity(Level level, LivingEntity owner, int duration) {
      this(ModEntities.RED_SKELETON_HAJUN.get(), level);
      this.ownerUuid = owner == null ? null : owner.getUUID();
      this.entityData.set(DURATION, duration);
      if (owner != null) {
         followOwner(owner);
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(DURATION, 20 * 20);
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      int duration = this.entityData.get(DURATION) - 1;
      this.entityData.set(DURATION, duration);
      LivingEntity owner = getOwnerLiving(level);
      if (duration <= 0 || owner == null || !owner.isAlive()) {
         this.discard();
         return;
      }
      followOwner(owner);
      if (this.tickCount % 5 == 0) {
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 2.2, this.getZ(), 10, 1.1, 1.2, 1.1, 0.02);
      }
   }

   private void followOwner(LivingEntity owner) {
      Vec3 look = owner.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-4) {
         look = new Vec3(0.0, 0.0, 1.0);
      } else {
         look = look.normalize();
      }
      Vec3 pos = owner.position().subtract(look.scale(4.5)).add(0.0, 0.6, 0.0);
      this.setPos(pos.x, pos.y, pos.z);
      float yaw = (float)(Mth.atan2(look.x, look.z) * Mth.RAD_TO_DEG);
      this.setYRot(yaw);
      this.yRotO = yaw;
      this.setXRot(0.0F);
      this.xRotO = 0.0F;
   }

   private LivingEntity getOwnerLiving(ServerLevel level) {
      if (this.ownerUuid == null) {
         return null;
      }
      Entity owner = level.getEntity(this.ownerUuid);
      return owner instanceof LivingEntity living ? living : null;
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) {
         this.ownerUuid = tag.getUUID("Owner");
      }
      this.entityData.set(DURATION, tag.contains("Duration") ? tag.getInt("Duration") : 20 * 20);
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }
      tag.putInt("Duration", this.entityData.get(DURATION));
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event ->
         event.setAndContinue(RawAnimation.begin().thenLoop("standing"))
      ));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}

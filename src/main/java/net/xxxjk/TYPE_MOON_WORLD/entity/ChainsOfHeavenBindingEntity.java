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
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ChainsOfHeavenBindingEntity extends Entity implements GeoEntity {
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(ChainsOfHeavenBindingEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DIVINE_BIND = SynchedEntityData.defineId(ChainsOfHeavenBindingEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> WIDTH_SCALE = SynchedEntityData.defineId(ChainsOfHeavenBindingEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> HEIGHT_SCALE = SynchedEntityData.defineId(ChainsOfHeavenBindingEntity.class, EntityDataSerializers.FLOAT);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private UUID targetUuid;
   private UUID ownerUuid;

   public ChainsOfHeavenBindingEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public ChainsOfHeavenBindingEntity(Level level, LivingEntity owner, LivingEntity target, int duration, boolean divineBind) {
      this(ModEntities.CHAINS_OF_HEAVEN_BINDING.get(), level);
      this.ownerUuid = owner == null ? null : owner.getUUID();
      this.targetUuid = target == null ? null : target.getUUID();
      this.entityData.set(DURATION, duration);
      this.entityData.set(DIVINE_BIND, divineBind);
      if (target != null) {
         followTarget(target);
      }
   }

   public boolean isDivineBind() {
      return this.entityData.get(DIVINE_BIND);
   }

   public float getWidthScale() {
      return this.entityData.get(WIDTH_SCALE);
   }

   public float getHeightScale() {
      return this.entityData.get(HEIGHT_SCALE);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(DURATION, 20);
      builder.define(DIVINE_BIND, false);
      builder.define(WIDTH_SCALE, 1.0F);
      builder.define(HEIGHT_SCALE, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }

      int duration = this.entityData.get(DURATION) - 1;
      this.entityData.set(DURATION, duration);
      LivingEntity target = getTargetLiving(level);
      if (duration <= 0 || target == null || !target.isAlive()) {
         this.discard();
         return;
      }
      followTarget(target);
      if (this.tickCount % 4 == 0) {
         level.sendParticles(this.isDivineBind() ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER,
            this.getX(), this.getY() + target.getBbHeight() * 0.55, this.getZ(), this.isDivineBind() ? 10 : 5, 0.55, 0.55, 0.55, 0.02);
      }
   }

   private void followTarget(LivingEntity target) {
      this.setPos(target.getX(), target.getY(), target.getZ());
      this.setYRot(target.getYRot());
      this.yRotO = this.getYRot();
      this.entityData.set(WIDTH_SCALE, Mth.clamp(target.getBbWidth() / 0.6F, 0.55F, 5.5F));
      this.entityData.set(HEIGHT_SCALE, Mth.clamp(target.getBbHeight() / 1.8F, 0.55F, 4.5F));
   }

   private LivingEntity getTargetLiving(ServerLevel level) {
      if (this.targetUuid == null) {
         return null;
      }
      Entity target = level.getEntity(this.targetUuid);
      return target instanceof LivingEntity living ? living : null;
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Target")) {
         this.targetUuid = tag.getUUID("Target");
      }
      if (tag.hasUUID("Owner")) {
         this.ownerUuid = tag.getUUID("Owner");
      }
      this.entityData.set(DURATION, tag.contains("Duration") ? tag.getInt("Duration") : 20);
      this.entityData.set(DIVINE_BIND, tag.getBoolean("DivineBind"));
      this.entityData.set(WIDTH_SCALE, tag.contains("WidthScale") ? tag.getFloat("WidthScale") : 1.0F);
      this.entityData.set(HEIGHT_SCALE, tag.contains("HeightScale") ? tag.getFloat("HeightScale") : 1.0F);
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.targetUuid != null) {
         tag.putUUID("Target", this.targetUuid);
      }
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }
      tag.putInt("Duration", this.entityData.get(DURATION));
      tag.putBoolean("DivineBind", this.entityData.get(DIVINE_BIND));
      tag.putFloat("WidthScale", this.entityData.get(WIDTH_SCALE));
      tag.putFloat("HeightScale", this.entityData.get(HEIGHT_SCALE));
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event ->
         event.setAndContinue(RawAnimation.begin().thenLoop("animation.chains_of_heaven.bind"))
      ));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}

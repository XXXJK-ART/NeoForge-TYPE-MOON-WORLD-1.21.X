package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
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
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RhoAiasEntity extends Entity implements GeoEntity {
   private static final float MAX_SHIELD_HP = 3500.0F;
   private static final EntityDataAccessor<Float> SHIELD_HP = SynchedEntityData.defineId(RhoAiasEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(RhoAiasEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> LAYERS = SynchedEntityData.defineId(RhoAiasEntity.class, EntityDataSerializers.INT);
   private static final double PROTECT_RADIUS = 6.0;
   private static final double OWNER_EXIT_DISTANCE = 5.4;
   private static final double OWNER_BEHIND_DOT = -0.25;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private UUID ownerUuid;

   public RhoAiasEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public RhoAiasEntity(Level level, LivingEntity owner) {
      this(level, owner, null);
   }

   public RhoAiasEntity(Level level, LivingEntity owner, LivingEntity target) {
      this(ModEntities.RHO_AIAS_SHIELD.get(), level);
      this.ownerUuid = owner == null ? null : owner.getUUID();
      this.entityData.set(SHIELD_HP, MAX_SHIELD_HP);
      this.entityData.set(DURATION, 20 * 15);
      this.entityData.set(LAYERS, 7);
      if (owner != null) {
         Vec3 direction = fixedDirection(owner, target);
         Vec3 pos = owner.position().add(direction.scale(2.2)).add(0.0, owner.getBbHeight() * 0.55, 0.0);
         this.setPos(pos.x, pos.y, pos.z);
         this.setFacingDirection(direction);
         this.setXRot(0.0F);
      }
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(SHIELD_HP, MAX_SHIELD_HP);
      builder.define(DURATION, 20 * 15);
      builder.define(LAYERS, 7);
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity owner = this.getOwnerEntity();
      if (owner == null || !owner.isAlive()) {
         this.discard();
         return;
      }
      if (this.tickCount >= this.entityData.get(DURATION) || this.entityData.get(SHIELD_HP) <= 0.0F) {
         this.discard();
         return;
      }
      if (owner.distanceToSqr(this) > OWNER_EXIT_DISTANCE * OWNER_EXIT_DISTANCE) {
         this.discard();
         return;
      }
      this.updateFacingFromOwner(owner);
      if (!this.ownerIsBehindShield(owner)) {
         this.discard();
         return;
      }
      if (this.tickCount % 2 == 0) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 4, 1.1, 1.1, 1.1, 0.0);
      }
   }

   public boolean protects(LivingEntity entity) {
      LivingEntity owner = this.getOwnerEntity();
      return owner != null && entity != null && entity.isAlive() && (entity == owner || owner.isAlliedTo(entity))
         && entity.distanceToSqr(owner) <= PROTECT_RADIUS * PROTECT_RADIUS
         && entity.distanceToSqr(this) <= (PROTECT_RADIUS + 2.0) * (PROTECT_RADIUS + 2.0);
   }

   public float absorb(float damage) {
      float hp = this.entityData.get(SHIELD_HP);
      float absorbed = Math.min(hp, damage);
      float remaining = hp - absorbed;
      this.entityData.set(SHIELD_HP, remaining);
      updateLayers(remaining);
      if (remaining <= 0.0F) {
         this.discard();
      }
      return absorbed;
   }

   public float getShieldHp() {
      return this.entityData.get(SHIELD_HP);
   }

   public int getLayers() {
      return this.entityData.get(LAYERS);
   }

   public LivingEntity getOwnerEntity() {
      if (this.ownerUuid == null || !(this.level() instanceof ServerLevel level)) {
         return null;
      }
      Entity entity = level.getEntity(this.ownerUuid);
      return entity instanceof LivingEntity living ? living : null;
   }

   public Vec3 getFacingDirection() {
      double radians = (this.getYRot() + 90.0F) * Mth.DEG_TO_RAD;
      return new Vec3(Math.cos(radians), 0.0, Math.sin(radians)).normalize();
   }

   public Vec3 getCoverPosition(double ownerY) {
      Vec3 forward = this.getFacingDirection();
      return new Vec3(this.getX() - forward.x * 2.15, ownerY, this.getZ() - forward.z * 2.15);
   }

   private void updateLayers(float hp) {
      int layers = Mth.clamp(Mth.ceil(Math.max(0.0F, hp) / MAX_SHIELD_HP * 7.0F), 1, 7);
      this.entityData.set(LAYERS, layers);
   }

   private boolean ownerIsBehindShield(LivingEntity owner) {
      if (owner.distanceToSqr(this) > OWNER_EXIT_DISTANCE * OWNER_EXIT_DISTANCE) {
         return false;
      }
      Vec3 toOwner = owner.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
      if (toOwner.lengthSqr() < 1.0E-4) {
         return true;
      }
      return toOwner.normalize().dot(this.getFacingDirection()) <= OWNER_BEHIND_DOT;
   }

   private void updateFacingFromOwner(LivingEntity owner) {
      Vec3 look = owner.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-4) {
         return;
      }
      this.setFacingDirection(look.normalize());
   }

   private void setFacingDirection(Vec3 direction) {
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return;
      }
      float yaw = (float)(Mth.atan2(horizontal.z, horizontal.x) * Mth.RAD_TO_DEG) - 90.0F;
      this.yRotO = this.getYRot();
      this.setYRot(yaw);
   }

   private static Vec3 fixedDirection(LivingEntity owner, LivingEntity target) {
      Vec3 direction = target != null ? target.position().subtract(owner.position()) : owner.getLookAngle();
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = owner.getLookAngle();
         horizontal = new Vec3(horizontal.x, 0.0, horizontal.z);
      }
      return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) {
         this.ownerUuid = tag.getUUID("Owner");
      }
      float hp = tag.contains("ShieldHp") ? tag.getFloat("ShieldHp") : MAX_SHIELD_HP;
      this.entityData.set(SHIELD_HP, hp);
      this.entityData.set(DURATION, tag.contains("Duration") ? tag.getInt("Duration") : 20 * 15);
      this.entityData.set(LAYERS, tag.contains("Layers") ? Mth.clamp(tag.getInt("Layers"), 1, 7) : 7);
      if (tag.contains("FixedYaw")) {
         float yaw = tag.getFloat("FixedYaw");
         this.setYRot(yaw);
         this.yRotO = yaw;
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }
      tag.putFloat("ShieldHp", this.entityData.get(SHIELD_HP));
      tag.putInt("Duration", this.entityData.get(DURATION));
      tag.putInt("Layers", this.entityData.get(LAYERS));
      tag.putFloat("FixedYaw", this.getYRot());
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event ->
         event.setAndContinue(RawAnimation.begin().thenLoop(String.valueOf(this.getLayers())))
      ));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}

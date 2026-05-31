package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MedusaPegasusEntity extends PathfinderMob implements GeoEntity {
   private static final EntityDataAccessor<Boolean> FLYING_MODE = SynchedEntityData.defineId(MedusaPegasusEntity.class, EntityDataSerializers.BOOLEAN);
   private static final String TAG_SUMMONER_UUID = "MedusaPegasusSummoner";
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   @Nullable
   private UUID summonerUuid;

   public MedusaPegasusEntity(EntityType<? extends MedusaPegasusEntity> entityType, Level level) {
      super(entityType, level);
      this.setPersistenceRequired();
      this.setNoGravity(true);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 120.0)
         .add(Attributes.MOVEMENT_SPEED, 0.45)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.FLYING_SPEED, 0.55)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.FOLLOW_RANGE, 48.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FLYING_MODE, false);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      this.setNoGravity(this.isFlyingMode());
      this.fallDistance = 0.0F;
      LivingEntity summoner = this.getSummoner();
      if (this.summonerUuid != null && (summoner == null || !summoner.isAlive())) {
         this.discard();
      }
      if (!this.isVehicle() && this.tickCount > 40 && !this.level().isClientSide()) {
         this.discard();
      }
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event -> {
         if (this.isFlyingMode()) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.medusa_pegasus.flying"));
         }
         if (event.isMoving()) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.medusa_pegasus.walk"));
         }
         return event.setAndContinue(RawAnimation.begin().thenLoop("animation.medusa_pegasus.idle"));
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) {
         return true;
      }
      LivingEntity summoner = this.getSummoner();
      return summoner != null && (other == summoner || summoner.isAlliedTo(other));
   }

   @Override
   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
      return new Vec3(0.0, 0.55, -0.2);
   }

   public boolean isFlyingMode() {
      return this.entityData.get(FLYING_MODE);
   }

   public void setFlyingMode(boolean flyingMode) {
      this.entityData.set(FLYING_MODE, flyingMode);
      this.setNoGravity(flyingMode);
   }

   public void setSummoner(MedusaEntity summoner) {
      this.summonerUuid = summoner.getUUID();
      this.getPersistentData().putUUID(TAG_SUMMONER_UUID, this.summonerUuid);
   }

   @Nullable
   public LivingEntity getSummoner() {
      if (this.summonerUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
         return null;
      }
      Entity entity = serverLevel.getEntity(this.summonerUuid);
      return entity instanceof LivingEntity living ? living : null;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.summonerUuid != null) {
         tag.putUUID(TAG_SUMMONER_UUID, this.summonerUuid);
      }
      tag.putBoolean("PegasusFlyingMode", this.isFlyingMode());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID(TAG_SUMMONER_UUID)) {
         this.summonerUuid = tag.getUUID(TAG_SUMMONER_UUID);
         this.getPersistentData().putUUID(TAG_SUMMONER_UUID, this.summonerUuid);
      }
      this.entityData.set(FLYING_MODE, tag.getBoolean("PegasusFlyingMode"));
   }
}

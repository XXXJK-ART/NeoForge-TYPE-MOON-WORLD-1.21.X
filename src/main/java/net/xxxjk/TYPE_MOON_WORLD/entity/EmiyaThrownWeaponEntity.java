package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;

public class EmiyaThrownWeaponEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Float> FIXED_DAMAGE = SynchedEntityData.defineId(EmiyaThrownWeaponEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> BREAK_LOW_HARDNESS = SynchedEntityData.defineId(EmiyaThrownWeaponEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> PIERCING_IMPACT = SynchedEntityData.defineId(EmiyaThrownWeaponEntity.class, EntityDataSerializers.BOOLEAN);
   private final Set<Integer> hitEntities = new HashSet<>();
   private double curveStartX;
   private double curveStartZ;
   private double curveForwardX;
   private double curveForwardZ;
   private double curveSideX;
   private double curveSideZ;
   private double curveSideOffset;
   private double curveLength;
   private double maxDistanceSqr;
   private double maxDistanceOriginX;
   private double maxDistanceOriginY;
   private double maxDistanceOriginZ;
   private boolean maxDistanceInitialized;
   private boolean arcInitialized;
   private int lastBreakthroughTick;

   public EmiyaThrownWeaponEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public EmiyaThrownWeaponEntity(Level level, LivingEntity shooter, ItemStack stack) {
      super(ModEntities.EMIYA_THROWN_WEAPON.get(), shooter, level);
      this.setItem(stack);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FIXED_DAMAGE, 24.0F);
      builder.define(BREAK_LOW_HARDNESS, false);
      builder.define(PIERCING_IMPACT, false);
   }

   public void setFixedDamage(float damage) {
      this.entityData.set(FIXED_DAMAGE, damage);
   }

   public void setBreakLowHardnessBlocks(boolean enabled) {
      this.entityData.set(BREAK_LOW_HARDNESS, enabled);
   }

   public void setPiercingImpact(boolean enabled) {
      this.entityData.set(PIERCING_IMPACT, enabled);
      if (enabled) {
         this.entityData.set(BREAK_LOW_HARDNESS, true);
      }
   }

   public boolean isPiercingImpact() {
      return this.entityData.get(PIERCING_IMPACT);
   }

   public boolean isLancelotIronRodProjectile() {
      return this.getItem().is(ModItems.LANCELOT_IRON_ROD.get());
   }

   public void setMaxFlightDistance(double blocks) {
      if (blocks <= 0.0) {
         this.maxDistanceSqr = 0.0;
         this.maxDistanceInitialized = false;
         return;
      }
      this.maxDistanceSqr = blocks * blocks;
      this.maxDistanceOriginX = this.getX();
      this.maxDistanceOriginY = this.getY();
      this.maxDistanceOriginZ = this.getZ();
      this.maxDistanceInitialized = true;
   }

   public void setArcingFlight(Vec3 direction, double sideOffset, double curveLength) {
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(0.0, 0.0, 1.0);
      } else {
         horizontal = horizontal.normalize();
      }
      this.curveForwardX = horizontal.x;
      this.curveForwardZ = horizontal.z;
      this.curveSideX = -horizontal.z;
      this.curveSideZ = horizontal.x;
      this.curveSideOffset = sideOffset;
      this.curveLength = Math.max(1.0, curveLength);
      this.arcInitialized = false;
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      return entity != null && entity != this.getOwner() && !EntityUtils.isImmunePlayerTarget(entity) && super.canHitEntity(entity);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.IRON_SWORD;
   }

   @Override
   public void tick() {
      Vec3 previousPosition = this.position();
      super.tick();
      if (!this.level().isClientSide() && exceededMaxFlightDistance()) {
         this.discard();
         return;
      }
      updateArcFlight();
      updatePoseFromMotion();
      if (!this.level().isClientSide() && !this.isRemoved()) {
         sweepHit(previousPosition);
      }
      if (!this.level().isClientSide() && this.entityData.get(PIERCING_IMPACT)) {
         destroyBreakthroughPath(previousPosition);
      }
      if (!this.level().isClientSide() && this.entityData.get(BREAK_LOW_HARDNESS)) {
         destroySoftBlocksAhead();
      }
      if (this.tickCount > 100) {
         this.discard();
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!(result.getEntity() instanceof LivingEntity living) || this.level().isClientSide()) {
         return;
      }
      if (this.hitEntities.add(living.getId())) {
         DamageSource source = this.getOwner() instanceof LivingEntity owner ? this.damageSources().mobProjectile(this, owner) : this.damageSources().thrown(this, this);
         living.invulnerableTime = 0;
         living.hurt(source, this.damageForCurrentItem());
         living.invulnerableTime = 0;
      }
      if (!this.entityData.get(PIERCING_IMPACT)) {
         this.discard();
      }
   }

   @Override
   protected void onHit(HitResult result) {
      if (this.entityData.get(PIERCING_IMPACT)) {
         if (result.getType() == HitResult.Type.ENTITY && result instanceof EntityHitResult entityHit) {
            this.onHitEntity(entityHit);
            return;
         }
         if (result.getType() == HitResult.Type.BLOCK && result instanceof BlockHitResult blockHit) {
            this.onHitBlock(blockHit);
            return;
         }
      }
      super.onHit(result);
   }

   @Override
   protected void onHitBlock(BlockHitResult result) {
      if (!this.entityData.get(PIERCING_IMPACT)) {
         super.onHitBlock(result);
         return;
      }
      if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
         Vec3 motion = this.getDeltaMovement();
         Vec3 direction = motion.lengthSqr() > 1.0E-4 ? motion.normalize() : this.getLookAngle();
         TerrainImpactService.impactForwardBreakthrough(serverLevel, this.getOwner() instanceof LivingEntity living ? living : null,
            result.getLocation(), direction, TerrainImpactProfile.of(TerrainImpactProfile.Tier.MEDIUM), 3.8, 2, 2);
         this.setPos(result.getLocation().add(direction.scale(0.45)));
      }
   }

   private float damageForCurrentItem() {
      float fixed = this.entityData.get(FIXED_DAMAGE);
      if (fixed > 0.0F) {
         return fixed;
      }
      ItemStack stack = this.getItem();
      if (stack.isEmpty()) {
         return 8.0F;
      }
      ItemAttributeModifiers modifiers = (ItemAttributeModifiers)stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      return (float)Math.max(8.0, modifiers.compute(1.0, EquipmentSlot.MAINHAND));
   }

   private void updateArcFlight() {
      if (Math.abs(this.curveSideOffset) < 1.0E-4 || this.curveLength <= 0.0) {
         return;
      }
      if (!this.arcInitialized) {
         this.curveStartX = this.getX() - this.curveSideX * this.curveSideOffset;
         this.curveStartZ = this.getZ() - this.curveSideZ * this.curveSideOffset;
         this.arcInitialized = true;
      }
      double relX = this.getX() - this.curveStartX;
      double relZ = this.getZ() - this.curveStartZ;
      double forwardTravel = Math.max(0.0, relX * this.curveForwardX + relZ * this.curveForwardZ);
      double progress = Mth.clamp(forwardTravel / this.curveLength, 0.0, 1.0);
      double side = this.curveSideOffset * Math.cos(progress * Math.PI * 0.5);
      double nextX = this.curveStartX + this.curveForwardX * forwardTravel + this.curveSideX * side;
      double nextZ = this.curveStartZ + this.curveForwardZ * forwardTravel + this.curveSideZ * side;
      this.setPos(nextX, this.getY(), nextZ);
   }

   private void updatePoseFromMotion() {
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-6) {
         return;
      }
      double horizontal = motion.horizontalDistance();
      this.setYRot((float)(Mth.atan2(motion.x, motion.z) * 180.0F / (float)Math.PI));
      this.setXRot((float)(Mth.atan2(motion.y, horizontal) * 180.0F / (float)Math.PI));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   public void alignPoseToMotion() {
      updatePoseFromMotion();
   }

   private void sweepHit(Vec3 previousPosition) {
      Vec3 currentPosition = this.position();
      if (previousPosition.distanceToSqr(currentPosition) < 1.0E-4) {
         return;
      }
      AABB swept = new AABB(previousPosition, currentPosition).inflate(1.05, 0.8, 1.05);
      LivingEntity target = this.level().getEntitiesOfClass(LivingEntity.class, swept,
            living -> this.canHitEntity(living) && !this.hitEntities.contains(living.getId()))
         .stream()
         .min((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this)))
         .orElse(null);
      if (target != null) {
         this.onHitEntity(new EntityHitResult(target));
      }
   }

   private void destroySoftBlocksAhead() {
      if (!(this.level() instanceof ServerLevel serverLevel)) {
         return;
      }
      if (this.getDeltaMovement().lengthSqr() < 1.0E-4) {
         return;
      }
      for (double step = 0.2; step <= 1.4; step += 0.2) {
         BlockPos pos = BlockPos.containing(this.position().add(this.getDeltaMovement().normalize().scale(step)));
         BlockState state = serverLevel.getBlockState(pos);
         float hardness = state.getDestroySpeed(serverLevel, pos);
         if (!state.isAir() && !state.is(Blocks.BEDROCK) && hardness >= 0.0F && hardness <= 8.0F) {
            serverLevel.destroyBlock(pos, false, this.getOwner() instanceof LivingEntity living ? living : null);
         }
      }
   }

   private void destroyBreakthroughPath(Vec3 previousPosition) {
      if (!(this.level() instanceof ServerLevel serverLevel) || this.tickCount - this.lastBreakthroughTick < 3) {
         return;
      }
      Vec3 motion = this.getDeltaMovement();
      Vec3 currentPosition = this.position();
      Vec3 travel = currentPosition.subtract(previousPosition);
      Vec3 direction = motion.lengthSqr() > 1.0E-4 ? motion.normalize()
         : travel.lengthSqr() > 1.0E-4 ? travel.normalize()
         : this.getLookAngle();
      if (direction.lengthSqr() < 1.0E-4) {
         return;
      }
      this.lastBreakthroughTick = this.tickCount;
      double length = Math.max(2.5, Math.min(5.0, Math.max(motion.length(), travel.length()) + 1.8));
      TerrainImpactService.impactForwardBreakthrough(serverLevel, this.getOwner() instanceof LivingEntity living ? living : null,
         currentPosition, direction, TerrainImpactProfile.of(TerrainImpactProfile.Tier.MEDIUM), length, 2, 2);
   }

   private boolean exceededMaxFlightDistance() {
      if (this.maxDistanceSqr <= 0.0) {
         return false;
      }
      if (!this.maxDistanceInitialized) {
         this.maxDistanceOriginX = this.getX();
         this.maxDistanceOriginY = this.getY();
         this.maxDistanceOriginZ = this.getZ();
         this.maxDistanceInitialized = true;
         return false;
      }
      double dx = this.getX() - this.maxDistanceOriginX;
      double dy = this.getY() - this.maxDistanceOriginY;
      double dz = this.getZ() - this.maxDistanceOriginZ;
      return dx * dx + dy * dy + dz * dz > this.maxDistanceSqr;
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(FIXED_DAMAGE, tag.getFloat("FixedDamage"));
      this.entityData.set(BREAK_LOW_HARDNESS, tag.getBoolean("BreakLowHardness"));
      this.entityData.set(PIERCING_IMPACT, tag.getBoolean("PiercingImpact"));
      this.curveStartX = tag.getDouble("CurveStartX");
      this.curveStartZ = tag.getDouble("CurveStartZ");
      this.curveForwardX = tag.getDouble("CurveForwardX");
      this.curveForwardZ = tag.getDouble("CurveForwardZ");
      this.curveSideX = tag.getDouble("CurveSideX");
      this.curveSideZ = tag.getDouble("CurveSideZ");
      this.curveSideOffset = tag.getDouble("CurveSideOffset");
      this.curveLength = tag.getDouble("CurveLength");
      this.maxDistanceSqr = tag.getDouble("MaxFlightDistanceSqr");
      this.maxDistanceOriginX = tag.getDouble("MaxFlightOriginX");
      this.maxDistanceOriginY = tag.getDouble("MaxFlightOriginY");
      this.maxDistanceOriginZ = tag.getDouble("MaxFlightOriginZ");
      this.maxDistanceInitialized = tag.getBoolean("MaxFlightDistanceInitialized");
      this.arcInitialized = tag.getBoolean("CurveInitialized");
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("FixedDamage", this.entityData.get(FIXED_DAMAGE));
      tag.putBoolean("BreakLowHardness", this.entityData.get(BREAK_LOW_HARDNESS));
      tag.putBoolean("PiercingImpact", this.entityData.get(PIERCING_IMPACT));
      tag.putDouble("CurveStartX", this.curveStartX);
      tag.putDouble("CurveStartZ", this.curveStartZ);
      tag.putDouble("CurveForwardX", this.curveForwardX);
      tag.putDouble("CurveForwardZ", this.curveForwardZ);
      tag.putDouble("CurveSideX", this.curveSideX);
      tag.putDouble("CurveSideZ", this.curveSideZ);
      tag.putDouble("CurveSideOffset", this.curveSideOffset);
      tag.putDouble("CurveLength", this.curveLength);
      tag.putDouble("MaxFlightDistanceSqr", this.maxDistanceSqr);
      tag.putDouble("MaxFlightOriginX", this.maxDistanceOriginX);
      tag.putDouble("MaxFlightOriginY", this.maxDistanceOriginY);
      tag.putDouble("MaxFlightOriginZ", this.maxDistanceOriginZ);
      tag.putBoolean("MaxFlightDistanceInitialized", this.maxDistanceInitialized);
      tag.putBoolean("CurveInitialized", this.arcInitialized);
   }
}

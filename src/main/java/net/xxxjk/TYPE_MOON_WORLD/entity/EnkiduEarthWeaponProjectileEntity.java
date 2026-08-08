package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class EnkiduEarthWeaponProjectileEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Float> FIXED_DAMAGE = SynchedEntityData.defineId(EnkiduEarthWeaponProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(EnkiduEarthWeaponProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> HOMING_STRENGTH = SynchedEntityData.defineId(EnkiduEarthWeaponProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> GROUND_BORN = SynchedEntityData.defineId(EnkiduEarthWeaponProjectileEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> WEAPON_KIND = SynchedEntityData.defineId(EnkiduEarthWeaponProjectileEntity.class, EntityDataSerializers.INT);
   private final Set<Integer> hitEntities = new HashSet<>();
   private int brokenBlocks;
   private boolean heavyInteractions = true;

   public EnkiduEarthWeaponProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public EnkiduEarthWeaponProjectileEntity(Level level, LivingEntity shooter, ItemStack stack, float damage) {
      super(ModEntities.ENKIDU_EARTH_WEAPON.get(), shooter, level);
      this.setItem(stack);
      this.entityData.set(FIXED_DAMAGE, damage);
      this.setNoGravity(true);
   }

   public static EnkiduEarthWeaponProjectileEntity weapon(Level level, LivingEntity shooter, ItemStack stack, float damage, LivingEntity target, float homingStrength, boolean groundBorn) {
      EnkiduEarthWeaponProjectileEntity projectile = new EnkiduEarthWeaponProjectileEntity(level, shooter, stack, damage);
      projectile.configure(target, homingStrength, groundBorn, 0);
      return projectile;
   }

   public static EnkiduEarthWeaponProjectileEntity bow(Level level, LivingEntity shooter, ItemStack stack, float damage, LivingEntity target, boolean crossbow) {
      EnkiduEarthWeaponProjectileEntity projectile = new EnkiduEarthWeaponProjectileEntity(level, shooter, stack, damage);
      projectile.configure(target, 0.0F, true, crossbow ? 2 : 1);
      return projectile;
   }

   public void configure(LivingEntity target, float homingStrength, boolean groundBorn, int weaponKind) {
      this.entityData.set(TARGET_ID, target == null || EntityUtils.isImmunePlayerTarget(target) ? -1 : target.getId());
      this.entityData.set(HOMING_STRENGTH, homingStrength);
      this.entityData.set(GROUND_BORN, groundBorn);
      this.entityData.set(WEAPON_KIND, weaponKind);
   }

   public void setHeavyInteractions(boolean heavyInteractions) {
      this.heavyInteractions = heavyInteractions;
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FIXED_DAMAGE, 16.0F);
      builder.define(TARGET_ID, -1);
      builder.define(HOMING_STRENGTH, 0.0F);
      builder.define(GROUND_BORN, false);
      builder.define(WEAPON_KIND, 0);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.IRON_SWORD;
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      if (entity == null || entity == this.getOwner() || EntityUtils.isImmunePlayerTarget(entity)) {
         return false;
      }
      if (entity instanceof Projectile) {
         return super.canHitEntity(entity);
      }
      return entity instanceof LivingEntity living && (this.getOwner() == null || !living.isAlliedTo(this.getOwner())) && super.canHitEntity(entity);
   }

   @Override
   public void tick() {
      super.tick();
      tickGroundRiseAndHoming();
      updatePoseFromMotion();
      if (this.level() instanceof ServerLevel level) {
         if ((this.tickCount + this.getId() & 15) == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY(), this.getZ(), 1, 0.04, 0.04, 0.04, 0.01);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY(), this.getZ(), 1, 0.03, 0.03, 0.03, 0.01);
         }
         tickBowOrCrossbow(level);
         if (this.heavyInteractions && (this.tickCount + this.getId() & 3) == 0) {
            interceptNearbyProjectile(level);
         }
         if (this.heavyInteractions && (this.tickCount & 1) == 0) {
            breakLowHardnessBlocks(level);
         }
      }
      if (this.tickCount > 100) {
         this.discard();
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      if (this.level().isClientSide()) {
         return;
      }
      Entity hit = result.getEntity();
      if (hit instanceof Projectile projectile) {
         cancelProjectile(projectile);
         this.discard();
         return;
      }
      if (hit instanceof LivingEntity living && this.hitEntities.add(living.getId())) {
         DamageSource source = this.getOwner() instanceof LivingEntity owner ? this.damageSources().mobProjectile(this, owner) : this.damageSources().thrown(this, this);
         living.invulnerableTime = 0;
         living.hurt(source, this.entityData.get(FIXED_DAMAGE));
         living.invulnerableTime = 0;
      }
      this.discard();
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide() && result.getType() != HitResult.Type.ENTITY) {
         if (this.level() instanceof ServerLevel level) {
            breakLowHardnessBlocks(level, result.getLocation(), true);
         }
         if (this.brokenBlocks <= 0 || this.tickCount > 12) {
            this.discard();
         }
      }
   }

   private void breakLowHardnessBlocks(ServerLevel level) {
      if (this.tickCount < 14 || this.entityData.get(WEAPON_KIND) > 0 || this.brokenBlocks >= 6) {
         return;
      }
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() < 0.05) {
         return;
      }
      Vec3 dir = motion.normalize();
      for (double d = 0.25; d <= 1.35 && this.brokenBlocks < 6; d += 0.35) {
         breakBlockAt(level, this.position().add(dir.scale(d)));
      }
   }

   private void breakLowHardnessBlocks(ServerLevel level, Vec3 center, boolean impact) {
      int radius = impact ? 1 : 0;
      for (int dx = -radius; dx <= radius && this.brokenBlocks < 8; dx++) {
         for (int dy = -radius; dy <= radius && this.brokenBlocks < 8; dy++) {
            for (int dz = -radius; dz <= radius && this.brokenBlocks < 8; dz++) {
               breakBlockAt(level, center.add(dx, dy, dz));
            }
         }
      }
   }

   private boolean breakBlockAt(ServerLevel level, Vec3 sample) {
      BlockPos pos = BlockPos.containing(sample);
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      if (state.isAir()
         || state.hasBlockEntity()
         || state.is(Blocks.BEDROCK)
         || hardness < 0.0F
         || hardness > 4.0F
         || state.getExplosionResistance(level, pos, null) >= 60.0F) {
         return false;
      }
      if (level.removeBlock(pos, false)) {
         this.brokenBlocks++;
         level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.18, 0.18, 0.18, 0.025);
         if (this.brokenBlocks % 2 == 0) {
            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 0.35F, 1.55F);
         }
         return true;
      }
      return false;
   }

   private void interceptNearbyProjectile(ServerLevel level) {
      Entity owner = this.getOwner();
      Projectile matchlockBullet = level.getEntitiesOfClass(OdaMatchlockBulletEntity.class, this.getBoundingBox().inflate(1.65),
         p -> p.isAlive() && (owner == null || p.getOwner() == null || !owner.isAlliedTo(p.getOwner()))).stream().findFirst().orElse(null);
      if (matchlockBullet != null) {
         cancelProjectile(matchlockBullet);
         this.discard();
         return;
      }
      Projectile hostile = level.getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(0.6),
         p -> p != this && p.isAlive() && (owner == null || p.getOwner() == null || !owner.isAlliedTo(p.getOwner()))).stream().findFirst().orElse(null);
      if (hostile != null) {
         cancelProjectile(hostile);
         this.discard();
      }
   }

   private void cancelProjectile(Entity projectile) {
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CRIT, projectile.getX(), projectile.getY(), projectile.getZ(), 12, 0.25, 0.25, 0.25, 0.08);
         level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.55F, 1.8F);
      }
      projectile.discard();
   }

   private void updatePoseFromMotion() {
      var motion = this.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-6) {
         return;
      }
      double horizontal = motion.horizontalDistance();
      this.setYRot((float)(Mth.atan2(motion.x, motion.z) * 180.0F / Math.PI));
      this.setXRot((float)(Mth.atan2(motion.y, horizontal) * 180.0F / Math.PI));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   private void tickGroundRiseAndHoming() {
      int kind = this.entityData.get(WEAPON_KIND);
      Entity target = this.entityData.get(TARGET_ID) >= 0 ? this.level().getEntity(this.entityData.get(TARGET_ID)) : null;
      if (kind > 0) {
         if (!(target instanceof LivingEntity living) || !living.isAlive() || EntityUtils.isImmunePlayerTarget(living)) {
            this.entityData.set(TARGET_ID, -1);
            this.discard();
            return;
         }
         if (this.tickCount < 20) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x * 0.18, 0.135, motion.z * 0.18);
         } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.35));
         }
         return;
      }
      if (this.entityData.get(GROUND_BORN) && this.tickCount < 18) {
         Vec3 motion = this.getDeltaMovement();
         this.setDeltaMovement(motion.x * 0.18, 0.14, motion.z * 0.18);
         return;
      }
      float strength = this.entityData.get(HOMING_STRENGTH);
      if (strength <= 0.0F) {
         return;
      }
      if (!(target instanceof LivingEntity living) || !living.isAlive() || EntityUtils.isImmunePlayerTarget(living)) {
         this.entityData.set(TARGET_ID, -1);
         this.discard();
         return;
      }
      Vec3 aim = living.position().add(0.0, living.getBbHeight() * 0.55, 0.0).subtract(this.position());
      if (aim.lengthSqr() < 1.0E-4) {
         return;
      }
      double speed = Math.max(2.0, this.getDeltaMovement().length());
      Vec3 desired = aim.normalize().scale(speed);
      this.setDeltaMovement(this.getDeltaMovement().scale(1.0 - strength).add(desired.scale(strength)).normalize().scale(speed));
      this.hasImpulse = true;
   }

   private void tickBowOrCrossbow(ServerLevel level) {
      int kind = this.entityData.get(WEAPON_KIND);
      if (kind <= 0 || this.tickCount != 24) {
         return;
      }
      Entity targetEntity = this.entityData.get(TARGET_ID) >= 0 ? this.level().getEntity(this.entityData.get(TARGET_ID)) : null;
      if (!(targetEntity instanceof LivingEntity target) || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         this.entityData.set(TARGET_ID, -1);
         this.discard();
         return;
      }
      Entity owner = this.getOwner();
      Arrow arrow = owner instanceof LivingEntity livingOwner ? new Arrow(level, livingOwner, Items.ARROW.getDefaultInstance(), this.getItem()) : new Arrow(level, this.getX(), this.getY(), this.getZ(), Items.ARROW.getDefaultInstance(), this.getItem());
      arrow.setBaseDamage(Math.max(1.0, this.entityData.get(FIXED_DAMAGE)));
      arrow.setPos(this.getX(), this.getY() + 0.15, this.getZ());
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(arrow.position());
      arrow.shoot(aim.x, aim.y, aim.z, kind == 2 ? 3.0F : 2.45F, 0.0F);
      level.addFreshEntity(arrow);
      level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 10, 0.12, 0.12, 0.12, 0.04);
      level.playSound(null, this.getX(), this.getY(), this.getZ(), kind == 2 ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 0.9F, 1.35F);
      this.discard();
   }

   public void alignToMotion() {
      updatePoseFromMotion();
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(FIXED_DAMAGE, tag.getFloat("FixedDamage"));
      this.entityData.set(TARGET_ID, tag.getInt("TargetId"));
      this.entityData.set(HOMING_STRENGTH, tag.getFloat("HomingStrength"));
      this.entityData.set(GROUND_BORN, tag.getBoolean("GroundBorn"));
      this.entityData.set(WEAPON_KIND, tag.getInt("WeaponKind"));
      this.brokenBlocks = tag.getInt("BrokenBlocks");
      this.heavyInteractions = !tag.contains("HeavyInteractions") || tag.getBoolean("HeavyInteractions");
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("FixedDamage", this.entityData.get(FIXED_DAMAGE));
      tag.putInt("TargetId", this.entityData.get(TARGET_ID));
      tag.putFloat("HomingStrength", this.entityData.get(HOMING_STRENGTH));
      tag.putBoolean("GroundBorn", this.entityData.get(GROUND_BORN));
      tag.putInt("WeaponKind", this.entityData.get(WEAPON_KIND));
      tag.putInt("BrokenBlocks", this.brokenBlocks);
      tag.putBoolean("HeavyInteractions", this.heavyInteractions);
   }
}

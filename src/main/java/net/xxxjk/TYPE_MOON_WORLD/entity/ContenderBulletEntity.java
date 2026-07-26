package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class ContenderBulletEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Boolean> ORIGIN_BULLET = SynchedEntityData.defineId(ContenderBulletEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> VISUAL_ONLY = SynchedEntityData.defineId(ContenderBulletEntity.class, EntityDataSerializers.BOOLEAN);
   private static final int MAX_LIFE = 45;
   public final List<Vec3> tracePos = new LinkedList<>();

   public ContenderBulletEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public ContenderBulletEntity(Level level, LivingEntity owner, boolean originBullet) {
      super(ModEntities.CONTENDER_BULLET.get(), owner, level);
      this.setNoGravity(true);
      this.entityData.set(ORIGIN_BULLET, originBullet);
      this.setItem(new ItemStack(originBullet ? ModItems.ORIGIN_BULLET.get() : ModItems.BULLET.get()));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ORIGIN_BULLET, false);
      builder.define(VISUAL_ONLY, false);
   }

   @Override
   protected Item getDefaultItem() {
      // Called while Entity is still constructing, before this.entityData is assigned.
      return ModItems.BULLET.get();
   }

   public boolean isOriginBullet() {
      return this.entityData.get(ORIGIN_BULLET);
   }

   public ContenderBulletEntity setVisualOnly(boolean visualOnly) {
      this.entityData.set(VISUAL_ONLY, visualOnly);
      return this;
   }

   public boolean isVisualOnly() {
      return this.entityData.get(VISUAL_ONLY);
   }

   @Override
   public boolean isNoGravity() {
      return true;
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      if (this.isVisualOnly()) {
         return false;
      }
      if (entity == null || entity == this.getOwner() || EntityUtils.isImmunePlayerTarget(entity)) {
         return false;
      }
      if (this.getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && owner.isAlliedTo(living)) {
         return false;
      }
      return super.canHitEntity(entity);
   }

   @Override
   public void tick() {
      this.setNoGravity(true);
      super.tick();
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() > 1.0E-4) {
         this.setYRot((float)(Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
         this.setXRot((float)(Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG));
      }
      if (this.level().isClientSide) {
         captureTrace();
         this.level().addParticle(this.isOriginBullet() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      } else if (this.tickCount > MAX_LIFE) {
         this.discard();
      } else if (this.level() instanceof ServerLevel level && this.tickCount % 2 == 0) {
         Vec3 back = motion.lengthSqr() > 1.0E-4 ? motion.normalize().scale(-0.12) : Vec3.ZERO;
         Vec3 pos = this.position().add(back);
         level.sendParticles(this.isOriginBullet() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, 0.015, 0.015, 0.015, 0.0);
         level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity target) {
         target.invulnerableTime = 0;
         target.hurt(this.damageSources().thrown(this, this.getOwner()), 20.0F);
         target.invulnerableTime = 0;
         if (this.isOriginBullet()) {
            applyOriginEffectTo(target);
         }
         impact(result.getLocation(), true);
      }
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide && result.getType() != HitResult.Type.ENTITY) {
         impact(result.getLocation(), false);
      }
   }

   public void applyOriginEffectTo(LivingEntity target) {
      if (!target.isAlive()) {
         return;
      }
      if (target instanceof Player player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.is_magic_circuit_open) {
            player.invulnerableTime = 0;
            player.hurt(this.damageSources().thrown(this, this.getOwner()), (float)Math.max(0.0, vars.player_max_mana));
            player.invulnerableTime = 0;
         }
         if (player.isAlive()) {
            OriginBulletHelper.sealPlayerUntilDeath(player);
         }
      }
      if (target.isAlive() && OriginBulletHelper.isNpcSealed(target) == false) {
         OriginBulletHelper.sealNpc(target);
      }
      if (target.isAlive() && OriginBulletHelper.isServantTarget(target)) {
         float bonus = (float)Math.max(0.0, OriginBulletHelper.maxMpOf(target) / 10.0);
         if (bonus > 0.0F) {
            target.invulnerableTime = 0;
            target.hurt(this.damageSources().thrown(this, this.getOwner()), bonus);
            target.invulnerableTime = 0;
         }
      }
   }

   private void impact(Vec3 pos, boolean entityHit) {
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(this.isOriginBullet() ? ParticleTypes.SOUL : ParticleTypes.CRIT, pos.x, pos.y, pos.z, this.isOriginBullet() ? 18 : 8, 0.12, 0.12, 0.12, 0.03);
         level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, this.isOriginBullet() ? 12 : 5, 0.1, 0.1, 0.1, 0.02);
         level.playSound(null, pos.x, pos.y, pos.z, entityHit ? SoundEvents.ARROW_HIT_PLAYER : SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.7F, this.isOriginBullet() ? 0.65F : 1.2F);
      }
      this.discard();
   }

   private void captureTrace() {
      Vec3 pos = this.position();
      if (this.tracePos.isEmpty() || this.tracePos.get(this.tracePos.size() - 1).distanceToSqr(pos) >= 0.01) {
         this.tracePos.add(pos);
         while (this.tracePos.size() > 32) {
            this.tracePos.remove(0);
         }
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(ORIGIN_BULLET, tag.getBoolean("OriginBullet"));
      this.entityData.set(VISUAL_ONLY, tag.getBoolean("VisualOnly"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("OriginBullet", this.isOriginBullet());
      tag.putBoolean("VisualOnly", this.isVisualOnly());
   }
}

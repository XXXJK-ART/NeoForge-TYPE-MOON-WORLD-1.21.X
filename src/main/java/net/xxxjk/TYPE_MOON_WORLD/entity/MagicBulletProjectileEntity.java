package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.LinkedList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.player.MercurySwordMagicAmplifier;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.typemoonworld.api.MagicComplexity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public class MagicBulletProjectileEntity extends ThrowableItemProjectile {
   public static final int ELEMENT_NONE = 0;
   public static final int ELEMENT_FIRE = 1;
   public static final int ELEMENT_WATER = 2;
   public static final int ELEMENT_EARTH = 3;
   public static final int ELEMENT_WIND = 4;
   private static final EntityDataAccessor<Integer> ELEMENT = SynchedEntityData.defineId(MagicBulletProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> VISUAL_SCALE = SynchedEntityData.defineId(MagicBulletProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final DustParticleOptions ARCANE_DUST = new DustParticleOptions(new Vector3f(0.48F, 0.24F, 1.0F), 1.0F);
   private static final DustParticleOptions WATER_DUST = new DustParticleOptions(new Vector3f(0.15F, 0.45F, 1.0F), 1.0F);
   private static final DustParticleOptions EARTH_DUST = new DustParticleOptions(new Vector3f(0.42F, 0.28F, 0.12F), 1.0F);
   private static final DustParticleOptions WIND_DUST = new DustParticleOptions(new Vector3f(0.65F, 1.0F, 0.78F), 1.0F);
   public final List<Vec3> tracePos = new LinkedList<>();
   private float magicDamage = 3.0F;
   private float slowPercent = 0.0F;
   private double maxRange = 15.0;
   private Vec3 originPos = Vec3.ZERO;
   private String sourceMagicId = "magic_bullet";
   private double casterProficiency = 0.0;

   public MagicBulletProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public MagicBulletProjectileEntity(Level level, LivingEntity shooter) {
      super(ModEntities.MAGIC_BULLET_PROJECTILE.get(), shooter, level);
      this.originPos = shooter.position();
      this.setPos(EntityUtils.getRightHandCastAnchor(shooter));
   }

   public MagicBulletProjectileEntity(Level level, double x, double y, double z) {
      super(ModEntities.MAGIC_BULLET_PROJECTILE.get(), x, y, z, level);
      this.originPos = new Vec3(x, y, z);
   }

   protected Item getDefaultItem() {
      return ModItems.MAGIC_FRAGMENTS.get();
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ELEMENT, ELEMENT_NONE);
      builder.define(VISUAL_SCALE, 0.65F);
   }

   public void configure(float damage, float slowPercent, double range, int element, float visualScale) {
      this.magicDamage = Math.max(0.0F, damage);
      this.slowPercent = Math.max(0.0F, Math.min(0.4F, slowPercent));
      this.maxRange = Math.max(4.0, range);
      this.entityData.set(ELEMENT, Math.max(ELEMENT_NONE, Math.min(ELEMENT_WIND, element)));
      this.entityData.set(VISUAL_SCALE, Math.max(0.2F, visualScale));
      this.originPos = this.position();
   }

   public void setMagicSource(String magicId, double proficiency) {
      this.sourceMagicId = magicId == null || magicId.isBlank() ? "magic_bullet" : magicId;
      this.casterProficiency = Math.max(0.0, Math.min(100.0, proficiency));
   }

   public int getElement() {
      return this.entityData.get(ELEMENT);
   }

   public float getVisualScale() {
      return this.entityData.get(VISUAL_SCALE);
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide || this.tickCount % 2 == 0) {
         spawnTrailParticles();
      }
      if (!this.level().isClientSide && (this.tickCount > 80 || this.position().distanceToSqr(this.originPos) > this.maxRange * this.maxRange)) {
         this.discard();
      }

      if (this.level().isClientSide) {
         Vec3 pos = this.position();
         if (this.tracePos.isEmpty() || pos.distanceToSqr(this.tracePos.get(this.tracePos.size() - 1)) >= 0.01) {
            this.tracePos.add(pos);
            if (this.tracePos.size() > 32) {
               this.tracePos.remove(0);
            }
         }
      }
   }

   protected boolean canHitEntity(Entity entity) {
      return entity != null && entity != this.getOwner() && super.canHitEntity(entity);
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide) {
         Entity entity = result.getEntity();
         if (entity instanceof LivingEntity target && !EntityUtils.isImmunePlayerTarget(target)) {
            float damage = this.magicDamage;
            if (this.getOwner() instanceof LivingEntity owner) {
               damage = MercurySwordMagicAmplifier.amplifyDamage(owner, damage);
            }
            damage = MagicResistanceHelper.applyMagicDamageReduction(
               target, this.damageSources().magic(), damage, MagicComplexity.SIMPLE_ACTION,
               this.getOwner() instanceof LivingEntity owner ? owner : null, this.sourceMagicId, this.casterProficiency);
            target.invulnerableTime = 0;
            target.hurt(this.damageSources().magic(), damage);
            target.invulnerableTime = 0;
            int amplifier = this.slowPercent >= 0.3F ? 1 : 0;
            int duration = MagicResistanceHelper.applyHarmfulMagicEffectResistance(
               target, 60, MagicComplexity.SIMPLE_ACTION,
               this.getOwner() instanceof LivingEntity owner ? owner : null, this.sourceMagicId, this.casterProficiency);
            if (this.slowPercent > 0.0F && duration > 0) {
               target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier, false, true, true));
            }
            if (duration > 0 || damage > 0.0F) {
               applyElementEffect(target);
            }
            spawnImpactParticles(this.position());
         }
         this.discard();
      }
   }

   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide && !this.isRemoved()) {
         spawnImpactParticles(result.getLocation());
         this.discard();
      }
   }

   private void applyElementEffect(LivingEntity target) {
      int element = getElement();
      int duration = MagicResistanceHelper.applyDebuffResistance(target, 60);
      if (element == ELEMENT_FIRE) {
         target.igniteForSeconds(3.0F);
      } else if (element == ELEMENT_WATER) {
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration + 20, 1, false, true, true));
      } else if (element == ELEMENT_EARTH) {
         target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
      } else if (element == ELEMENT_WIND) {
         Vec3 motion = this.getDeltaMovement();
         if (motion.lengthSqr() > 1.0E-6) {
            target.setDeltaMovement(target.getDeltaMovement().add(motion.normalize().scale(0.45)));
            target.hurtMarked = true;
         }
      }
   }

   private void spawnTrailParticles() {
      ParticleOptions particle = particleForElement(getElement());
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(particle, this.getX(), this.getY(), this.getZ(), 2, 0.04, 0.04, 0.04, 0.0);
      } else {
         this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      }
   }

   private void spawnImpactParticles(Vec3 pos) {
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(particleForElement(getElement()), pos.x, pos.y, pos.z, 18, 0.18, 0.18, 0.18, 0.02);
         level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 6, 0.12, 0.12, 0.12, 0.02);
      }
   }

   private static ParticleOptions particleForElement(int element) {
      return switch (element) {
         case ELEMENT_FIRE -> ParticleTypes.FLAME;
         case ELEMENT_WATER -> WATER_DUST;
         case ELEMENT_EARTH -> EARTH_DUST;
         case ELEMENT_WIND -> WIND_DUST;
         default -> ARCANE_DUST;
      };
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putString("TypeMoonSourceMagicId", this.sourceMagicId);
      tag.putDouble("TypeMoonCasterProficiency", this.casterProficiency);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("TypeMoonSourceMagicId")) {
         this.sourceMagicId = tag.getString("TypeMoonSourceMagicId");
      }
      if (tag.contains("TypeMoonCasterProficiency")) {
         this.casterProficiency = Math.max(0.0, Math.min(100.0, tag.getDouble("TypeMoonCasterProficiency")));
      }
   }
}

package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Server-authoritative Gate of Babylon projectile. The client only needs the tracked position. */
public class GilgameshGateWeaponProjectileEntity extends Entity implements GeoEntity {
   private static final EntityDataAccessor<String> WEAPON_ID = SynchedEntityData.defineId(GilgameshGateWeaponProjectileEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Integer> SOURCE_STYLE = SynchedEntityData.defineId(GilgameshGateWeaponProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<String> DUEL_TOKEN = SynchedEntityData.defineId(GilgameshGateWeaponProjectileEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Integer> LAUNCH_DELAY = SynchedEntityData.defineId(GilgameshGateWeaponProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> EMPOWERED = SynchedEntityData.defineId(GilgameshGateWeaponProjectileEntity.class, EntityDataSerializers.BOOLEAN);
   private static final DustParticleOptions VAJRA_PURPLE = new DustParticleOptions(new Vector3f(0.62F, 0.12F, 1.0F), 1.45F);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private UUID ownerUuid;
   private float damage = 18.0F;
   private final Set<Integer> hit = new HashSet<>();

   public GilgameshGateWeaponProjectileEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noCulling = true;
      this.setNoGravity(true);
   }

   public GilgameshGateWeaponProjectileEntity(Level level, LivingEntity owner, Vec3 start, Vec3 direction, String weaponId, float damage) {
      this(ModEntities.GILGAMESH_GATE_PROJECTILE.get(), level);
      this.ownerUuid = owner.getUUID();
      this.entityData.set(WEAPON_ID, weaponId == null ? "durandal" : weaponId);
      this.damage = damage;
      this.setPos(start);
      this.setDeltaMovement(direction.normalize().scale(1.9));
   }

   public String getWeaponId() { return this.entityData.get(WEAPON_ID); }
   public int getSourceStyle() { return this.entityData.get(SOURCE_STYLE); }
   public void setSourceStyle(int style) { this.entityData.set(SOURCE_STYLE, style); }
   public void setDuelToken(String token) { this.entityData.set(DUEL_TOKEN, token == null ? "" : token); }
   public void setLaunchDelay(int ticks) { this.entityData.set(LAUNCH_DELAY, Math.max(0, ticks)); }
   public void setEmpowered(boolean empowered) { this.entityData.set(EMPOWERED, empowered); }
   public int getLaunchDelay() { return this.entityData.get(LAUNCH_DELAY); }
   public float getSummonProgress(float partialTick) {
      int delay = this.getLaunchDelay();
      if (delay <= 0) return 1.0F;
      return net.minecraft.util.Mth.clamp((this.tickCount + partialTick) / Math.max(1.0F, delay - 2.0F), 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 old = this.position();
      Vec3 next = old.add(this.getDeltaMovement());
      if (this.level() instanceof net.minecraft.server.level.ServerLevel level) {
         LivingEntity owner = getOwner(level);
         int delay = this.entityData.get(LAUNCH_DELAY);
         int lifetime = (this.entityData.get(DUEL_TOKEN).isBlank() ? 40 : 22) + delay;
         if (owner == null || !owner.isAlive() || this.tickCount > lifetime) {
            if (owner != null && this.entityData.get(DUEL_TOKEN).isBlank() && "vajra".equals(this.getWeaponId())) {
               explodeVajra(level, owner, this.position());
            }
            this.discard();
            return;
         }
         if (this.tickCount <= delay) {
            if (this.tickCount % 4 == 0) {
               level.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY(), this.getZ(), 3, 0.45, 0.45, 0.45, 0.025);
               level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 2, 0.22, 0.22, 0.22, 0.012);
            }
            return;
         }
         String token = this.entityData.get(DUEL_TOKEN);
         if (!token.isBlank()) {
            GilgameshGateWeaponProjectileEntity counterpart = level.getEntitiesOfClass(GilgameshGateWeaponProjectileEntity.class,
               this.getBoundingBox().inflate(1.4), p -> p != this && p.isAlive() && token.equals(p.entityData.get(DUEL_TOKEN))
                  && p.getSourceStyle() != this.getSourceStyle()).stream().findFirst().orElse(null);
            if (counterpart != null) {
               level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 14, 0.22, 0.22, 0.22, 0.08);
               counterpart.discard();
               this.discard();
               return;
            }
         }
         if (!token.isBlank()) {
            this.setPos(next);
            return;
         }
         HitResult blockHit = level.clip(new ClipContext(old, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
         if (blockHit.getType() == HitResult.Type.BLOCK) {
            if ("vajra".equals(this.getWeaponId())) {
               explodeVajra(level, owner, blockHit.getLocation());
            } else {
               spawnImpactParticles(level, blockHit.getLocation());
            }
            this.discard();
            return;
         }
         AABB box = new AABB(old, next).inflate(0.65);
         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != owner && !e.isAlliedTo(owner) && !EntityUtils.isImmunePlayerTarget(e))) {
            if (this.hit.add(target.getId())) {
               DamageSource source = owner.damageSources().mobProjectile(this, owner);
               target.invulnerableTime = 0;
               target.hurt(source, this.damage);
               target.invulnerableTime = 0;
               applyWeaponEffect(level, owner, target);
               this.discard();
               break;
            }
         }
         spawnTrail(level);
      }
      this.setPos(next);
   }

   private LivingEntity getOwner(net.minecraft.server.level.ServerLevel level) {
      if (this.ownerUuid == null) return null;
      Entity e = level.getEntity(this.ownerUuid);
      return e instanceof LivingEntity living ? living : null;
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      builder.define(WEAPON_ID, "durandal");
      builder.define(SOURCE_STYLE, 0);
      builder.define(DUEL_TOKEN, "");
      builder.define(LAUNCH_DELAY, 0);
      builder.define(EMPOWERED, false);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) this.ownerUuid = tag.getUUID("Owner");
      this.entityData.set(WEAPON_ID, tag.contains("Weapon") ? tag.getString("Weapon") : "durandal");
      this.entityData.set(SOURCE_STYLE, tag.getInt("SourceStyle"));
      this.entityData.set(DUEL_TOKEN, tag.getString("DuelToken"));
      this.entityData.set(LAUNCH_DELAY, tag.getInt("LaunchDelay"));
      this.entityData.set(EMPOWERED, tag.getBoolean("Empowered"));
      this.damage = tag.contains("Damage") ? tag.getFloat("Damage") : 18.0F;
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
      tag.putString("Weapon", this.getWeaponId());
      tag.putInt("SourceStyle", this.getSourceStyle());
      tag.putString("DuelToken", this.entityData.get(DUEL_TOKEN));
      tag.putInt("LaunchDelay", this.entityData.get(LAUNCH_DELAY));
      tag.putBoolean("Empowered", this.entityData.get(EMPOWERED));
      tag.putFloat("Damage", this.damage);
   }

   private void spawnTrail(net.minecraft.server.level.ServerLevel level) {
      if ("vajra".equals(this.getWeaponId())) {
         level.sendParticles(VAJRA_PURPLE, this.getX(), this.getY(), this.getZ(), this.entityData.get(EMPOWERED) ? 18 : 5, 0.22, 0.22, 0.22, 0.03);
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), this.entityData.get(EMPOWERED) ? 14 : 3, 0.28, 0.28, 0.28, 0.12);
         level.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY(), this.getZ(), this.entityData.get(EMPOWERED) ? 8 : 2, 0.18, 0.18, 0.18, 0.02);
      } else if ("gram".equals(this.getWeaponId())) {
         level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 4, 0.08, 0.08, 0.08, 0.02);
      } else {
         level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 2, 0.04, 0.04, 0.04, 0.01);
      }
   }

   private void applyWeaponEffect(net.minecraft.server.level.ServerLevel level, LivingEntity owner, LivingEntity target) {
      boolean empowered = this.entityData.get(EMPOWERED);
      switch (this.getWeaponId()) {
         case "durandal" -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, empowered ? 100 : 30, empowered ? 2 : 0, false, true, true));
            level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), empowered ? 42 : 10, 0.55, 0.65, 0.55, 0.05);
            if (empowered) hurtWithoutIFrames(target, owner.damageSources().magic(), 30.0F);
         }
         case "gram" -> {
            target.igniteForSeconds(empowered ? 8.0F : 2.0F);
            if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.DRAGON)) {
               hurtWithoutIFrames(target, owner.damageSources().magic(), empowered ? 70.0F : 12.0F);
            }
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), empowered ? 48 : 10, 0.65, 0.7, 0.65, 0.08);
         }
         case "harpe" -> {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, empowered ? 200 : 60, 0, false, true, true));
            if (empowered) {
               target.removeEffect(MobEffects.REGENERATION);
               target.removeEffect(MobEffects.ABSORPTION);
               if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.UNDEAD)) {
                  hurtWithoutIFrames(target, owner.damageSources().magic(), 40.0F);
               }
            }
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), empowered ? 32 : 8, 0.45, 0.55, 0.45, 0.08);
         }
         case "vajra" -> {
            explodeVajra(level, owner, target.position().add(0, target.getBbHeight() * 0.45, 0));
         }
         case "fangtian_huaji" -> {
            Vec3 push = this.getDeltaMovement().normalize();
            target.push(push.x * (empowered ? 2.2 : 0.7), empowered ? 0.55 : 0.15, push.z * (empowered ? 2.2 : 0.7));
            if (empowered) {
               for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.5),
                  e -> e.isAlive() && e != owner && e != target && !e.isAlliedTo(owner) && !EntityUtils.isImmunePlayerTarget(e))) {
                  hurtWithoutIFrames(nearby, owner.damageSources().mobProjectile(this, owner), 50.0F);
                  nearby.push(push.x * 1.2, 0.25, push.z * 1.2);
               }
               level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 20, 1.2, 0.45, 1.2, 0.0);
            }
         }
         default -> { }
      }
   }

   private void explodeVajra(net.minecraft.server.level.ServerLevel level, LivingEntity owner, Vec3 center) {
      boolean empowered = this.entityData.get(EMPOWERED);
      if (empowered) this.entityData.set(EMPOWERED, false);
      double radius = empowered ? 10.0 : 2.0;
      DamageSource source = owner.damageSources().mobProjectile(this, owner);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius)),
         e -> e.isAlive() && e != owner && !e.isAlliedTo(owner) && !EntityUtils.isImmunePlayerTarget(e) && e.distanceToSqr(center) <= radius * radius)) {
         double normalized = Math.min(1.0, Math.sqrt(target.distanceToSqr(center)) / radius);
         float damage = empowered ? (float)(110.0 - 55.0 * normalized) : 12.0F;
         hurtWithoutIFrames(target, source, damage);
         Vec3 push = target.position().subtract(center).normalize().scale(empowered ? 1.25 : 0.45);
         target.push(push.x, empowered ? 0.45 : 0.12, push.z);
      }
      level.sendParticles(VAJRA_PURPLE, center.x, center.y, center.z, empowered ? 220 : 42, radius * 0.48, radius * 0.48, radius * 0.48, empowered ? 0.24 : 0.1);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, empowered ? 180 : 34, radius * 0.6, radius * 0.6, radius * 0.6, empowered ? 0.32 : 0.14);
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, empowered ? 8 : 2, 0.6, 0.6, 0.6, 0.0);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, empowered ? 3 : 1, 0.45, 0.45, 0.45, 0.0);
      level.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.PLAYERS, empowered ? 2.5F : 1.0F, 1.25F);
      level.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, empowered ? 1.8F : 0.65F, 0.85F);
      DeferredTerrainDestruction.queueSphere(level, center, empowered ? 10 : 2, empowered ? 80.0F : 20.0F, empowered ? 12 : 4);
   }

   private static void spawnImpactParticles(net.minecraft.server.level.ServerLevel level, Vec3 center) {
      level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 18, 0.3, 0.3, 0.3, 0.08);
   }

   private static void hurtWithoutIFrames(LivingEntity target, DamageSource source, float amount) {
      target.invulnerableTime = 0;
      target.hurt(source, amount);
      target.invulnerableTime = 0;
   }

   @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}

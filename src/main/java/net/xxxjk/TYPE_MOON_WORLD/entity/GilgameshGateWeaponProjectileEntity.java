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
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Server-authoritative Gate of Babylon projectile. The client only needs the tracked position. */
public class GilgameshGateWeaponProjectileEntity extends Entity implements GeoEntity {
   public static final int SOURCE_STYLE_GILGAMESH = 0;
   public static final int SOURCE_STYLE_EMIYA = 1;
   public static final int SOURCE_STYLE_ENKIDU = 2;
   public static final int SOURCE_STYLE_LANCELOT = 3;
   private static final EntityDataAccessor<String> WEAPON_ID = SynchedEntityData.defineId(GilgameshGateWeaponProjectileEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Integer> SOURCE_STYLE = SynchedEntityData.defineId(GilgameshGateWeaponProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<String> DUEL_TOKEN = SynchedEntityData.defineId(GilgameshGateWeaponProjectileEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Integer> LAUNCH_DELAY = SynchedEntityData.defineId(GilgameshGateWeaponProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> EMPOWERED = SynchedEntityData.defineId(GilgameshGateWeaponProjectileEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> HOMING_TARGET_ID = SynchedEntityData.defineId(GilgameshGateWeaponProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> EFFECT_STRIDE = SynchedEntityData.defineId(GilgameshGateWeaponProjectileEntity.class, EntityDataSerializers.INT);
   private static final DustParticleOptions VAJRA_PURPLE = new DustParticleOptions(new Vector3f(0.62F, 0.12F, 1.0F), 1.45F);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private UUID ownerUuid;
   private UUID homingTargetUuid;
   private boolean requireHomingTarget;
   private float damage = 18.0F;
   private final Set<Integer> hit = new HashSet<>();

   public GilgameshGateWeaponProjectileEntity(EntityType<?> type, Level level) {
      super(type, level);
      // Gate entities are numerous during a release. Let the normal frustum
      // and distance checks discard off-screen models, especially with a GUI open.
      this.noCulling = false;
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
   public float getDamage() { return this.damage; }
   public boolean isEmpowered() { return this.entityData.get(EMPOWERED); }
   public boolean isKnightOfOwnerCounter() { return this.getSourceStyle() == SOURCE_STYLE_LANCELOT; }
   /** Resolves the living owner for AI threat classification on the server. */
   public LivingEntity getOwnerEntity() {
      return this.level() instanceof net.minecraft.server.level.ServerLevel level ? getOwner(level) : null;
   }
   public int getSourceStyle() { return this.entityData.get(SOURCE_STYLE); }
   public void setSourceStyle(int style) { this.entityData.set(SOURCE_STYLE, style); }
   public void setDuelToken(String token) { this.entityData.set(DUEL_TOKEN, token == null ? "" : token); }
   public void setLaunchDelay(int ticks) { this.entityData.set(LAUNCH_DELAY, Math.max(0, ticks)); }
   public void setEffectStride(int stride) { this.entityData.set(EFFECT_STRIDE, Math.max(1, stride)); }
   public void setEmpowered(boolean empowered) { this.entityData.set(EMPOWERED, empowered); }
   public void setHomingTarget(LivingEntity target) {
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         this.homingTargetUuid = null;
         this.entityData.set(HOMING_TARGET_ID, 0);
         return;
      }
      this.homingTargetUuid = target.getUUID();
      this.requireHomingTarget = true;
      this.entityData.set(HOMING_TARGET_ID, target.getId());
   }
   public int getHomingTargetId() { return this.entityData.get(HOMING_TARGET_ID); }
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
      if (!(this.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
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
         if (this.tickCount == 1) {
            String duelToken = this.entityData.get(DUEL_TOKEN);
            int effectStride = this.entityData.get(EFFECT_STRIDE);
            boolean showDuelFx = duelToken.isBlank()
               ? Math.floorMod(this.getId(), effectStride) == 0
               : (duelToken.hashCode() & 3) == 0;
            if (showDuelFx) {
               if (this.getSourceStyle() == SOURCE_STYLE_ENKIDU) {
                  VFXServerEffects.spawn(level, "servant_enkidu_age_of_babylon_gate", this.position(), 160.0);
               } else if (this.getSourceStyle() == SOURCE_STYLE_EMIYA) {
                  // Emiya's counter projectiles are projections, not golden
                  // Gate of Babylon portals.
                  VFXServerEffects.spawnOriented(level, "servant_emiya_projection",
                     this.position().subtract(this.getDeltaMovement().normalize().scale(0.45)), this.getDeltaMovement(), 160.0);
               } else if (this.getSourceStyle() == SOURCE_STYLE_GILGAMESH) {
                  VFXServerEffects.spawnOriented(level, "gilgamesh_gate",
                     this.position().subtract(this.getDeltaMovement().normalize().scale(0.45)), this.getDeltaMovement(), 160.0);
               }
            }
         }
         if (this.tickCount <= delay) {
            return;
         }
         String token = this.entityData.get(DUEL_TOKEN);
         if (token.isBlank()) updateHomingTarget(level);
         if (this.isRemoved()) return;
         next = old.add(this.getDeltaMovement());
         if (!token.isBlank()) {
            GilgameshGateWeaponProjectileEntity counterpart = level.getEntitiesOfClass(GilgameshGateWeaponProjectileEntity.class,
               this.getBoundingBox().inflate(2.4), p -> p != this && p.isAlive() && token.equals(p.entityData.get(DUEL_TOKEN))
                  && p.getSourceStyle() != this.getSourceStyle()).stream().findFirst().orElse(null);
            if (counterpart != null) {
               Vec3 impact = this.position().lerp(counterpart.position(), 0.5);
               if ((token.hashCode() & 3) == 0) {
                  level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 2, 0.22, 0.22, 0.22, 0.02);
                  level.sendParticles(ParticleTypes.FLASH, impact.x, impact.y, impact.z, 1, 0.04, 0.04, 0.04, 0.0);
                  level.sendParticles(ParticleTypes.CRIT, impact.x, impact.y, impact.z, 8, 0.34, 0.34, 0.34, 0.12);
                  level.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z, 4, 0.26, 0.26, 0.26, 0.06);
               }
               if ((token.hashCode() & 15) == 0) {
                  level.playSound(null, impact.x, impact.y, impact.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                     net.minecraft.sounds.SoundSource.HOSTILE, 0.75F, 1.65F);
               }
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
            e -> e.isAlive() && e != owner && !ServantMasterTargeting.isContractMaster(owner, e)
               && !e.isAlliedTo(owner) && !EntityUtils.isImmunePlayerTarget(e))) {
            if (this.hit.add(target.getId())) {
               DamageSource source = owner.damageSources().mobProjectile(this, owner);
               Vec3 movementBeforeHit = target.getDeltaMovement();
               boolean empoweredHit = this.entityData.get(EMPOWERED);
               target.invulnerableTime = 0;
               target.hurt(source, this.damage);
               target.invulnerableTime = 0;
               applyWeaponEffect(level, owner, target);
               if (!empoweredHit) {
                  target.setDeltaMovement(movementBeforeHit);
                  target.hurtMarked = true;
               }
               this.discard();
               break;
            }
         }
         if ((this.tickCount & 1) == 0) {
            int effectStride = this.entityData.get(EFFECT_STRIDE);
            boolean showTrail = token.isBlank()
               ? Math.floorMod(this.getId(), effectStride) == 0
               : (token.hashCode() & 3) == 0;
            if (showTrail) spawnTrail(level);
         }
      this.setPos(next);
   }

   private LivingEntity getOwner(net.minecraft.server.level.ServerLevel level) {
      if (this.ownerUuid == null) return null;
      Entity e = level.getEntity(this.ownerUuid);
      return e instanceof LivingEntity living ? living : null;
   }

   private void updateHomingTarget(net.minecraft.server.level.ServerLevel level) {
      Entity entity = this.entityData.get(HOMING_TARGET_ID) == 0 ? null : level.getEntity(this.entityData.get(HOMING_TARGET_ID));
      LivingEntity target = entity instanceof LivingEntity living ? living : null;
      if ((target == null || !target.isAlive()) && this.homingTargetUuid != null) {
         Entity resolved = level.getEntity(this.homingTargetUuid);
         target = resolved instanceof LivingEntity living ? living : null;
         if (target != null) this.entityData.set(HOMING_TARGET_ID, target.getId());
      }
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         this.homingTargetUuid = null;
         this.entityData.set(HOMING_TARGET_ID, 0);
         if (this.requireHomingTarget) {
            this.discard();
         }
         return;
      }
      LivingEntity owner = getOwner(level);
      if (owner != null && (target == owner || target.isAlliedTo(owner)
         || ServantMasterTargeting.isContractMaster(owner, target))) {
         this.homingTargetUuid = null;
         this.entityData.set(HOMING_TARGET_ID, 0);
         this.discard();
         return;
      }
      Vec3 targetPoint = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0)
         .add(target.getDeltaMovement().scale(Math.min(2.5, Math.max(0.35, this.distanceTo(target) / 8.0))));
      Vec3 desired = targetPoint.subtract(this.position());
      if (desired.lengthSqr() < 1.0E-6) return;
      double speed = Math.max(1.35, this.getDeltaMovement().length());
      Vec3 current = this.getDeltaMovement().lengthSqr() < 1.0E-6 ? desired.normalize() : this.getDeltaMovement().normalize();
      Vec3 steered = current.scale(0.78).add(desired.normalize().scale(0.22));
      if (steered.lengthSqr() > 1.0E-6) this.setDeltaMovement(steered.normalize().scale(speed));
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      builder.define(WEAPON_ID, "durandal");
      builder.define(SOURCE_STYLE, 0);
      builder.define(DUEL_TOKEN, "");
      builder.define(LAUNCH_DELAY, 0);
      builder.define(EMPOWERED, false);
      builder.define(HOMING_TARGET_ID, 0);
      builder.define(EFFECT_STRIDE, 1);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) this.ownerUuid = tag.getUUID("Owner");
      this.entityData.set(WEAPON_ID, tag.contains("Weapon") ? tag.getString("Weapon") : "durandal");
      this.entityData.set(SOURCE_STYLE, tag.getInt("SourceStyle"));
      this.entityData.set(DUEL_TOKEN, tag.getString("DuelToken"));
      this.entityData.set(LAUNCH_DELAY, tag.getInt("LaunchDelay"));
      this.entityData.set(EMPOWERED, tag.getBoolean("Empowered"));
      this.entityData.set(HOMING_TARGET_ID, tag.getInt("HomingTargetId"));
      this.entityData.set(EFFECT_STRIDE, Math.max(1, tag.getInt("EffectStride")));
      if (tag.hasUUID("HomingTarget")) this.homingTargetUuid = tag.getUUID("HomingTarget");
      this.damage = tag.contains("Damage") ? tag.getFloat("Damage") : 18.0F;
      this.requireHomingTarget = tag.hasUUID("HomingTarget");
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
      tag.putString("Weapon", this.getWeaponId());
      tag.putInt("SourceStyle", this.getSourceStyle());
      tag.putString("DuelToken", this.entityData.get(DUEL_TOKEN));
      tag.putInt("LaunchDelay", this.entityData.get(LAUNCH_DELAY));
      tag.putBoolean("Empowered", this.entityData.get(EMPOWERED));
      tag.putInt("HomingTargetId", this.entityData.get(HOMING_TARGET_ID));
      tag.putInt("EffectStride", this.entityData.get(EFFECT_STRIDE));
      if (this.homingTargetUuid != null) tag.putUUID("HomingTarget", this.homingTargetUuid);
      tag.putFloat("Damage", this.damage);
   }

   private void spawnTrail(net.minecraft.server.level.ServerLevel level) {
      if (this.isKnightOfOwnerCounter()) {
         level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 5, 0.14, 0.14, 0.14, 0.025);
         level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(), this.getZ(), 2, 0.08, 0.08, 0.08, 0.01);
         return;
      }
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
            if (empowered) {
               Vec3 push = this.getDeltaMovement().normalize();
               target.push(push.x * 2.2, 0.55, push.z * 2.2);
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
         Vec3 movementBeforeHit = target.getDeltaMovement();
         hurtWithoutIFrames(target, source, damage);
         if (empowered) {
            Vec3 push = target.position().subtract(center).normalize().scale(1.25);
            target.push(push.x, 0.45, push.z);
         } else {
            target.setDeltaMovement(movementBeforeHit);
            target.hurtMarked = true;
         }
      }
      level.sendParticles(VAJRA_PURPLE, center.x, center.y, center.z, empowered ? 220 : 42, radius * 0.48, radius * 0.48, radius * 0.48, empowered ? 0.24 : 0.1);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, empowered ? 180 : 34, radius * 0.6, radius * 0.6, radius * 0.6, empowered ? 0.32 : 0.14);
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, empowered ? 8 : 2, 0.6, 0.6, 0.6, 0.0);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, empowered ? 3 : 1, 0.45, 0.45, 0.45, 0.0);
      level.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.PLAYERS, empowered ? 2.5F : 1.0F, 1.25F);
      level.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, empowered ? 1.8F : 0.65F, 0.85F);
      queueVajraTerrainFromCenter(level, center, empowered ? 10 : 2, empowered ? 80.0F : 20.0F);
   }

   private static void queueVajraTerrainFromCenter(net.minecraft.server.level.ServerLevel level, Vec3 center, int radius, float maxHardness) {
      for (int wave = 1; wave <= radius; wave++) {
         int currentWave = wave;
         net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(currentWave - 1, () -> {
            double inner = Math.max(0.0, currentWave - 1.0);
            DeferredTerrainDestruction.queueShell(level, center, currentWave, inner, 8,
               (serverLevel, pos, distanceSqr, shellRadius, origin) -> {
                  if (!serverLevel.hasChunkAt(pos) || serverLevel.getBlockEntity(pos) != null) return false;
                  var state = serverLevel.getBlockState(pos);
                  float hardness = state.getDestroySpeed(serverLevel, pos);
                  return !state.isAir() && !state.is(net.minecraft.world.level.block.Blocks.BEDROCK)
                     && hardness >= 0.0F && hardness <= maxHardness
                     && state.getExplosionResistance(serverLevel, pos, null) < 1200.0F;
               }, null);
         });
      }
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

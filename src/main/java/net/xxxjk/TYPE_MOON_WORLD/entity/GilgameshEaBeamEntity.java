package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshNoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.BeamClashManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.BeamClashParticipant;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.BeamType;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshDuelState;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Server-owned continuous EA charge/release state machine. */
public class GilgameshEaBeamEntity extends Entity implements BeamClashParticipant, GeoEntity {
   private static final EntityDataAccessor<Integer> SYNC_STAGE = SynchedEntityData.defineId(GilgameshEaBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> SYNC_CHARGE = SynchedEntityData.defineId(GilgameshEaBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> SYNC_SCALE = SynchedEntityData.defineId(GilgameshEaBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(GilgameshEaBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(GilgameshEaBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(GilgameshEaBeamEntity.class, EntityDataSerializers.FLOAT);
   public static final int UNLOCK_END = 20, TREE_END = 60, WIND_END = 100, FULL_CHARGE = 200;
   private static final double LENGTH = 150.0, HALF_WIDTH = 12.0, HALF_HEIGHT = 8.0;
   private UUID ownerUuid;
   private Vec3 direction = new Vec3(0, 0, 1);
   private boolean releaseRequested, windOnly, clashing, beamStarted, impactQueued, chargeEffectStarted, autoReleaseFull;
   private int beamTicks;
   private float damageScale = 1.0F;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public GilgameshEaBeamEntity(EntityType<?> type, Level level) { super(type, level); noCulling = true; setNoGravity(true); }
   public GilgameshEaBeamEntity(Level level, LivingEntity owner, Vec3 direction) {
      this(ModEntities.GILGAMESH_EA_BEAM.get(), level); ownerUuid = owner.getUUID();
      autoReleaseFull = !(owner instanceof net.minecraft.server.level.ServerPlayer);
      this.direction = direction.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : direction.normalize();
      syncDirection();
      setPos(owner.position().add(0, owner.getBbHeight() * .65, 0).add(this.direction.scale(1.5)));
   }
   public boolean isOwnedBy(Entity entity) { return entity != null && entity.getUUID().equals(ownerUuid); }
   public void requestRelease(int heldTicks) { releaseRequested = true; if (heldTicks < UNLOCK_END) windOnly = true; }
   public Vec3 getBeamDirection() { return level().isClientSide ? new Vec3(entityData.get(DIR_X),entityData.get(DIR_Y),entityData.get(DIR_Z)) : direction; }
   public int getChargeTicks() { return Math.min(FULL_CHARGE, Math.max(0, tickCount - WIND_END)); }

   @Override public void tick() {
      super.tick();
      if (!(level() instanceof ServerLevel level)) return;
      LivingEntity owner = getOwner(level);
      if (owner == null || !owner.isAlive()) { discard(); return; }
      setPos(owner.position().add(0, owner.getBbHeight() * .65, 0).add(direction.scale(1.5)));
      owner.setDeltaMovement(Vec3.ZERO);
      owner.getPersistentData().putLong("GilgameshEaProtectedUntil", level.getGameTime() + 2);
      entityData.set(SYNC_CHARGE, getChargeTicks()); entityData.set(SYNC_SCALE, damageScale); entityData.set(SYNC_STAGE, stageId());
      if (tickCount == 1) { VFXServerEffects.spawnOriented(level, "ea_unlock", position(), direction, 192); level.playSound(null, owner.blockPosition(), ModSounds.GILGAMESH_VOICE_EA_DRAW.get(), SoundSource.HOSTILE, 2, 1); }
      if (tickCount == UNLOCK_END) VFXServerEffects.spawnOriented(level, "ea_tree", position(), direction, 192);
      if (tickCount == TREE_END) {
         if (!drainMana(owner, 50)) { discard(); return; }
         VFXServerEffects.spawnOriented(level, "ea_wind", position(), direction, 256);
         queueWindTerrain(level);
      }
      if (tickCount >= TREE_END && tickCount < WIND_END) {
         if (tickCount % 4 == 0) applySweptDamage(level, owner, 50, 60, 24);
         if (releaseRequested) windOnly = true;
      }
      if (tickCount == WIND_END && windOnly) { finishAndGiveEa(owner, 600); discard(); return; }
      if (tickCount >= WIND_END && !beamStarted) {
         if (!chargeEffectStarted) { chargeEffectStarted = true; VFXServerEffects.spawnOriented(level, "ea_charge", position(), direction, 256); }
         if (releaseRequested || (autoReleaseFull && tickCount >= FULL_CHARGE)) {
            damageScale = Math.max(.35F, Math.min(1.0F, (tickCount - WIND_END) / 100.0F));
            beamStarted = true; VFXServerEffects.spawnOriented(level, "ea_beam", position(), direction, 256);
            DeferredTerrainDestruction.queueDirectionalCut(level, position(), direction, 150, 24, 16, false);
         } else {
            if (tickCount < FULL_CHARGE && !drainMana(owner, 1.5)) { releaseRequested = true; }
         }
      }
      if (beamStarted) {
         beamTicks++;
         BeamClashManager.tick(level, this);
         if (!clashing && beamTicks % 5 == 0) applySweptDamage(level, owner, 250 * damageScale, HALF_WIDTH, HALF_HEIGHT);
         if (beamTicks >= 100) {
            if (!clashing) finalImpact(level, owner);
            int cooldown = 1200 + Math.round(1200 * Math.max(0.0F, damageScale - 0.35F) / 0.65F);
            finishAndGiveEa(owner, cooldown); discard();
         }
      }
   }

   private void applySweptDamage(ServerLevel level, LivingEntity owner, float damage, double halfWidth, double halfHeight) {
      Vec3 start = position(), end = start.add(direction.scale(LENGTH));
      Vec3 worldUp = Math.abs(direction.y) > .95 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
      Vec3 right = direction.cross(worldUp).normalize(), up = right.cross(direction).normalize();
      AABB search = new AABB(start, end).inflate(halfWidth + 2, halfHeight + 2, halfWidth + 2);
      DamageSource source = owner.damageSources().mobProjectile(this, owner);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search, e -> e.isAlive() && e != owner && !e.isAlliedTo(owner) && !GilgameshDuelState.areDuelPartners(owner, e) && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 rel = target.position().add(0, target.getBbHeight() * .5, 0).subtract(start);
         if (rel.dot(direction) < 0 || rel.dot(direction) > LENGTH || Math.abs(rel.dot(right)) > halfWidth || Math.abs(rel.dot(up)) > halfHeight) continue;
         target.invulnerableTime = 0; target.hurt(source, damage); target.invulnerableTime = 0;
      }
   }

   private void queueWindTerrain(ServerLevel level) { DeferredTerrainDestruction.queueDirectionalCut(level, position(), direction, 150, 120, 48, true); }

   private void finalImpact(ServerLevel level, LivingEntity owner) {
      if (impactQueued) return; impactQueued = true;
      Vec3 center = beamEnd(); double radius = 39 + 21 * damageScale;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius)), e -> e.isAlive() && e != owner && !GilgameshDuelState.areDuelPartners(owner, e) && !EntityUtils.isImmunePlayerTarget(e) && e.distanceToSqr(center) <= radius * radius)) {
         target.invulnerableTime = 0; target.hurt(level.damageSources().explosion(this, owner), 200 * damageScale); target.invulnerableTime = 0;
      }
      DeferredTerrainDestruction.queueSphere(level, center, (int)Math.ceil(radius), 80.0F, 20);
      VFXServerEffects.spawnOriented(level, "ea_impact", center, direction, 256);
   }

   private void finishAndGiveEa(LivingEntity owner, int cooldown) {
      if (owner instanceof net.minecraft.server.level.ServerPlayer player) {
         GilgameshNoblePhantasmItem.giveEaIfMissing(player);
         player.getCooldowns().addCooldown(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.GILGAMESH_EA.get(), cooldown);
         player.getCooldowns().addCooldown(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.GILGAMESH_BAB_ILU.get(), cooldown);
      }
   }
   private boolean drainMana(LivingEntity owner, double amount) {
      if (owner instanceof Player player && player.getAbilities().instabuild) return true;
      if (owner instanceof GilgameshEntity gil) { if (gil.getCurrentMp() < amount) return false; gil.setCurrentMp(gil.getCurrentMp() - amount); return true; }
      if (owner instanceof net.minecraft.server.level.ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.servant_card_transformed) { if (vars.servant_card_mana < amount) return false; vars.servant_card_mana -= amount; vars.syncMana(player); return true; }
         if (vars.player_mana < amount) return false; vars.player_mana -= amount; vars.syncMana(player); return true;
      }
      return true;
   }
   private LivingEntity getOwner(ServerLevel level) { Entity e = ownerUuid == null ? null : level.getEntity(ownerUuid); return e instanceof LivingEntity living ? living : null; }
   private int stageId() { if (beamStarted) return 4; if (tickCount >= WIND_END) return 3; if (tickCount >= TREE_END) return 2; if (tickCount >= UNLOCK_END) return 1; return 0; }
   private void syncDirection() { entityData.set(DIR_X,(float)direction.x); entityData.set(DIR_Y,(float)direction.y); entityData.set(DIR_Z,(float)direction.z); }
   @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { builder.define(SYNC_STAGE,0); builder.define(SYNC_CHARGE,0); builder.define(SYNC_SCALE,1.0F); builder.define(DIR_X,0.0F); builder.define(DIR_Y,0.0F); builder.define(DIR_Z,1.0F); }
   @Override protected void readAdditionalSaveData(CompoundTag tag) { if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner"); direction = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ")); damageScale = tag.getFloat("Scale"); releaseRequested = tag.getBoolean("Release"); windOnly = tag.getBoolean("WindOnly"); beamStarted = tag.getBoolean("Beam"); beamTicks=tag.getInt("BeamTicks"); syncDirection(); }
   @Override protected void addAdditionalSaveData(CompoundTag tag) { if (ownerUuid != null) tag.putUUID("Owner", ownerUuid); tag.putDouble("DirX", direction.x); tag.putDouble("DirY", direction.y); tag.putDouble("DirZ", direction.z); tag.putFloat("Scale", damageScale); tag.putBoolean("Release", releaseRequested); tag.putBoolean("WindOnly", windOnly); tag.putBoolean("Beam", beamStarted); tag.putInt("BeamTicks",beamTicks); }
   @Override public Entity clashEntity() { return this; }
   @Override public BeamType beamType() { return BeamType.EA; }
   @Override public LivingEntity beamOwner(ServerLevel level) { return getOwner(level); }
   @Override public Vec3 beamStart() { return position(); }
   @Override public Vec3 beamEnd() { return position().add(getBeamDirection().scale(LENGTH)); }
   @Override public double beamHalfWidth() { return HALF_WIDTH; }
   @Override public boolean isBeamDamageActive() { return beamStarted && beamTicks < 100; }
   @Override public boolean isClashing() { return clashing; }
   @Override public void setClashing(boolean value) { clashing = value; }
   @Override public void setClashDamageScale(float value) { damageScale = Math.max(0, value); }
   @Override public void cancelClashBeam() { discard(); }
   @Override public void continueAfterClash() { clashing = false; }
   @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

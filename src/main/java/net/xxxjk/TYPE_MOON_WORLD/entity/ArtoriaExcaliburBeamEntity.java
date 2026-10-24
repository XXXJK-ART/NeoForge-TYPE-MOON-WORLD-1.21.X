package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaStateService;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.BeamSweepGeometry;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmDamageClassifier;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.BeamClashManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.BeamClashParticipant;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.BeamType;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public class ArtoriaExcaliburBeamEntity extends Entity implements BeamClashParticipant {
   private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DAMAGE_START_TICK = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> POWER_SCALE = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> GOLDEN_VARIANT = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> SHORT_VARIANT = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.BOOLEAN);
   private static final double LENGTH = 150.0;
   private static final double HALF_WIDTH = 12.0;
   private static final double HALF_HEIGHT = 7.0;
   private static final int BLOCK_DESTROY_PHASE_TICKS = 5;
   private static final float DAMAGE_TOTAL = 7500.0F;
   private static final float CRATER_DAMAGE = 180.0F;
   private static final double GOLDEN_LENGTH = 45.0;
   private static final double GOLDEN_HALF_WIDTH = 4.0;
   private static final double GOLDEN_HALF_HEIGHT = 2.5;
   private static final float GOLDEN_DAMAGE_TOTAL = 300.0F;
   private static final float GOLDEN_DAMAGE_PER_PULSE = GOLDEN_DAMAGE_TOTAL / (30.0F / 5.0F);
   private UUID ownerUuid;
   private boolean craterQueued;
   private int shortBlocksDestroyed;
   private boolean restored;
   private int activeTicks, emittedPulses;
   private String ownerForm = "";
   private Vec3 lastDirection;
   private List<Vec3> terrainDirections = List.of();
   private final List<Vec3> pulseDirections = new ArrayList<>();
   private boolean clashing;
   private float clashDamageScale = 1.0F;

   public ArtoriaExcaliburBeamEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noCulling = true;
      this.setNoGravity(true);
   }

   public ArtoriaExcaliburBeamEntity(Level level, LivingEntity owner, Vec3 start, int duration) {
      this(level, owner, start, duration, 0);
   }

   public ArtoriaExcaliburBeamEntity(Level level, LivingEntity owner, Vec3 start, int duration, int damageStartTick) {
      this(level, owner, start, duration, damageStartTick, 1.0F);
   }

   public ArtoriaExcaliburBeamEntity(Level level, LivingEntity owner, Vec3 start, int duration, int damageStartTick, float powerScale) {
      this(level, owner, start, duration, damageStartTick, powerScale, false);
   }

   public ArtoriaExcaliburBeamEntity(Level level, LivingEntity owner, Vec3 start, int duration, int damageStartTick, float powerScale, boolean goldenVariant) {
      this(level, owner, start, duration, damageStartTick, powerScale, goldenVariant, false);
   }

   public ArtoriaExcaliburBeamEntity(Level level, LivingEntity owner, Vec3 start, int duration, int damageStartTick,
                                     float powerScale, boolean goldenVariant, boolean shortVariant) {
      this(ModEntities.ARTORIA_EXCALIBUR_BEAM.get(), level);
      this.ownerUuid = owner.getUUID();
      if (owner instanceof ServerPlayer player) {
         var card = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
         if (card.transformed()) this.ownerForm = card.id();
      }
      this.setPos(start);
      this.entityData.set(DURATION, duration);
      this.entityData.set(DAMAGE_START_TICK, Math.max(0, Math.min(duration, damageStartTick)));
      this.entityData.set(POWER_SCALE, Math.max(0.2F, Math.min(1.0F, powerScale)));
      this.entityData.set(GOLDEN_VARIANT, goldenVariant);
      this.entityData.set(SHORT_VARIANT, shortVariant);
      this.updateEndFromOwner(owner);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(END_X, 0.0F);
      builder.define(END_Y, 0.0F);
      builder.define(END_Z, 0.0F);
      builder.define(DURATION, 150);
      builder.define(DAMAGE_START_TICK, 0);
      builder.define(POWER_SCALE, 1.0F);
      builder.define(GOLDEN_VARIANT, false);
      builder.define(SHORT_VARIANT, false);
   }

   public Vec3 getEndPos() {
      return new Vec3(this.entityData.get(END_X), this.entityData.get(END_Y), this.entityData.get(END_Z));
   }

   public int getDuration() {
      return this.entityData.get(DURATION);
   }

   public int getDamageStartTick() {
      return this.entityData.get(DAMAGE_START_TICK);
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) return;
      LivingEntity owner = this.getOwner(level);
      if (this.restored || !validOwner(owner)) { this.discard(); return; }
      this.setPos(owner.position().add(0, owner.getBbHeight() * .66, 0).add(ArtoriaPendragonCombatHelper.excaliburLook(owner).scale(1.2)));
      this.updateEndFromOwner(owner);
      Vec3 direction = this.getEndPos().subtract(this.position()).normalize();
      List<Vec3> sweep = BeamSweepGeometry.directions(lastDirection, direction);
      lastDirection = direction;
      terrainDirections = sweep;
      BeamClashManager.tick(level, this);
      if (this.clashing) {
         this.entityData.set(DURATION, getDuration() + 1);
         pulseDirections.clear();
         return;
      }
      activeTicks++;
      if (isBeamActive()) {
         pulseDirections.addAll(sweep);
         int interval = isShortVariant() ? ArtoriaRules.EXCALIBUR_SHORT_PULSE_INTERVAL
            : isGoldenVariant() ? 5 : ArtoriaRules.PULSE_INTERVAL;
         int maxPulses = isShortVariant() ? ArtoriaRules.EXCALIBUR_SHORT_PULSES : ArtoriaRules.PULSES;
         if ((activeTicks - getDamageStartTick()) % interval == 0
            && (isGoldenVariant() || emittedPulses < maxPulses)) {
            this.applyBeamDamage(level, owner);
            emittedPulses++;
            pulseDirections.clear();
         }
         this.destroyBeamBlocks(level, owner);
      }
      if (this.tickCount >= getDuration()) {
         if (!isShortVariant()) {
            this.queueCrater(level, owner, getEndPos());
            if (ArtoriaStateService.isArtoria(owner)) ArtoriaAbilityService.reveal(owner, ArtoriaRules.REVEAL_AFTER_NP);
         }
         this.discard();
      }
   }

   private boolean validOwner(LivingEntity owner) {
      if (owner == null || !owner.isAlive() || owner.isRemoved()) return false;
      if (owner instanceof ServerPlayer player) {
         if (player.hasDisconnected()) return false;
         if (!ownerForm.isEmpty()) {
            var card = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
            if (!card.transformed() || !ownerForm.equals(card.id())) return false;
         }
      }
      return true;
   }

   @Override public void remove(RemovalReason reason) {
      if (level() instanceof ServerLevel level) {
         LivingEntity owner = getOwner(level);
         if (owner != null && owner.getPersistentData().hasUUID(ArtoriaStateService.NP_BEAM)
            && getUUID().equals(owner.getPersistentData().getUUID(ArtoriaStateService.NP_BEAM))) {
            owner.getPersistentData().remove(ArtoriaStateService.NP_BEAM);
            owner.getPersistentData().remove(ArtoriaPendragonCombatHelper.TAG_EXCALIBUR_RELEASE_UNTIL);
         }
      }
      super.remove(reason);
   }

   public int emittedPulses() { return emittedPulses; }
   public float pulseDamage() {
      return (isShortVariant() ? ArtoriaRules.EXCALIBUR_SHORT_PULSE_DAMAGE
         : isGoldenVariant() ? GOLDEN_DAMAGE_PER_PULSE * powerScale() : ArtoriaRules.PULSE_DAMAGE) * clashDamageScale;
   }
   public float impactPower() { return powerScale() * clashDamageScale; }
   public boolean hasTerminalImpact() { return craterQueued; }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.restored = true; // A saved beam cannot resume a cast after server reload.
      this.entityData.set(END_X, tag.getFloat("EndX"));
      this.entityData.set(END_Y, tag.getFloat("EndY"));
      this.entityData.set(END_Z, tag.getFloat("EndZ"));
      this.entityData.set(DURATION, tag.getInt("Duration"));
      this.entityData.set(DAMAGE_START_TICK, tag.getInt("DamageStartTick"));
      this.entityData.set(POWER_SCALE, Math.max(0.2F, Math.min(1.0F, tag.contains("PowerScale") ? tag.getFloat("PowerScale") : 1.0F)));
      this.entityData.set(GOLDEN_VARIANT, tag.getBoolean("GoldenVariant"));
      this.entityData.set(SHORT_VARIANT, tag.getBoolean("ShortVariant"));
      this.craterQueued = tag.getBoolean("CraterQueued");
      this.clashDamageScale = Math.max(0.0F, Math.min(1.0F,
         tag.contains("ClashDamageScale") ? tag.getFloat("ClashDamageScale") : 1.0F));
      if (tag.hasUUID("Owner")) {
         this.ownerUuid = tag.getUUID("Owner");
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("EndX", this.entityData.get(END_X));
      tag.putFloat("EndY", this.entityData.get(END_Y));
      tag.putFloat("EndZ", this.entityData.get(END_Z));
      tag.putInt("Duration", this.entityData.get(DURATION));
      tag.putInt("DamageStartTick", this.entityData.get(DAMAGE_START_TICK));
      tag.putFloat("PowerScale", this.entityData.get(POWER_SCALE));
      tag.putBoolean("GoldenVariant", this.entityData.get(GOLDEN_VARIANT));
      tag.putBoolean("ShortVariant", this.entityData.get(SHORT_VARIANT));
      tag.putBoolean("CraterQueued", this.craterQueued);
      tag.putFloat("ClashDamageScale", this.clashDamageScale);
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 262144.0;
   }

   @Override
   public AABB getBoundingBoxForCulling() {
      Vec3 start = this.position();
      Vec3 end = this.getEndPos();
      AABB box = new AABB(start, start).minmax(new AABB(end, end));
      if (this.getDamageStartTick() > 0 && this.tickCount < this.getDamageStartTick()) {
         box = box.minmax(new AABB(start.add(0.0, this.beamLength(), 0.0), start.add(0.0, this.beamLength(), 0.0)));
      }
      return box.inflate(16.0);
   }

   private LivingEntity getOwner(ServerLevel level) {
      if (this.ownerUuid == null) {
         return null;
      }
      Entity entity = level.getEntity(this.ownerUuid);
      return entity instanceof LivingEntity living ? living : null;
   }

   private void updateEndFromOwner(LivingEntity owner) {
      Vec3 end = this.position().add(ArtoriaPendragonCombatHelper.excaliburLook(owner).scale(this.beamLength()));
      this.entityData.set(END_X, (float)end.x);
      this.entityData.set(END_Y, (float)end.y);
      this.entityData.set(END_Z, (float)end.z);
   }

   private float powerScale() {
      return Math.max(0.2F, Math.min(1.0F, this.entityData.get(POWER_SCALE)));
   }

   private double beamLength() {
      if (this.entityData.get(SHORT_VARIANT)) return ArtoriaRules.EXCALIBUR_SHORT_RANGE * powerScale();
      double base = this.entityData.get(GOLDEN_VARIANT) ? GOLDEN_LENGTH : LENGTH;
      return base * (0.35 + this.powerScale() * 0.65);
   }

   public double beamHalfWidth() {
      double base = this.entityData.get(GOLDEN_VARIANT) ? GOLDEN_HALF_WIDTH : HALF_WIDTH;
      return base * (isShortVariant() ? 0.25 : (0.28 + this.powerScale() * 0.72));
   }

   public boolean isGoldenVariant() {
      return this.entityData.get(GOLDEN_VARIANT);
   }

   public boolean isShortVariant() {
      return this.entityData.get(SHORT_VARIANT);
   }

   @Override
   public float clashPower() { return this.powerScale(); }

   @Override
   public float clashBaseDamage() {
      if (this.entityData.get(SHORT_VARIANT))
         return ArtoriaRules.EXCALIBUR_SHORT_PULSES * ArtoriaRules.EXCALIBUR_SHORT_PULSE_DAMAGE;
      return (this.entityData.get(GOLDEN_VARIANT) ? GOLDEN_DAMAGE_TOTAL : DAMAGE_TOTAL)
         * this.powerScale();
   }

   public double beamHalfHeight() {
      double base = this.entityData.get(GOLDEN_VARIANT) ? GOLDEN_HALF_HEIGHT : HALF_HEIGHT;
      return base * (isShortVariant() ? 0.25 : (0.35 + this.powerScale() * 0.65));
   }

   private boolean isBeamActive() {
      return this.activeTicks >= this.entityData.get(DAMAGE_START_TICK);
   }

   private void applyBeamDamage(ServerLevel level, LivingEntity owner) {
      Vec3 start = position();
      List<Vec3> directions = pulseDirections.isEmpty() ? List.of(lastDirection) : pulseDirections;
      AABB search = new AABB(start, start);
      for (Vec3 direction : directions) {
         Vec3 end = start.add(direction.scale(beamLength()));
         search = search.minmax(new AABB(end, end));
      }
      search = search.inflate(beamHalfWidth() + 2, beamHalfHeight() + 2, beamHalfWidth() + 2);
      DamageSource source = NoblePhantasmDamageClassifier.withNoblePhantasm(owner.damageSources().mobProjectile(this, owner), "excalibur_beam");
       float amount = pulseDamage();
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search, e -> ArtoriaAbilityService.enemy(owner, e))) {
         for (Vec3 direction : directions) {
            if (!BeamSweepGeometry.intersects(target.getBoundingBox(), start, direction, beamLength(), beamHalfWidth(), beamHalfHeight())) continue;
            hurtWithoutIFrames(target, source, amount);
            target.push(direction.x * .15, direction.y * .08, direction.z * .15);
            target.hurtMarked = true;
            break; // A target can receive this pulse only once across every sweep sample.
         }
      }
   }

   private void destroyBeamBlocks(ServerLevel level, LivingEntity owner) {
      Vec3 start = position();
      int broken = 0, examined = 0;
      // A finite per-tick budget includes empty samples, preventing rapid turns from stalling the server.
      for (Vec3 forward : terrainDirections) {
         Vec3 worldUp = Math.abs(forward.y) > .95 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
         Vec3 right = forward.cross(worldUp).normalize(), up = right.cross(forward).normalize();
         int phase = activeTicks % 5;
         for (double along = phase + .75; along <= beamLength(); along += 5) {
            double width = beamHalfWidth() * Math.pow(Math.max(.25, Math.sin(Math.PI * along / beamLength())), .35);
            for (double side = -width; side <= width; side += 1.7)
               for (double y = -beamHalfHeight(); y <= beamHalfHeight(); y += 1.7) {
                   if (++examined > 8192 || broken >= (isShortVariant() ? 6 : 260)) return;
                  Vec3 sample = start.add(forward.scale(along)).add(right.scale(side)).add(up.scale(y));
                  if (owner.position().distanceToSqr(sample) <= 2.25) continue;
                  if (destroyBlock(level, owner, BlockPos.containing(sample))) broken++;
               }
         }
      }
   }



   private boolean destroyBlock(ServerLevel level, LivingEntity owner, BlockPos pos) {
      if (!Config.terrainDestructionEnabled || !level.hasChunkAt(pos) || owner == null) return false;
      if (owner instanceof ServerPlayer player) {
         if (!Config.playerTerrainDestructionEnabled || !level.mayInteract(player, pos)) return false;
      } else if (!Config.npcTerrainDestructionEnabled || !net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, owner)) return false;
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
       float max = isShortVariant() ? 0.6F : (isGoldenVariant() ? 25 : 100);
      if (state.isAir() || state.is(Blocks.BEDROCK) || hardness < 0 || hardness > max || level.getBlockEntity(pos) != null
         || state.getExplosionResistance(level, pos, null) >= 1200 || !state.canEntityDestroy(level, pos, owner)
         || !net.neoforged.neoforge.event.EventHooks.onEntityDestroyBlock(owner, pos, state)) return false;
      return level.removeBlock(pos, false);
   }

   private void queueCrater(ServerLevel level, LivingEntity owner, Vec3 center) {
       if (craterQueued || isGoldenVariant() || isShortVariant() || !validOwner(owner) || center == null) return;
      craterQueued = true;
      level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 5, .45F);
      VFXServerEffects.spawn(level, "artoria_excalibur_impact", center, 192);
      float power = powerScale() * clashDamageScale;
      int radius = Math.max(6, (int)Math.floor(28 * (.25 + power * .75)));
      int height = Math.max(18, (int)Math.floor(112 * (.22 + power * .78)));
      AABB area = new AABB(center, center).inflate(radius, height, radius);
      DamageSource source = NoblePhantasmDamageClassifier.withNoblePhantasm(level.damageSources().explosion(this, owner), "excalibur_beam");
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> ArtoriaAbilityService.enemy(owner, e))) {
         Vec3 rel = target.getBoundingBox().getCenter().subtract(center);
         double normalized = (rel.x * rel.x + rel.z * rel.z) / (radius * radius) + rel.y * rel.y / (height * height);
         if (normalized > 1.18) continue;
         hurtWithoutIFrames(target, source, (float)(CRATER_DAMAGE * power * Math.max(.35, 1.15 - normalized)));
      }
      // Finish the endpoint in this cast transaction; no post-discard damage or terrain jobs.
      int removed = 0;
      for (int y = -height; y <= height && removed < 512; y += 3)
         for (int x = -radius; x <= radius && removed < 512; x += 2)
            for (int z = -radius; z <= radius && removed < 512; z += 2) {
               if ((x * x + z * z) / (double)(radius * radius) + y * y / (double)(height * height) > 1) continue;
               if (destroyBlock(level, owner, BlockPos.containing(center.add(x, y, z)))) removed++;
            }
   }

   private static Vec3 horizontalLook(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      return look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
   }

   private static void hurtWithoutIFrames(LivingEntity target, DamageSource source, float damage) {
      target.invulnerableTime = 0;
      target.hurtTime = 0;
      target.hurtDuration = 0;
      target.hurt(source, damage);
      target.invulnerableTime = 0;
      target.hurtTime = 0;
      target.hurtDuration = 0;
   }

   @Override
   public Entity clashEntity() { return this; }

   @Override
   public BeamType beamType() { return BeamType.EXCALIBUR; }

   @Override
   public LivingEntity beamOwner(ServerLevel level) { return this.getOwner(level); }

   @Override
   public Vec3 beamStart() { return this.position(); }

   @Override
   public Vec3 beamEnd() { return this.getEndPos(); }

   @Override
   public boolean isBeamDamageActive() { return this.isBeamActive(); }

   @Override
   public boolean isClashing() { return this.clashing; }

   @Override
   public void setClashing(boolean value) { this.clashing = value; }

   @Override
   public void setClashDamageScale(float value) { this.clashDamageScale = Math.max(0.0F, Math.min(1.0F, value)); }

   @Override
   public void cancelClashBeam() { this.discard(); }

   @Override
   public void continueAfterClash() { this.clashing = false; }

}

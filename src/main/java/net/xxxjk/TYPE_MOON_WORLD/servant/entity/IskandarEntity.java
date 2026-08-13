package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.BucephalusEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GordiusWheelEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.IskandarMountEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MacedonianSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.iskandar.IonioiHetairoiRankPool;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.Nullable;

public final class IskandarEntity extends ServantEntity {
   public static final String SERVANT_KEY = "iskandar";
   public static final String TAG_LEADERSHIP_UNTIL = "IskandarLeadershipUntil";
   public static final String TAG_MILITARY_TACTICS_UNTIL = "IskandarMilitaryTacticsUntil";
   public static final String TAG_IONIOI_ACTIVE = "IskandarIonioiActive";
   public static final String TAG_IONIOI_DEATHS = "IskandarIonioiDeaths";
   public static final String TAG_IONIOI_NEXT_INDEX = "IskandarIonioiNextIndex";
   public static final String TAG_IONIOI_SEED = "IskandarIonioiSeed";
   public static final String TAG_IONIOI_START_TICK = "IskandarIonioiStartTick";
   public static final String TAG_IONIOI_OFFSCREEN_DUEL = "IskandarIonioiOffscreenDuel";
   public static final String TAG_IONIOI_OFFSCREEN_PREVIOUS_INVISIBLE = "IskandarIonioiOffscreenPrevInvisible";
   public static final String TAG_IONIOI_OFFSCREEN_PREVIOUS_INVULNERABLE = "IskandarIonioiOffscreenPrevInvulnerable";
   public static final String TAG_IONIOI_OFFSCREEN_PREVIOUS_NO_AI = "IskandarIonioiOffscreenPrevNoAi";
   static final String TAG_BUCEPHALUS_SUMMON_COOLDOWN = "IskandarBucephalusSummonCooldown";
   static final String TAG_GORDIUS_WHEEL_SUMMONED_ONCE = "IskandarGordiusWheelSummonedOnce";
   private static final String TAG_IONIOI_RETURN_DIM = "IskandarIonioiReturnDim";
   private static final String TAG_IONIOI_RETURN_X = "IskandarIonioiReturnX";
   private static final String TAG_IONIOI_RETURN_Y = "IskandarIonioiReturnY";
   private static final String TAG_IONIOI_RETURN_Z = "IskandarIonioiReturnZ";
   private static final String TAG_IONIOI_TARGET_OWNER = "IonioiHetairoiTargetOwner";
   private static final String TAG_IONIOI_TARGET_RETURN_DIM = "IonioiHetairoiReturnDim";
   private static final String TAG_IONIOI_TARGET_RETURN_X = "IonioiHetairoiReturnX";
   private static final String TAG_IONIOI_TARGET_RETURN_Y = "IonioiHetairoiReturnY";
   private static final String TAG_IONIOI_TARGET_RETURN_Z = "IonioiHetairoiReturnZ";
   private static final String TAG_IONIOI_TARGET_PRIMARY = "IonioiHetairoiPrimaryTarget";
   private static final ResourceLocation LEADERSHIP_ATTACK_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "iskandar_leadership_attack");
   private static final ResourceLocation MOUNT_ARMOR_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "iskandar_mount_armor");
   private static final int LEADERSHIP_COOLDOWN = 25 * 20;
   private static final int LEADERSHIP_DURATION = 20 * 20;
   private static final int MILITARY_TACTICS_COOLDOWN = 20 * 20;
   private static final int MILITARY_TACTICS_DURATION = 15 * 20;
   static final int MOUNT_SUMMON_COOLDOWN = 30 * 20;
   static final int WHEEL_CHARGE_COOLDOWN = 15 * 20;
   private static final int IONIOI_COOLDOWN = 90 * 20;
   public static final int IONIOI_FORMATION_DELAY_TICKS = 2 * 20;
   private static final int IONIOI_FREE_UPKEEP = 30 * 20;
   private static final int IONIOI_ACTIVE_CAP = 200;
   static final double IONIOI_MP_COST = 120.0;
   private static final double IONIOI_UPKEEP_MP_PER_SECOND = 5.0;
   @Nullable private UUID bucephalusUuid;
   @Nullable private UUID gordiusWheelUuid;
   private final List<UUID> ionioiSoldiers = new ArrayList<>();
   @Nullable private IonioiHetairoiRankPool ionioiPool;

   public IskandarEntity(EntityType<? extends IskandarEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      tickMountedPose();
      tickKnownMounts(level);
      updateMountArmor();
      tickLeadership(level);
      tickMilitaryTactics(level);
      IskandarCombatHelper.tick(this, level);
      tickIonioiHetairoi(level);
   }

   @Override
   protected void customServerAiStep() {
      if (this.isPassenger() && this.getVehicle() instanceof IskandarMountEntity) {
         this.getNavigation().stop();
         this.setDeltaMovement(Vec3.ZERO);
         return;
      }
      super.customServerAiStep();
   }

   private void tickMountedPose() {
      if (!(this.getVehicle() instanceof IskandarMountEntity mount)) {
         return;
      }
      float yaw = mount.getYRot();
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
      this.setXRot(0.0F);
   }

   void summonBucephalusAndRide(ServerLevel level) {
      if (!canSummonBucephalus(level) || this.getVehicle() instanceof BucephalusEntity || getGordiusWheel(level) != null) {
         return;
      }
      BucephalusEntity horse = ModEntities.BUCEPHALUS.get().create(level);
      if (horse == null) {
         return;
      }
      Vec3 forward = this.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      Vec3 pos = this.position().add(forward.normalize().scale(1.4));
      horse.moveTo(pos.x, this.getY(), pos.z, this.getYRot(), 0.0F);
      horse.bindIskandar(this, this.getEntityMaster());
      level.addFreshEntity(horse);
      this.bucephalusUuid = horse.getUUID();
      this.startRiding(horse, true);
   }

   void summonGordiusWheelAndRide(ServerLevel level) {
      if (!canSummonGordiusWheel(level) || this.getVehicle() instanceof GordiusWheelEntity) {
         return;
      }
      discardBucephalus(level);
      GordiusWheelEntity wheel = ModEntities.GORDIUS_WHEEL.get().create(level);
      if (wheel == null) {
         return;
      }
      Vec3 forward = this.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      Vec3 pos = this.position().add(forward.normalize().scale(2.2)).add(0.0, 1.2, 0.0);
      wheel.moveTo(pos.x, pos.y, pos.z, this.getYRot(), 0.0F);
      wheel.bindIskandar(this, this.getEntityMaster());
      wheel.snapRearBodyToCurrentPosition();
      level.addFreshEntity(wheel);
      this.gordiusWheelUuid = wheel.getUUID();
      this.getPersistentData().putBoolean(TAG_GORDIUS_WHEEL_SUMMONED_ONCE, true);
      this.startRiding(wheel, true);
   }

   private boolean canSummonBucephalus(ServerLevel level) {
      return this.isAlive()
         && (!this.isIonioiHetairoiActive() || ModDimensions.isIonioiHetairoiDimension(level.dimension().location()))
         && level.getGameTime() >= this.getPersistentData().getLong(TAG_BUCEPHALUS_SUMMON_COOLDOWN);
   }

   boolean canSummonGordiusWheel(ServerLevel level) {
      return this.isAlive()
         && !this.getPersistentData().getBoolean(TAG_GORDIUS_WHEEL_SUMMONED_ONCE)
         && (!this.isIonioiHetairoiActive() || ModDimensions.isIonioiHetairoiDimension(level.dimension().location()));
   }

   boolean hasSummonedGordiusWheelOnce() {
      return this.getPersistentData().getBoolean(TAG_GORDIUS_WHEEL_SUMMONED_ONCE);
   }

   private void tickKnownMounts(ServerLevel level) {
      if (this.bucephalusUuid != null) {
         getBucephalus(level);
      }
      if (this.gordiusWheelUuid != null) {
         getGordiusWheel(level);
      }
   }

   private void tickLeadership(ServerLevel level) {
      long now = level.getGameTime();
      if (this.getTarget() == null || now < this.getPersistentData().getLong("IskandarLeadershipCooldown") || this.getCurrentMp() < 20.0) {
         cleanupLeadership(level, now);
         return;
      }
      int allies = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(30.0),
         entity -> entity != this && entity.isAlive() && this.isAlliedTo(entity)).size();
      if (allies < 3 && this.getHealth() > this.getMaxHealth() * 0.5F) {
         cleanupLeadership(level, now);
         return;
      }
      this.setCurrentMp(this.getCurrentMp() - 20.0);
      this.getPersistentData().putLong("IskandarLeadershipCooldown", now + LEADERSHIP_COOLDOWN);
      this.getPersistentData().putLong(TAG_LEADERSHIP_UNTIL, now + LEADERSHIP_DURATION);
      this.triggerNamedActionAnimation("leadership");
      level.playSound(null, this.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0F, 0.85F);
      cleanupLeadership(level, now);
   }

   private void cleanupLeadership(ServerLevel level, long now) {
      boolean active = now < this.getPersistentData().getLong(TAG_LEADERSHIP_UNTIL);
      for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(32.0),
         entity -> entity.isAlive() && (entity == this || this.isAlliedTo(entity)))) {
         AttributeInstance attack = ally.getAttribute(Attributes.ATTACK_DAMAGE);
         if (attack == null) {
            continue;
         }
         AttributeModifier current = attack.getModifier(LEADERSHIP_ATTACK_ID);
         if (!active) {
            if (current != null) {
               attack.removeModifier(LEADERSHIP_ATTACK_ID);
            }
         } else if (current == null) {
            attack.addTransientModifier(new AttributeModifier(LEADERSHIP_ATTACK_ID, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
         }
      }
   }

   private void tickMilitaryTactics(ServerLevel level) {
      long now = level.getGameTime();
      if (this.getTarget() != null && now >= this.getPersistentData().getLong("IskandarMilitaryTacticsCooldown") && this.getCurrentMp() >= 15.0) {
         this.setCurrentMp(this.getCurrentMp() - 15.0);
         this.getPersistentData().putLong("IskandarMilitaryTacticsCooldown", now + MILITARY_TACTICS_COOLDOWN);
         this.getPersistentData().putLong(TAG_MILITARY_TACTICS_UNTIL, now + MILITARY_TACTICS_DURATION);
         this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MILITARY_TACTICS_DURATION, 0, true, false));
      }
   }

   void tryGordiusWheelCharge(ServerLevel level) {
      LivingEntity target = this.getTarget();
      GordiusWheelEntity wheel = getGordiusWheel(level);
      if (target == null || !target.isAlive() || wheel == null || this.getVehicle() != wheel) {
         return;
      }
      long now = level.getGameTime();
      if (now >= this.getPersistentData().getLong("IskandarWheelChargeCooldown") && this.distanceToSqr(target) <= 32.0 * 32.0) {
         this.getPersistentData().putLong("IskandarWheelChargeCooldown", now + WHEEL_CHARGE_COOLDOWN);
         wheel.performCharge(level, this, 36.0F, 2.6);
         this.triggerNamedActionAnimation("charge");
      }
   }

   private void tickIonioiHetairoi(ServerLevel level) {
      CompoundTag data = this.getPersistentData();
      long now = level.getGameTime();
      boolean wantsNp = this.shouldUseIonioi(level);
      if (!this.isIonioiHetairoiActive() && wantsNp && now >= data.getLong("IskandarIonioiCooldown") && this.getCurrentMp() >= IONIOI_MP_COST) {
         IskandarEntity moved = startIonioiHetairoi(level);
         if (moved != this) {
            return;
         }
      }
      if (!this.isIonioiHetairoiActive()) {
         return;
      }

      long start = data.getLong(TAG_IONIOI_START_TICK);
      if (now > start + IONIOI_FREE_UPKEEP && now % 20L == 0L) {
         if (this.getCurrentMp() < IONIOI_UPKEEP_MP_PER_SECOND) {
            endIonioiHetairoi(level);
            return;
         }
         this.setCurrentMp(this.getCurrentMp() - IONIOI_UPKEEP_MP_PER_SECOND);
      }
      if (data.getInt(TAG_IONIOI_DEATHS) >= IonioiHetairoiRankPool.TOTAL_SIZE || !this.isAlive()) {
         endIonioiHetairoi(level);
         return;
      }
      pruneDeadSoldiers(level);
      int deaths = data.getInt(TAG_IONIOI_DEATHS);
      if (deaths > 8000 && now % 10L == 0L) {
         level.sendParticles(ParticleTypes.ASH, this.getX(), this.getY() + 1.0, this.getZ(), 24, 8.0, 1.5, 8.0, 0.08);
      }
      if (now >= start + IONIOI_FORMATION_DELAY_TICKS) {
         replenishSoldiers(level);
      }
      if (now % 5L == 0L) {
         level.sendParticles(ParticleTypes.DUST_PLUME, this.getX(), this.getY() + 0.1, this.getZ(), 12, 8.0, 0.1, 8.0, 0.05);
      }
   }

   private boolean shouldUseIonioi(ServerLevel level) {
      return IskandarCombatHelper.shouldUseIonioi(this, level);
   }

   private IskandarEntity startIonioiHetairoi(ServerLevel level) {
      ServerLevel ionioiLevel = level.getServer().getLevel(ModDimensions.IONIOI_HETAIROI_KEY);
      if (ionioiLevel == null) {
         return this;
      }
      LivingEntity primary = this.getTarget();
      if (primary == null || !primary.isAlive()) {
         return this;
      }

      List<LivingEntity> pulled = collectIonioiTargets(level, primary);
      LivingEntity master = this.getEntityMaster();
      if (master != null && master.isAlive() && master.level() == level && this.distanceToSqr(master) <= 30.0 * 30.0 && !pulled.contains(master)) {
         pulled.add(master);
      }
      if (!pulled.isEmpty() && pulled.stream().noneMatch(ServerPlayer.class::isInstance)) {
         startOffscreenIonioiDuel(level, pulled.get(0), level.getGameTime());
         return this;
      }

      CompoundTag data = this.getPersistentData();
      data.putString(TAG_IONIOI_RETURN_DIM, level.dimension().location().toString());
      data.putDouble(TAG_IONIOI_RETURN_X, this.getX());
      data.putDouble(TAG_IONIOI_RETURN_Y, this.getY());
      data.putDouble(TAG_IONIOI_RETURN_Z, this.getZ());

      long seed = this.getRandom().nextLong();
      this.ionioiPool = IonioiHetairoiRankPool.create(seed);
      this.ionioiSoldiers.clear();
      this.setCurrentMp(this.getCurrentMp() - IONIOI_MP_COST);
      data.putBoolean(TAG_IONIOI_ACTIVE, true);
      data.putLong(TAG_IONIOI_SEED, seed);
      data.putLong(TAG_IONIOI_START_TICK, level.getGameTime());
      data.putLong("IskandarIonioiCooldown", level.getGameTime() + IONIOI_COOLDOWN);
      data.putInt(TAG_IONIOI_DEATHS, 0);
      data.putInt(TAG_IONIOI_NEXT_INDEX, 0);
      this.triggerNamedActionAnimation("ionioi_hetairoi");
      ServantVoiceHelper.tryPlayIskandarIonioi(this);
      level.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.2, this.getZ(), 4, 0.0, 0.0, 0.0, 0.0);
      level.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.4F, 0.75F);
      discardLocalMounts(level);

      Vec3 entry = randomIonioiEntry();
      entry = new Vec3(entry.x, findSafeSpawnY(ionioiLevel, Mth.floor(entry.x), Mth.floor(entry.z)), entry.z);
      LivingEntity movedPrimary = moveIonioiTargets(level, ionioiLevel, pulled, primary, entry);
      Entity moved = this.changeDimension(new DimensionTransition(ionioiLevel, entry, Vec3.ZERO, this.getYRot(), this.getXRot(), DimensionTransition.DO_NOTHING));
      if (moved instanceof IskandarEntity iskandar) {
         CompoundTag movedData = iskandar.getPersistentData();
         movedData.putBoolean(TAG_IONIOI_ACTIVE, true);
         movedData.putLong(TAG_IONIOI_SEED, seed);
         movedData.putLong(TAG_IONIOI_START_TICK, ionioiLevel.getGameTime());
         movedData.putLong("IskandarIonioiCooldown", ionioiLevel.getGameTime() + IONIOI_COOLDOWN);
         movedData.putInt(TAG_IONIOI_DEATHS, 0);
         movedData.putInt(TAG_IONIOI_NEXT_INDEX, 0);
         movedData.putString(TAG_IONIOI_RETURN_DIM, data.getString(TAG_IONIOI_RETURN_DIM));
         movedData.putDouble(TAG_IONIOI_RETURN_X, data.getDouble(TAG_IONIOI_RETURN_X));
         movedData.putDouble(TAG_IONIOI_RETURN_Y, data.getDouble(TAG_IONIOI_RETURN_Y));
         movedData.putDouble(TAG_IONIOI_RETURN_Z, data.getDouble(TAG_IONIOI_RETURN_Z));
         iskandar.ionioiPool = IonioiHetairoiRankPool.create(seed);
         iskandar.ionioiSoldiers.clear();
         if (movedPrimary != null && movedPrimary.isAlive() && !movedPrimary.isAlliedTo(iskandar)) {
            iskandar.setTarget(movedPrimary);
         }
         iskandar.bucephalusUuid = null;
         iskandar.gordiusWheelUuid = null;
         iskandar.spawnInitialFormation(ionioiLevel);
         ionioiLevel.sendParticles(ParticleTypes.FLASH, entry.x, entry.y + 1.2, entry.z, 6, 0.0, 0.0, 0.0, 0.0);
         ionioiLevel.playSound(null, BlockPos.containing(entry), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.2F, 0.85F);
         return iskandar;
      }
      endIonioiHetairoi(ionioiLevel);
      return this;
   }

   private void startOffscreenIonioiDuel(ServerLevel level, LivingEntity target, long now) {
      if (target == null || !target.isAlive() || target instanceof ServerPlayer) {
         return;
      }
      this.setCurrentMp(this.getCurrentMp() - IONIOI_MP_COST);
      this.getPersistentData().putLong("IskandarIonioiCooldown", now + IONIOI_COOLDOWN);
      this.triggerNamedActionAnimation("ionioi_hetairoi");
      ServantVoiceHelper.tryPlayIskandarIonioi(this);

      int duration = 120 + this.getRandom().nextInt(81);
      Vec3 center = this.position().add(target.position()).scale(0.5);
      level.playSound(null, BlockPos.containing(center), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.2F, 0.7F);
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.4, center.z, 4, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.DUST_PLUME, center.x, center.y + 0.1, center.z, 64, 2.8, 0.2, 2.8, 0.08);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y + 0.25, center.z, 32, 1.6, 0.18, 1.6, 0.025);

      hideForIonioiOffscreenDuel(this);
      hideForIonioiOffscreenDuel(target);
      this.setTarget(null);
      if (target instanceof Mob mobTarget) {
         mobTarget.setTarget(null);
      }

      double iskandarScore = duelScore(this) + 38.0 + this.getRandom().nextDouble() * 65.0;
      double targetScore = duelScore(target) + target.getRandom().nextDouble() * 55.0;
      int outcome = iskandarScore > targetScore + 14.0 ? 1 : (targetScore > iskandarScore + 22.0 ? -1 : 0);
      TYPE_MOON_WORLD.queueServerWork(duration, () -> finishOffscreenIonioiDuel(this, target, outcome));
   }

   private static void hideForIonioiOffscreenDuel(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      data.putBoolean(TAG_IONIOI_OFFSCREEN_DUEL, true);
      data.putBoolean(TAG_IONIOI_OFFSCREEN_PREVIOUS_INVISIBLE, living.isInvisible());
      data.putBoolean(TAG_IONIOI_OFFSCREEN_PREVIOUS_INVULNERABLE, living.isInvulnerable());
      if (living instanceof Mob mob) {
         data.putBoolean(TAG_IONIOI_OFFSCREEN_PREVIOUS_NO_AI, mob.isNoAi());
         mob.setNoAi(true);
         mob.getNavigation().stop();
      }
      living.setInvisible(true);
      living.setInvulnerable(true);
      living.setDeltaMovement(Vec3.ZERO);
      living.hurtMarked = true;
   }

   private static void finishOffscreenIonioiDuel(IskandarEntity entity, LivingEntity target, int outcome) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = entity.position().add(target.position()).scale(0.5);
      restoreFromIonioiOffscreenDuel(entity);
      restoreFromIonioiOffscreenDuel(target);
      if (!entity.isAlive() || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         return;
      }
      level.sendParticles(ParticleTypes.DUST_PLUME, center.x, center.y + 0.2, center.z, 48, 2.4, 0.2, 2.4, 0.07);
      level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 0.8, entity.getZ(), 16, 0.35, 0.35, 0.35, 0.04);
      level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 0.8, target.getZ(), 16, 0.35, 0.35, 0.35, 0.04);
      level.playSound(null, BlockPos.containing(center), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.72F);

      if (outcome > 0) {
         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().mobAttack(entity), Math.max(target.getMaxHealth() + 1.0F, 100.0F));
         target.invulnerableTime = 0;
         entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, false, true));
      } else if (outcome < 0) {
         entity.invulnerableTime = 0;
         entity.hurt(target.damageSources().mobAttack(target), Math.max(entity.getMaxHealth() * 0.75F, 80.0F));
         entity.invulnerableTime = 0;
         target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, false, true));
      } else {
         entity.setHealth(Math.max(1.0F, entity.getHealth() * 0.68F));
         target.setHealth(Math.max(1.0F, target.getHealth() * 0.42F));
         entity.setTarget(target);
         if (target instanceof Mob mobTarget) {
            mobTarget.setTarget(entity);
         }
      }
   }

   private static void restoreFromIonioiOffscreenDuel(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      if (!data.getBoolean(TAG_IONIOI_OFFSCREEN_DUEL)) {
         return;
      }
      living.setInvisible(data.getBoolean(TAG_IONIOI_OFFSCREEN_PREVIOUS_INVISIBLE));
      living.setInvulnerable(data.getBoolean(TAG_IONIOI_OFFSCREEN_PREVIOUS_INVULNERABLE));
      if (living instanceof Mob mob) {
         mob.setNoAi(data.getBoolean(TAG_IONIOI_OFFSCREEN_PREVIOUS_NO_AI));
      }
      clearIonioiOffscreenDuelState(living);
   }

   private static void clearIonioiOffscreenDuelState(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      data.remove(TAG_IONIOI_OFFSCREEN_DUEL);
      data.remove(TAG_IONIOI_OFFSCREEN_PREVIOUS_INVISIBLE);
      data.remove(TAG_IONIOI_OFFSCREEN_PREVIOUS_INVULNERABLE);
      data.remove(TAG_IONIOI_OFFSCREEN_PREVIOUS_NO_AI);
   }

   private static double duelScore(LivingEntity living) {
      double score = living.getHealth() / Math.max(1.0F, living.getMaxHealth()) * 35.0;
      score += living.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.4;
      score += living.getAttributeValue(Attributes.ARMOR) * 0.75;
      score += living.getAttributeValue(Attributes.MOVEMENT_SPEED) * 55.0;
      if (living instanceof ServantEntity servant && servant.getDefinition() != null) {
         ServantParams params = servant.getDefinition().parameters();
         score += effectiveRank(params.strength(), params.strengthPlus()) * 0.45;
         score += effectiveRank(params.agility(), params.agilityPlus()) * 0.38;
         score += effectiveRank(params.magic(), params.magicPlus()) * 0.28;
         score += effectiveRank(params.luck(), params.luckPlus()) * 0.18;
      }
      return score;
   }

   private static int effectiveRank(StatRank rank, boolean plus) {
      return plus ? rank.plusCoefficient() : rank.coefficient();
   }

   private void spawnInitialFormation(ServerLevel level) {
      for (int i = 0; i < IONIOI_ACTIVE_CAP; i++) {
         int row = i / 20;
         int col = i % 20;
         double x = (col - 9.5) * 1.4;
         double z = 5.0 + row * 1.4;
         spawnNextSoldier(level, this.position().add(x, 0.0, z));
      }
   }

   private void replenishSoldiers(ServerLevel level) {
      CompoundTag data = this.getPersistentData();
      while (this.ionioiSoldiers.size() < IONIOI_ACTIVE_CAP
         && data.getInt(TAG_IONIOI_NEXT_INDEX) < IonioiHetairoiRankPool.TOTAL_SIZE
         && data.getInt(TAG_IONIOI_DEATHS) < IonioiHetairoiRankPool.TOTAL_SIZE) {
         Vec3 pos = this.position().add((this.getRandom().nextDouble() - 0.5) * 18.0, 0.0, 6.0 + this.getRandom().nextDouble() * 16.0);
         spawnNextSoldier(level, pos);
      }
   }

   private void spawnNextSoldier(ServerLevel level, Vec3 pos) {
      CompoundTag data = this.getPersistentData();
      int index = data.getInt(TAG_IONIOI_NEXT_INDEX);
      if (index >= IonioiHetairoiRankPool.TOTAL_SIZE) {
         return;
      }
      IonioiHetairoiRankPool pool = getOrCreateIonioiPool();
      StatRank rank = pool.rankAt(index);
      MacedonianSoldierEntity soldier = ModEntities.MACEDONIAN_SOLDIER.get().create(level);
      if (soldier == null) {
         return;
      }
      soldier.moveTo(pos.x, this.getY(), pos.z, this.getYRot(), 0.0F);
      soldier.initializeForIonioiHetairoi(this, rank, index);
      level.addFreshEntity(soldier);
      this.ionioiSoldiers.add(soldier.getUUID());
      data.putInt(TAG_IONIOI_NEXT_INDEX, index + 1);
   }

   private void pruneDeadSoldiers(ServerLevel level) {
      Iterator<UUID> iterator = this.ionioiSoldiers.iterator();
      while (iterator.hasNext()) {
         UUID id = iterator.next();
         Entity entity = level.getEntity(id);
         if (!(entity instanceof MacedonianSoldierEntity soldier) || !soldier.isAlive()) {
            iterator.remove();
         }
      }
   }

   public void onMacedonianSoldierDeath(MacedonianSoldierEntity soldier) {
      if (!(this.level() instanceof ServerLevel)) {
         return;
      }
      CompoundTag data = this.getPersistentData();
      if (!data.getBoolean(TAG_IONIOI_ACTIVE)) {
         return;
      }
      data.putInt(TAG_IONIOI_DEATHS, Math.min(IonioiHetairoiRankPool.TOTAL_SIZE, data.getInt(TAG_IONIOI_DEATHS) + 1));
      this.ionioiSoldiers.remove(soldier.getUUID());
   }

   private void endIonioiHetairoi(ServerLevel level) {
      this.getPersistentData().putBoolean(TAG_IONIOI_ACTIVE, false);
      returnIonioiTargets(level);
      discardLocalMounts(level);
      for (UUID id : List.copyOf(this.ionioiSoldiers)) {
         Entity entity = level.getEntity(id);
         if (entity instanceof MacedonianSoldierEntity soldier) {
            soldier.discard();
         }
      }
      this.ionioiSoldiers.clear();
      level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 60, 10.0, 2.0, 10.0, 0.12);
      if (ModDimensions.isIonioiHetairoiDimension(level.dimension().location())) {
         CompoundTag data = this.getPersistentData();
         ServerLevel returnLevel = resolveDimensionOrOverworld(level, data.getString(TAG_IONIOI_RETURN_DIM));
         Vec3 returnPos = new Vec3(data.getDouble(TAG_IONIOI_RETURN_X), data.getDouble(TAG_IONIOI_RETURN_Y), data.getDouble(TAG_IONIOI_RETURN_Z));
         clearIonioiReturnData(this);
         if (this.isAlive()) {
            Entity moved = this.changeDimension(new DimensionTransition(returnLevel, returnPos, Vec3.ZERO, this.getYRot(), this.getXRot(), DimensionTransition.DO_NOTHING));
            if (moved instanceof IskandarEntity returned) {
               clearIonioiReturnData(returned);
               returned.ionioiSoldiers.clear();
               returned.ionioiPool = null;
               returned.bucephalusUuid = null;
               returned.gordiusWheelUuid = null;
               returned.getPersistentData().putLong(TAG_BUCEPHALUS_SUMMON_COOLDOWN, returnLevel.getGameTime() + MOUNT_SUMMON_COOLDOWN);
            }
         }
      } else {
         clearIonioiReturnData(this);
      }
   }

   public boolean isIonioiHetairoiActive() {
      return this.getPersistentData().getBoolean(TAG_IONIOI_ACTIVE);
   }

   private IonioiHetairoiRankPool getOrCreateIonioiPool() {
      if (this.ionioiPool == null) {
         this.ionioiPool = IonioiHetairoiRankPool.create(this.getPersistentData().getLong(TAG_IONIOI_SEED));
      }
      return this.ionioiPool;
   }

   private List<LivingEntity> collectIonioiTargets(ServerLevel source, LivingEntity primary) {
      List<LivingEntity> targets = new ArrayList<>();
      if (isIonioiPullTarget(primary, source)) {
         targets.add(primary);
      }
      for (LivingEntity living : source.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(32.0),
         living -> living != primary && isIonioiPullTarget(living, source))) {
         targets.add(living);
      }
      return targets;
   }

   private boolean isIonioiPullTarget(@Nullable LivingEntity living, ServerLevel source) {
      return living != null
         && living.isAlive()
         && living != this
         && living.level() == source
         && !EntityUtils.isImmunePlayerTarget(living)
         && (living == this.getTarget() || !this.isAlliedTo(living));
   }

   @Nullable
   private LivingEntity moveIonioiTargets(ServerLevel source, ServerLevel ionioiLevel, List<LivingEntity> targets, LivingEntity primary, Vec3 entry) {
      LivingEntity movedPrimary = null;
      for (LivingEntity living : targets) {
         LivingEntity moved = moveOneIonioiTarget(source, ionioiLevel, living, entry, living == primary);
         if (living == primary && moved != null) {
            movedPrimary = moved;
         }
      }
      return movedPrimary;
   }

   @Nullable
   private LivingEntity moveOneIonioiTarget(ServerLevel source, ServerLevel ionioiLevel, LivingEntity living, Vec3 entry, boolean primaryTarget) {
      if (living == null || !living.isAlive() || living == this || living.level() != source) {
         return null;
      }
      double relX = Mth.clamp(living.getX() - this.getX(), -24.0, 24.0);
      double relZ = Mth.clamp(living.getZ() - this.getZ(), -24.0, 24.0);
      double targetX = entry.x + relX;
      double targetZ = entry.z + relZ;
      double targetY = findSafeSpawnY(ionioiLevel, Mth.floor(targetX), Mth.floor(targetZ));
      double returnX = living.getX();
      double returnY = living.getY();
      double returnZ = living.getZ();
      markIonioiTarget(living, source, returnX, returnY, returnZ, primaryTarget);
      Entity moved = living.changeDimension(new DimensionTransition(ionioiLevel, new Vec3(targetX, targetY, targetZ), Vec3.ZERO, living.getYRot(), living.getXRot(), DimensionTransition.DO_NOTHING));
      if (moved instanceof LivingEntity movedLiving) {
         markIonioiTarget(movedLiving, source, returnX, returnY, returnZ, primaryTarget);
         return movedLiving;
      }
      clearIonioiTarget(living);
      return null;
   }

   private void markIonioiTarget(LivingEntity living, ServerLevel source, double returnX, double returnY, double returnZ, boolean primaryTarget) {
      CompoundTag data = living.getPersistentData();
      data.putUUID(TAG_IONIOI_TARGET_OWNER, this.getUUID());
      data.putString(TAG_IONIOI_TARGET_RETURN_DIM, source.dimension().location().toString());
      data.putDouble(TAG_IONIOI_TARGET_RETURN_X, returnX);
      data.putDouble(TAG_IONIOI_TARGET_RETURN_Y, returnY);
      data.putDouble(TAG_IONIOI_TARGET_RETURN_Z, returnZ);
      if (primaryTarget) {
         data.putBoolean(TAG_IONIOI_TARGET_PRIMARY, true);
      } else {
         data.remove(TAG_IONIOI_TARGET_PRIMARY);
      }
   }

   private void returnIonioiTargets(ServerLevel sourceLevel) {
      List<LivingEntity> toReturn = new ArrayList<>();
      for (Entity candidate : sourceLevel.getEntities().getAll()) {
         if (candidate instanceof LivingEntity living && living.isAlive() && isPulledByIonioi(this.getUUID(), living)) {
            toReturn.add(living);
         }
      }
      for (LivingEntity living : toReturn) {
         CompoundTag data = living.getPersistentData();
         ServerLevel returnLevel = resolveDimensionOrOverworld(sourceLevel, data.getString(TAG_IONIOI_TARGET_RETURN_DIM));
         Vec3 returnPos = new Vec3(data.getDouble(TAG_IONIOI_TARGET_RETURN_X), data.getDouble(TAG_IONIOI_TARGET_RETURN_Y), data.getDouble(TAG_IONIOI_TARGET_RETURN_Z));
         clearIonioiTarget(living);
         Entity moved = living.changeDimension(new DimensionTransition(returnLevel, returnPos, Vec3.ZERO, living.getYRot(), living.getXRot(), DimensionTransition.DO_NOTHING));
         if (moved instanceof LivingEntity movedLiving) {
            clearIonioiTarget(movedLiving);
         }
      }
   }

   private static boolean isPulledByIonioi(UUID ownerId, LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      return data.hasUUID(TAG_IONIOI_TARGET_OWNER) && ownerId.equals(data.getUUID(TAG_IONIOI_TARGET_OWNER));
   }

   private static void clearIonioiTarget(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      data.remove(TAG_IONIOI_TARGET_OWNER);
      data.remove(TAG_IONIOI_TARGET_RETURN_DIM);
      data.remove(TAG_IONIOI_TARGET_RETURN_X);
      data.remove(TAG_IONIOI_TARGET_RETURN_Y);
      data.remove(TAG_IONIOI_TARGET_RETURN_Z);
      data.remove(TAG_IONIOI_TARGET_PRIMARY);
   }

   private void discardLocalMounts(ServerLevel level) {
      if (this.isPassenger()) {
         this.stopRiding();
      }
      discardBucephalus(level);
      discardGordiusWheel(level);
   }

   void discardBucephalus(ServerLevel level) {
      BucephalusEntity horse = getBucephalus(level);
      if (horse != null) {
         horse.ejectPassengers();
         horse.discard();
      }
      this.bucephalusUuid = null;
      markBucephalusLost(level);
   }

   void discardGordiusWheel(ServerLevel level) {
      GordiusWheelEntity wheel = getGordiusWheel(level);
      if (wheel != null) {
         wheel.ejectPassengers();
         wheel.discard();
      }
      this.gordiusWheelUuid = null;
   }

   private static void clearIonioiReturnData(IskandarEntity iskandar) {
      CompoundTag data = iskandar.getPersistentData();
      data.putBoolean(TAG_IONIOI_ACTIVE, false);
      data.remove(TAG_IONIOI_RETURN_DIM);
      data.remove(TAG_IONIOI_RETURN_X);
      data.remove(TAG_IONIOI_RETURN_Y);
      data.remove(TAG_IONIOI_RETURN_Z);
   }

   private Vec3 randomIonioiEntry() {
      double x = (this.getRandom().nextDouble() - 0.5) * 4096.0;
      double z = (this.getRandom().nextDouble() - 0.5) * 4096.0;
      return new Vec3(x, 72.0, z);
   }

   private static int findSafeSpawnY(ServerLevel level, int x, int z) {
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, level.getMaxBuildHeight() - 2, z);
      for (int y = level.getMaxBuildHeight() - 2; y >= level.getMinBuildHeight() + 1; y--) {
         cursor.setY(y);
         if (!level.getBlockState(cursor).isAir() && level.getBlockState(cursor.above()).isAir() && level.getBlockState(cursor.above(2)).isAir()) {
            return y + 1;
         }
      }
      return Mth.clamp(72, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
   }

   private static ServerLevel resolveDimensionOrOverworld(ServerLevel current, String id) {
      if (id != null && !id.isBlank()) {
         ResourceLocation location = ResourceLocation.tryParse(id);
         if (location != null) {
            ServerLevel level = current.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, location));
            if (level != null) {
               return level;
            }
         }
      }
      return current.getServer().overworld();
   }

   private void updateMountArmor() {
      AttributeInstance armor = this.getAttribute(Attributes.ARMOR);
      if (armor == null) {
         return;
      }
      AttributeModifier current = armor.getModifier(MOUNT_ARMOR_ID);
      boolean active = this.isPassenger() && this.getVehicle() instanceof IskandarMountEntity;
      if (!active) {
         if (current != null) {
            armor.removeModifier(MOUNT_ARMOR_ID);
         }
         return;
      }
      if (current == null) {
         armor.addTransientModifier(new AttributeModifier(MOUNT_ARMOR_ID, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (this.getPersistentData().getLong(TAG_MILITARY_TACTICS_UNTIL) > this.level().getGameTime()
         && net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmDamageClassifier.isNoblePhantasmDamage(source, amount)) {
         amount *= 0.70F;
      }
      return super.hurt(source, amount);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      boolean success = super.doHurtTarget(target);
      if (success) {
         ServantVoiceHelper.tryPlayAttack(this);
      }
      return success;
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) {
         return true;
      }
      if (other instanceof MacedonianSoldierEntity soldier && soldier.getPersistentData().hasUUID("IonioiHetairoiOwner")) {
         return this.getUUID().equals(soldier.getPersistentData().getUUID("IonioiHetairoiOwner"));
      }
      return other instanceof IskandarMountEntity mount && mount.isBoundCompanion(this);
   }

   @Override
   public void die(DamageSource cause) {
      if (this.level() instanceof ServerLevel level) {
         endIonioiHetairoi(level);
         if (getBucephalus(level) != null) getBucephalus(level).discard();
         if (getGordiusWheel(level) != null) getGordiusWheel(level).discard();
      }
      super.die(cause);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.bucephalusUuid != null) tag.putUUID("IskandarBucephalus", this.bucephalusUuid);
      if (this.gordiusWheelUuid != null) tag.putUUID("IskandarGordiusWheel", this.gordiusWheelUuid);
      tag.putInt("IskandarIonioiSoldierCount", this.ionioiSoldiers.size());
      for (int i = 0; i < this.ionioiSoldiers.size(); i++) {
         tag.putUUID("IskandarIonioiSoldier" + i, this.ionioiSoldiers.get(i));
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID("IskandarBucephalus")) this.bucephalusUuid = tag.getUUID("IskandarBucephalus");
      if (tag.hasUUID("IskandarGordiusWheel")) this.gordiusWheelUuid = tag.getUUID("IskandarGordiusWheel");
      this.ionioiSoldiers.clear();
      int count = tag.getInt("IskandarIonioiSoldierCount");
      for (int i = 0; i < count; i++) {
         String key = "IskandarIonioiSoldier" + i;
         if (tag.hasUUID(key)) {
            this.ionioiSoldiers.add(tag.getUUID(key));
         }
      }
      this.ionioiPool = null;
   }

   @Nullable
   BucephalusEntity getBucephalus(ServerLevel level) {
      if (this.bucephalusUuid == null) {
         return null;
      }
      Entity entity = level.getEntity(this.bucephalusUuid);
      if (entity instanceof BucephalusEntity horse && horse.isAlive()) {
         return horse;
      }
      this.bucephalusUuid = null;
      markBucephalusLost(level);
      return null;
   }

   @Nullable
   GordiusWheelEntity getGordiusWheel(ServerLevel level) {
      if (this.gordiusWheelUuid == null) {
         return null;
      }
      Entity entity = level.getEntity(this.gordiusWheelUuid);
      if (entity instanceof GordiusWheelEntity wheel && wheel.isAlive()) {
         return wheel;
      }
      this.gordiusWheelUuid = null;
      return null;
   }

   private void markBucephalusLost(ServerLevel level) {
      this.getPersistentData().putLong(TAG_BUCEPHALUS_SUMMON_COOLDOWN, level.getGameTime() + MOUNT_SUMMON_COOLDOWN);
   }

   public static boolean isIonioiArea(BlockPos pos) {
      return false;
   }
}

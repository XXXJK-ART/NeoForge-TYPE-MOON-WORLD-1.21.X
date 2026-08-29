package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanPursuitData;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanRules;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class ShadowHassanEntity extends ServantEntity {
   public static final String SERVANT_KEY = "shadow_hassan";
   private static final EntityDataAccessor<Boolean> PRESENCE_CONCEALED = SynchedEntityData.defineId(ShadowHassanEntity.class, EntityDataSerializers.BOOLEAN);
   private static final String TAG_MASTER = "ShadowHassanMaster";
   private static final String TAG_EXPOSED_UNTIL = "ShadowHassanExposedUntil";
   private static final String TAG_NP_CONSUMED = "ShadowHassanNpConsumed";
   private static final String TAG_NEXT_SHADOW_STEP = "ShadowHassanNextShadowStep";
   private static final String TAG_CONCEALED = "ShadowHassanPresenceConcealed";
   private static final String TAG_DEATH_VOICE_PLAYED = "ShadowHassanDeathVoicePlayed";
   private UUID masterUuid;
   private boolean internalDeath;

   public ShadowHassanEntity(EntityType<? extends ShadowHassanEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   public void bindMaster(ServerPlayer master) {
      super.bindMaster(master);
      this.masterUuid = master.getUUID();
   }

   @Override
   public void unbindMaster() {
      super.unbindMaster();
      this.masterUuid = null;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(PRESENCE_CONCEALED, false);
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                       @Nullable SpawnGroupData spawnData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
      this.equipMask();
      this.setPresenceConcealed(false);
      return result;
   }

   @Override
   protected void customServerAiStep() {
      if (this.isTotalDarkness()) {
         this.getNavigation().stop();
         this.setTarget(null);
         this.setDeltaMovement(Vec3.ZERO);
         return;
      }
      super.customServerAiStep();
   }

   @Override
   public void tick() {
      super.tick();
      if (this.isPresenceConcealed() && this.tickCount % 20 == 0) this.suppressRevealingEffects();
      if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) return;
      long now = level.getGameTime();
      ShadowHassanPursuitData saved = ShadowHassanPursuitData.get(level.getServer());
      if (saved.isConsumed(this.getUUID())) {
         this.consumeForMasterDeath();
         return;
      }
      if (this.tickCount % 20 == 0) saved.updateServant(this);

      LivingEntity target = this.getTarget();
      boolean hasCombatTarget = target != null && target.isAlive() && !target.isRemoved() && target.level() == level;
      boolean exposed = now < this.getPersistentData().getLong(TAG_EXPOSED_UNTIL);
      this.setPresenceConcealed(hasCombatTarget && !exposed);
      if (this.isTotalDarkness()) {
         this.getNavigation().stop();
         this.setDeltaMovement(Vec3.ZERO);
         return;
      }
      if (this.isPresenceConcealed()) this.clearEnemyAggro(level);
      else if (this.tickCount % 4 == 0) this.spawnVisibleParticles(level);

      if (this.tickCount % ShadowHassanRules.MANA_RESTORE_INTERVAL_TICKS == 0 && this.isInShadow()) {
         this.setCurrentMp(Math.min(this.getMaxMp(), this.getCurrentMp() + ShadowHassanRules.MANA_RESTORE_PER_SECOND));
      }

      if (!this.wasTacticalAiHandledThisTick()) {
         this.tickShadowMovement(level, target, now);
      }
   }

   public boolean isPresenceConcealed() {
      return this.entityData.get(PRESENCE_CONCEALED);
   }

   private void setPresenceConcealed(boolean concealed) {
      boolean changed = this.entityData.get(PRESENCE_CONCEALED) != concealed;
      if (changed) this.entityData.set(PRESENCE_CONCEALED, concealed);
      this.setInvisible(concealed);
      this.setSilent(concealed);
      this.setCustomNameVisible(!concealed);
      if (concealed) {
         if (changed) this.suppressRevealingEffects();
         MobEffectInstance current = this.getEffect(MobEffects.INVISIBILITY);
         if (current == null || current.getDuration() <= 10) {
            this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, false));
         }
      } else if (this.hasEffect(MobEffects.INVISIBILITY)) {
         this.removeEffect(MobEffects.INVISIBILITY);
      }
   }

   private void suppressRevealingEffects() {
      this.setGlowingTag(false);
      if (!this.level().isClientSide()) {
         for (MobEffectInstance effect : java.util.List.copyOf(this.getActiveEffects())) {
            if (effect.getEffect().is(ShadowHassanRules.REVEALING_EFFECTS)) this.removeEffect(effect.getEffect());
         }
      }
   }

   public void revealForAttack() {
      if (this.level().isClientSide()) return;
      this.getPersistentData().putLong(TAG_EXPOSED_UNTIL, this.level().getGameTime() + ShadowHassanRules.CONCEALMENT_EXPOSURE_TICKS);
      this.setPresenceConcealed(false);
   }

   private void clearEnemyAggro(ServerLevel level) {
      for (Mob mob : level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(48.0), mob -> mob != this && mob.getTarget() == this)) {
         mob.setTarget(null);
         mob.getNavigation().stop();
      }
   }

   private void spawnVisibleParticles(ServerLevel level) {
      level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 0.9, this.getZ(), 3, 0.28, 0.82, 0.28, 0.01);
      level.sendParticles(new DustParticleOptions(new Vector3f(0.015F, 0.015F, 0.02F), 1.35F),
         this.getX(), this.getY() + 0.9, this.getZ(), 4, 0.3, 0.86, 0.3, 0.005);
      level.sendParticles(ParticleTypes.ASH, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.24, 0.7, 0.24, 0.008);
   }

   private void tickShadowMovement(ServerLevel level, @Nullable LivingEntity target, long now) {
      boolean targetCastsShadow = target != null && ShadowHassanRules.castsSunlightShadow(level, target);
      if (!this.isInShadow() && !targetCastsShadow) return;
      long next = this.getPersistentData().getLong(TAG_NEXT_SHADOW_STEP);
      if (target != null && target.isAlive() && now >= next
         && (this.hurtTime > 0 || this.getHealth() <= this.getMaxHealth() * 0.25F)
         && this.teleportToRetreatShadow(level, target)) {
         this.getPersistentData().putLong(TAG_NEXT_SHADOW_STEP, now + ShadowHassanRules.SHADOW_STEP_COOLDOWN_TICKS);
         return;
      }
      if (target != null && target.isAlive() && now >= next) {
         Vec3 destination = this.shadowAttackPosition(target, now);
         if (this.teleportToNearbyShadow(level, destination, 5, target)) {
            this.getPersistentData().putLong(TAG_NEXT_SHADOW_STEP, now + ShadowHassanRules.SHADOW_STEP_COOLDOWN_TICKS);
            return;
         }
      }
      LivingEntity master = this.getMaster();
      if (master != null && this.distanceToSqr(master) > 24.0 * 24.0 && now >= next
         && this.teleportToNearbyShadow(level, master.position(), 5, null)) {
         this.getPersistentData().putLong(TAG_NEXT_SHADOW_STEP, now + ShadowHassanRules.SHADOW_STEP_COOLDOWN_TICKS);
      }
   }

   private Vec3 shadowAttackPosition(LivingEntity target, long now) {
      Vec3 forward = target.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-5) forward = new Vec3(0.0, 0.0, 1.0);
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      return switch ((int)(now / ShadowHassanRules.SHADOW_STEP_COOLDOWN_TICKS) % 3) {
         case 1 -> target.position().add(side.scale(2.0));
         case 2 -> target.position().add(forward.scale(-1.5)).add(side.scale(-1.5));
         default -> target.position().add(forward.scale(-2.0));
      };
   }

   private boolean teleportToRetreatShadow(ServerLevel level, LivingEntity threat) {
      BlockPos center = this.blockPosition();
      BlockPos best = null;
      double bestDistance = this.distanceToSqr(threat);
      for (int attempt = 0; attempt < 64; attempt++) {
         BlockPos pos = center.offset(this.random.nextIntBetweenInclusive(-16, 16),
            this.random.nextIntBetweenInclusive(-3, 3), this.random.nextIntBetweenInclusive(-16, 16));
         if (pos.distSqr(center) > 16.0 * 16.0) continue;
         if (!isValidShadowDestination(level, pos)) continue;
         double x = pos.getX() + 0.5;
         double y = pos.getY();
         double z = pos.getZ() + 0.5;
         if (!level.noCollision(this, this.getBoundingBox().move(x - this.getX(), y - this.getY(), z - this.getZ()))) continue;
         double distance = threat.distanceToSqr(x, y, z);
         if (distance > bestDistance) {
            bestDistance = distance;
            best = pos;
         }
      }
      if (best == null) return false;
      this.teleportTo(best.getX() + 0.5, best.getY(), best.getZ() + 0.5);
      return true;
   }

   private boolean teleportToNearbyShadow(ServerLevel level, Vec3 center, int radius, @Nullable LivingEntity faceTarget) {
      BlockPos base = BlockPos.containing(center);
      boolean targetCastsShadow = faceTarget != null && ShadowHassanRules.castsSunlightShadow(level, faceTarget);
      for (int attempt = 0; attempt < 36; attempt++) {
         BlockPos pos = attempt == 0 ? base : base.offset(this.random.nextIntBetweenInclusive(-radius, radius),
            this.random.nextIntBetweenInclusive(-2, 2), this.random.nextIntBetweenInclusive(-radius, radius));
         if (!isValidShadowDestination(level, pos, targetCastsShadow ? faceTarget : null)) continue;
         double x = pos.getX() + 0.5;
         double y = pos.getY();
         double z = pos.getZ() + 0.5;
         if (!level.noCollision(this, this.getBoundingBox().move(x - this.getX(), y - this.getY(), z - this.getZ()))) continue;
         this.teleportTo(x, y, z);
         if (faceTarget != null) this.getLookControl().setLookAt(faceTarget, 90.0F, 90.0F);
         return true;
      }
      return false;
   }

   private static boolean isValidShadowDestination(ServerLevel level, BlockPos pos) {
      return isValidShadowDestination(level, pos, null);
   }

   private static boolean isValidShadowDestination(ServerLevel level, BlockPos pos, @Nullable LivingEntity sunlightShadowCaster) {
      boolean naturalShadow = ShadowHassanRules.isShadowLight(level.getMaxLocalRawBrightness(pos));
      boolean castShadow = sunlightShadowCaster != null
         && pos.distSqr(sunlightShadowCaster.blockPosition()) <= 16.0;
      return (naturalShadow || castShadow)
         && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
         && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
         && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
   }

   private boolean isInShadow() {
      return ShadowHassanRules.isShadowLight(this.level().getMaxLocalRawBrightness(this.blockPosition()));
   }

   private boolean isTotalDarkness() {
      return ShadowHassanRules.isTotalDarkness(this.level().getMaxLocalRawBrightness(this.blockPosition()));
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (this.isTotalDarkness()) return false;
      boolean ambush = this.isPresenceConcealed();
      this.revealForAttack();
      if (target.getType().is(ShadowHassanDamageTypes.BLADE_IMMUNE)) return false;
      boolean hit = super.doHurtTarget(target);
      if (hit && target instanceof LivingEntity living) ShadowHassanCombatHelper.onSuccessfulAttack(this, living, ambush);
      return hit;
   }

   @Override
   public void setTarget(@Nullable LivingEntity target) {
      LivingEntity previous = this.getTarget();
      super.setTarget(target);
      if (!this.level().isClientSide() && this.masterUuid != null && target != null && target != previous) {
         ServantVoiceHelper.tryPlayShadowHassanCommandAttack(this);
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (amount <= 0.0F) return false;
      if (!this.isAdministrativeDamage(source) && !this.isEligibleMeleeDamage(source)) return false;
      return super.hurt(source, amount);
   }

   private boolean isEligibleMeleeDamage(DamageSource source) {
      LivingEntity attacker = this.resolveAttacker(source);
      if (attacker == null) return false;
      return attacker.distanceToSqr(this) <= ShadowHassanRules.MAX_MELEE_DISTANCE * ShadowHassanRules.MAX_MELEE_DISTANCE;
   }

   @Nullable
   private LivingEntity resolveAttacker(DamageSource source) {
      if (source.getEntity() instanceof LivingEntity living) return living;
      if (source.getDirectEntity() instanceof LivingEntity living) return living;
      if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) return owner;
      return null;
   }

   private boolean isAdministrativeDamage(DamageSource source) {
      return source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD);
   }

   @Override
   public void die(DamageSource cause) {
      if (this.internalDeath) {
         this.playDeathVoiceOnce();
         super.die(cause);
         return;
      }
      if (!this.isAdministrativeDamage(cause) && !this.isEligibleMeleeDamage(cause)) {
         this.getPersistentData().remove("CausalSevered");
         this.setHealth(Math.max(1.0F, this.getHealth()));
         return;
      }
      if (!this.isNoblePhantasmConsumed() && cause.getEntity() instanceof LivingEntity killer
         && !ShadowHassanPursuitData.isPaleRider(killer)) {
         this.consumeNoblePhantasm();
         if (this.level() instanceof ServerLevel level) ShadowHassanPursuitData.get(level.getServer()).addPursuit(level, this.position(), killer);
      }
      this.playDeathVoiceOnce();
      super.die(cause);
   }

   private void consumeNoblePhantasm() {
      this.getPersistentData().putBoolean(TAG_NP_CONSUMED, true);
      if (this.level() instanceof ServerLevel level) ShadowHassanPursuitData.get(level.getServer()).consumeServant(this.getUUID());
   }

   private boolean isNoblePhantasmConsumed() {
      return this.getPersistentData().getBoolean(TAG_NP_CONSUMED);
   }

   public void consumeForMasterDeath() {
      if (!this.isAlive()) return;
      this.getPersistentData().putBoolean(TAG_NP_CONSUMED, true);
      this.playDeathVoiceOnce();
      this.internalDeath = true;
      this.setHealth(0.0F);
      super.die(this.damageSources().genericKill());
   }

   private void playDeathVoiceOnce() {
      if (this.getPersistentData().getBoolean(TAG_DEATH_VOICE_PLAYED)) return;
      this.getPersistentData().putBoolean(TAG_DEATH_VOICE_PLAYED, true);
      ServantVoiceHelper.tryPlayShadowHassanMeditativeSensitivity(this);
   }

   @Override
   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (stack.is(ModItems.SERVANT_MASTER_CONTRACT.get()) && player instanceof ServerPlayer serverPlayer
         && this.level() instanceof ServerLevel level) {
         if (this.masterUuid != null && !this.masterUuid.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_hassan.contract_occupied"), true);
            return InteractionResult.FAIL;
         }
         if (!ShadowHassanPursuitData.get(level.getServer()).bind(this, serverPlayer)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_hassan.contract_occupied"), true);
            return InteractionResult.FAIL;
         }
         this.masterUuid = player.getUUID();
         if (!player.getAbilities().instabuild) stack.shrink(1);
         player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_hassan.contract"), true);
         return InteractionResult.SUCCESS;
      }
      return super.mobInteract(player, hand);
   }

   @Nullable
   public UUID getMasterUuid() {
      return this.masterUuid;
   }

   @Nullable
   public LivingEntity getMaster() {
      if (this.masterUuid == null || !(this.level() instanceof ServerLevel level)) return null;
      Entity master = level.getServer().getPlayerList().getPlayer(this.masterUuid);
      return master instanceof LivingEntity living ? living : null;
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      return super.isAlliedTo(other) || this.masterUuid != null && this.masterUuid.equals(other.getUUID());
   }

   @Override
   public boolean isInvisibleTo(Player player) {
      return this.isPresenceConcealed() || super.isInvisibleTo(player);
   }

   @Override
   public boolean isCustomNameVisible() {
      return !this.isPresenceConcealed() && super.isCustomNameVisible();
   }

   @Override
   public boolean isCurrentlyGlowing() {
      return !this.isPresenceConcealed() && super.isCurrentlyGlowing();
   }

   @Override
   public boolean displayFireAnimation() {
      return !this.isPresenceConcealed() && super.displayFireAnimation();
   }

   private void equipMask() {
      if (!this.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SHADOW_HASSAN_MASK.get())) {
         this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SHADOW_HASSAN_MASK.get()));
         this.setDropChance(EquipmentSlot.HEAD, 0.0F);
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.masterUuid != null) tag.putUUID(TAG_MASTER, this.masterUuid);
      tag.putBoolean(TAG_NP_CONSUMED, this.isNoblePhantasmConsumed());
      tag.putLong(TAG_EXPOSED_UNTIL, this.getPersistentData().getLong(TAG_EXPOSED_UNTIL));
      tag.putLong(TAG_NEXT_SHADOW_STEP, this.getPersistentData().getLong(TAG_NEXT_SHADOW_STEP));
      tag.putBoolean(TAG_CONCEALED, this.isPresenceConcealed());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.masterUuid = tag.hasUUID(TAG_MASTER) ? tag.getUUID(TAG_MASTER) : null;
      this.getPersistentData().putBoolean(TAG_NP_CONSUMED, tag.getBoolean(TAG_NP_CONSUMED));
      this.getPersistentData().putLong(TAG_EXPOSED_UNTIL, tag.getLong(TAG_EXPOSED_UNTIL));
      this.getPersistentData().putLong(TAG_NEXT_SHADOW_STEP, tag.getLong(TAG_NEXT_SHADOW_STEP));
      this.setPresenceConcealed(tag.contains(TAG_CONCEALED) && tag.getBoolean(TAG_CONCEALED));
      this.equipMask();
   }
}

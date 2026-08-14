package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardIskandarSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatFormulas;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.IskandarEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;
import org.jetbrains.annotations.Nullable;

public class MacedonianSoldierEntity extends PathfinderMob {
   private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(MacedonianSoldierEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> VISUAL_SCALE = SynchedEntityData.defineId(MacedonianSoldierEntity.class, EntityDataSerializers.FLOAT);
   private static final int FORMATION_ACTIVE_CAP = 400;
   private static final int FORMATION_SIZE = 25;
   private static final int FORMATION_SIDE = 5;
   private static final int FORMATION_GRID_WIDTH = 8;
   private static final int FORMATION_GRID_DEPTH = 2;
   private static final double FORMATION_SPACING = 1.35;
   private static final double FORMATION_GAP = 2.4;
   private static final double FORMATION_ADVANCE_LIMIT = 28.0;
   private static final double FORMATION_KEEP_DISTANCE = 6.4;
   private static final double SURROUND_OWNER_DISTANCE = 16.0;
   private static final double SURROUND_SOLDIER_DISTANCE = 3.4;
   private static final double FORMATION_THREAT_SCAN_RADIUS = 96.0;
   private static final double SPEAR_REACH_BONUS = 3.0;
   private static final int SPEAR_ATTACK_INTERVAL_TICKS = 12;
   private static final int FORMATION_VOLLEY_INTERVAL_TICKS = 18;
   private static final int FORMATION_VOLLEY_WINDOW_TICKS = 8;
   private static final int PERSONAL_SPEAR_CHECK_STRIDE = 3;
   private static final double SPEAR_FORWARD_DOT_MIN = 0.38;
   private static final double SPEAR_LANE_HALF_WIDTH = 1.55;
   private static final int FORMATION_TARGET_SCAN_INTERVAL = 30;
   private static final int FORMATION_BRAIN_STRIDE = 4;
   private static final int FORMATION_DIRECT_PATH_REFRESH_TICKS = 12;
   private static final int FORMATION_IDLE_PATH_REFRESH_TICKS = 80;
   private static final double FORMATION_DIRECT_MOVE_STOP_DISTANCE_SQR = 0.85;
   private static final int EMBEDDED_RECOVERY_INTERVAL = 20;
   private static final int SERVANT_DISSOLVE_TICKS = 10;
   private static final double MIN_IONIOI_MOVEMENT_SPEED = StatRank.C.toMovementSpeed();
   private static final double FORMATION_ASSAULT_SPEED = 1.45;
   private static final String TAG_IONIOI_TARGET_OWNER = "IonioiHetairoiTargetOwner";
   private static final String TAG_IONIOI_TARGET_SESSION = "IonioiHetairoiTargetSession";
   private static final String TAG_IONIOI_SESSION = "IskandarIonioiSession";
   private static final String TAG_CARD_IONIOI_SESSION = "ServantCardIskandarIonioiSession";
   private static final String TAG_CARD_OWNER = "ServantCardIskandarOwner";
   private static final String TAG_CARD_UNTIL = "ServantCardIskandarUntil";
   private static final String TAG_CARD_IONIOI = "ServantCardIskandarIonioiSoldier";
   private static final Map<UUID, FormationTargetCache> FORMATION_TARGET_CACHE = new HashMap<>();
   private static final Map<UUID, FormationTargetCache> CARD_FORMATION_TARGET_CACHE = new HashMap<>();
   private static final Map<Long, CachedFormationY> FORMATION_Y_CACHE = new HashMap<>();
   @Nullable private UUID iskandarUuid;
   private int poolIndex = -1;
   private long actionStartTick;
   @Nullable private Vec3 lastPathSlot;
   private long nextPathRefreshTick;
   private int directSpearCooldown;

   public MacedonianSoldierEntity(EntityType<? extends MacedonianSoldierEntity> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, StatRank.E.toMaxHealth())
         .add(Attributes.ATTACK_DAMAGE, StatRank.E.toAttackDamage())
         .add(Attributes.MOVEMENT_SPEED, MIN_IONIOI_MOVEMENT_SPEED)
         .add(Attributes.ARMOR, 3.0)
         .add(Attributes.ATTACK_SPEED, 4.8)
         .add(Attributes.FOLLOW_RANGE, 64.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.45);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(RANK, StatRank.E.ordinal());
      builder.define(VISUAL_SCALE, 0.95F);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MacedonianSpearAttackGoal(this, 1.18, true));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData
   ) {
      SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
      this.equipPhalanxGear();
      this.setVisualScale(0.9F + this.getRandom().nextFloat() * 0.1F);
      return data;
   }

   public void initializeForIonioiHetairoi(IskandarEntity iskandar, StatRank rank, int poolIndex) {
      this.iskandarUuid = iskandar.getUUID();
      this.poolIndex = poolIndex;
      this.actionStartTick = iskandar.level().getGameTime() + IskandarEntity.IONIOI_FORMATION_DELAY_TICKS;
      this.setSoldierRank(rank);
      this.setVisualScale(0.9F + this.getRandom().nextFloat() * 0.1F);
      this.equipPhalanxGear();
      this.getPersistentData().putBoolean("IonioiHetairoiSoldier", true);
      this.getPersistentData().putUUID("IonioiHetairoiOwner", iskandar.getUUID());
      this.setTarget(null);
   }

   public void initializeForServantCardIskandar(ServerPlayer owner, @Nullable LivingEntity target, long until) {
      this.iskandarUuid = null;
      this.poolIndex = -1;
      this.actionStartTick = this.level().getGameTime() + 10L;
      this.setSoldierRank(StatRank.D);
      this.setVisualScale(0.9F + this.getRandom().nextFloat() * 0.1F);
      this.equipPhalanxGear();
      this.getPersistentData().putUUID(TAG_CARD_OWNER, owner.getUUID());
      this.getPersistentData().putLong(TAG_CARD_UNTIL, until);
      if (target != null && target.isAlive() && !this.isAlliedTo(target)) {
         this.setTarget(target);
      }
   }

   public void initializeForServantCardIonioi(ServerPlayer owner, @Nullable LivingEntity target, StatRank rank, int poolIndex) {
      this.iskandarUuid = null;
      this.poolIndex = poolIndex;
      this.actionStartTick = this.level().getGameTime() + IskandarEntity.IONIOI_FORMATION_DELAY_TICKS;
      this.setSoldierRank(rank);
      this.setVisualScale(0.9F + this.getRandom().nextFloat() * 0.1F);
      this.equipPhalanxGear();
      this.getPersistentData().putUUID(TAG_CARD_OWNER, owner.getUUID());
      this.getPersistentData().putBoolean(TAG_CARD_IONIOI, true);
      if (target != null && target.isAlive() && !this.isAlliedTo(target)) {
         this.setTarget(target);
      }
   }

   public boolean isServantCardIonioiSoldier(UUID ownerId) {
      CompoundTag data = this.getPersistentData();
      return data.getBoolean(TAG_CARD_IONIOI) && data.hasUUID(TAG_CARD_OWNER) && ownerId.equals(data.getUUID(TAG_CARD_OWNER));
   }

   public StatRank getSoldierRank() {
      int id = this.entityData.get(RANK);
      StatRank[] values = StatRank.values();
      return id >= 0 && id < values.length ? values[id] : StatRank.E;
   }

   public int getPoolIndex() {
      return this.poolIndex;
   }

   private void setSoldierRank(StatRank rank) {
      StatRank resolved = rank == null ? StatRank.E : rank;
      this.entityData.set(RANK, resolved.ordinal());
      ServantParams params = new ServantParams(resolved, false, resolved, false, resolved, false, resolved, false, resolved, false);
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(params.maxHealth());
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(params.attackDamage());
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Math.max(MIN_IONIOI_MOVEMENT_SPEED, params.movementSpeed()));
      this.getAttribute(Attributes.ARMOR).setBaseValue(params.armor());
      this.setHealth(this.getMaxHealth());
   }

   private void equipPhalanxGear() {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MACEDONIAN_SPEAR.get()));
      this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.MACEDONIAN_ROUND_SHIELD.get()));
      this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
      this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
   }

   public float getVisualScale() {
      return this.entityData.get(VISUAL_SCALE);
   }

   public static void clearFormationCache(UUID ownerId) {
      if (ownerId != null) {
         FORMATION_TARGET_CACHE.remove(ownerId);
         CARD_FORMATION_TARGET_CACHE.remove(ownerId);
         if (FORMATION_Y_CACHE.size() > 4096) {
            FORMATION_Y_CACHE.clear();
         }
      }
   }

   private void setVisualScale(float scale) {
      this.entityData.set(VISUAL_SCALE, net.minecraft.util.Mth.clamp(scale, 0.9F, 1.0F));
   }

   @Override
   protected void customServerAiStep() {
      if (this.directSpearCooldown > 0) {
         this.directSpearCooldown--;
      }
      if (tickServantCardVanguard()) {
         return;
      }
      if (this.iskandarUuid == null) {
         boolean tactical = net.xxxjk.TYPE_MOON_WORLD.combat.ai.NpcTacticalController.tick(this);
         if (!tactical) {
            super.customServerAiStep();
         }
         return;
      }
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      recoverIfEmbedded(level);
      IskandarEntity owner = this.getIskandar(level);
      if (owner == null || !owner.isAlive() || !owner.isIonioiHetairoiActive()) {
         if (this.iskandarUuid != null) {
            FORMATION_TARGET_CACHE.remove(this.iskandarUuid);
         }
         this.discard();
         return;
      }
      if (level.getGameTime() < this.actionStartTick) {
         this.setTarget(null);
         this.getNavigation().stop();
         this.setDeltaMovement(Vec3.ZERO);
         return;
      }
      if (!shouldRunFormationBrain()) {
         return;
      }
      LivingEntity target = this.formationTarget(owner, level);
      if (isValidTarget(target)) {
         if (this.getTarget() != null) {
            this.setTarget(null);
         }
         Vec3 slot = formationSurroundSlot(owner, target);
         moveDirectlyInFormation(slot, FORMATION_ASSAULT_SPEED, true);
         tryDirectSpearAttack(target);
      } else {
         if (this.getTarget() != null) {
            this.setTarget(null);
         }
         moveInFormation(owner, null);
      }
   }

   private boolean shouldRunFormationBrain() {
      int phase = Math.floorMod(this.poolIndex >= 0 ? this.poolIndex : this.getId(), FORMATION_BRAIN_STRIDE);
      return Math.floorMod(this.tickCount + phase, FORMATION_BRAIN_STRIDE) == 0;
   }

   private boolean tickServantCardVanguard() {
      CompoundTag data = this.getPersistentData();
      if (!data.hasUUID(TAG_CARD_OWNER)) {
         return false;
      }
      if (!(this.level() instanceof ServerLevel level)) {
         return true;
      }
      ServerPlayer owner = level.getServer().getPlayerList().getPlayer(data.getUUID(TAG_CARD_OWNER));
      boolean cardIonioi = data.getBoolean(TAG_CARD_IONIOI);
      if (owner == null || !owner.isAlive()
         || cardIonioi && (!owner.getPersistentData().getBoolean("ServantCardIskandarIonioiActive")
            || !ModDimensions.isIonioiHetairoiDimension(level.dimension().location()))
         || !cardIonioi && level.getGameTime() > data.getLong(TAG_CARD_UNTIL)) {
         if (cardIonioi && data.hasUUID(TAG_CARD_OWNER)) {
            CARD_FORMATION_TARGET_CACHE.remove(data.getUUID(TAG_CARD_OWNER));
         }
         this.discard();
         return true;
      }
      recoverIfEmbedded(level);
      if (cardIonioi) {
         tickServantCardIonioi(owner, level);
         return true;
      }
      LivingEntity target = this.getTarget();
      if (!isValidTarget(target)) {
         target = owner.getLastHurtMob();
      }
      if (!isValidTarget(target)) {
         target = owner.getLastHurtByMob();
      }
      if (!isValidTarget(target)) {
         target = nearestOwnerEnemy(owner, level);
      }
      if (isValidTarget(target)) {
         this.setTarget(target);
         this.getNavigation().moveTo(target, 1.22);
      } else if (this.distanceToSqr(owner) > 7.0 * 7.0) {
         this.getNavigation().moveTo(owner, 1.15);
      } else {
         this.getNavigation().stop();
      }
      return true;
   }

   private void tickServantCardIonioi(ServerPlayer owner, ServerLevel level) {
      if (level.getGameTime() < this.actionStartTick) {
         this.setTarget(null);
         this.getNavigation().stop();
         this.setDeltaMovement(Vec3.ZERO);
         return;
      }
      if (!shouldRunFormationBrain()) {
         return;
      }
      LivingEntity target = cardIonioiTarget(owner, level);
      if (isValidTarget(target)) {
         if (this.getTarget() != null) {
            this.setTarget(null);
         }
         Vec3 slot = formationSurroundSlot(owner, target);
         moveDirectlyInFormation(slot, FORMATION_ASSAULT_SPEED, true);
         tryDirectSpearAttack(target);
      } else {
         if (this.getTarget() != null) {
            this.setTarget(null);
         }
         moveInCardFormation(owner, null);
      }
   }

   @Nullable
   private LivingEntity cardIonioiTarget(ServerPlayer owner, ServerLevel level) {
      FormationTargetCache cache = CARD_FORMATION_TARGET_CACHE.computeIfAbsent(owner.getUUID(), ignored -> new FormationTargetCache());
      cache.refreshCard(owner, level, this);
      LivingEntity cached = cache.targetForCardFormation(owner, level, formationNumber());
      if (cached != null) {
         return cached;
      }
      LivingEntity directThreat = this.directCardArmyThreat(owner);
      if (directThreat != null) {
         return directThreat;
      }
      return cache.fallbackCardTarget(owner, level);
   }

   @Nullable
   private LivingEntity directCardArmyThreat(ServerPlayer owner) {
      LivingEntity personalThreat = this.getTarget();
      if (isValidTarget(personalThreat)) {
         return personalThreat;
      }
      personalThreat = this.getLastHurtByMob();
      if (isValidTarget(personalThreat)) {
         return personalThreat;
      }
      LivingEntity ownerTarget = owner.getLastHurtMob();
      if (isValidTarget(ownerTarget)) {
         return ownerTarget;
      }
      ownerTarget = owner.getLastHurtByMob();
      return isValidTarget(ownerTarget) ? ownerTarget : null;
   }

   @Nullable
   private LivingEntity nearestOwnerEnemy(ServerPlayer owner, ServerLevel level) {
      LivingEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(18.0),
         entity -> entity != owner && entity.isAlive() && !owner.isAlliedTo(entity) && !entity.isAlliedTo(owner) && !this.isAlliedTo(entity))) {
         double distance = candidate.distanceToSqr(owner);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = candidate;
         }
      }
      return best;
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      boolean success = super.doHurtTarget(target);
      if (success) {
         this.swing(InteractionHand.MAIN_HAND);
      }
      return success;
   }

   @Override
   public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
      if (isFriendly(source.getEntity()) || isFriendly(source.getDirectEntity())) {
         return false;
      }
      if (source.is(DamageTypeTags.IS_FALL)) {
         this.fallDistance = 0.0F;
         return false;
      }
      amount = applyMacedonianGuard(source, amount);
      return super.hurt(source, amount);
   }

   private float applyMacedonianGuard(net.minecraft.world.damagesource.DamageSource source, float amount) {
      if (amount <= 0.0F || this.getOffhandItem().isEmpty()
         || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || source.is(DamageTypeTags.BYPASSES_SHIELD)) {
         return amount;
      }
      StatRank rank = this.getSoldierRank();
      ServantParams params = new ServantParams(rank, false, rank, false, rank, false, rank, false, rank, false);
      double normalReduction = ServantCombatFormulas.blockReduction(params);
      double reduction = rank == StatRank.A || rank == StatRank.B ? normalReduction : normalReduction * 0.5;
      if (this.level() instanceof ServerLevel level) {
         level.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE,
            rank == StatRank.A || rank == StatRank.B ? 0.9F : 0.62F, 0.92F + this.getRandom().nextFloat() * 0.18F);
      }
      return (float)(amount * (1.0 - reduction));
   }

   @Override
   public void die(net.minecraft.world.damagesource.DamageSource source) {
      if (this.level() instanceof ServerLevel level) {
         IskandarEntity iskandar = this.getIskandar(level);
         if (iskandar != null) {
            iskandar.onMacedonianSoldierDeath(this);
         }
         CompoundTag data = this.getPersistentData();
         if (data.getBoolean(TAG_CARD_IONIOI) && data.hasUUID(TAG_CARD_OWNER)) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(data.getUUID(TAG_CARD_OWNER));
            if (owner != null) {
               ServantCardIskandarSkills.onMacedonianSoldierDeath(owner, this);
            }
         }
      }
      super.die(source);
   }

   @Override
   protected void tickDeath() {
      this.deathTime++;
      this.setDeltaMovement(this.getDeltaMovement().multiply(0.25, 0.1, 0.25));
      if (this.deathTime >= SERVANT_DISSOLVE_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
         this.level().broadcastEntityEvent(this, (byte)60);
         this.remove(Entity.RemovalReason.KILLED);
      }
   }

   public boolean isShortServantDissolving() {
      return this.deathTime > 0;
   }

   public float getShortServantDissolveProgress(float partialTick) {
      if (this.deathTime <= 0) {
         return 0.0F;
      }
      return net.minecraft.util.Mth.clamp((this.deathTime + partialTick) / SERVANT_DISSOLVE_TICKS, 0.0F, 1.0F);
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) {
         return true;
      }
      return this.isFriendly(other);
   }

   private boolean isFriendly(@Nullable Entity entity) {
      if (entity == null) {
         return false;
      }
      if (this.iskandarUuid != null && this.iskandarUuid.equals(entity.getUUID())) {
         return true;
      }
      CompoundTag data = this.getPersistentData();
      if (data.hasUUID(TAG_CARD_OWNER) && data.getUUID(TAG_CARD_OWNER).equals(entity.getUUID())) {
         return true;
      }
      if (entity instanceof MacedonianSoldierEntity soldier) {
         if (this.iskandarUuid != null && this.iskandarUuid.equals(soldier.iskandarUuid)) {
            return true;
         }
         CompoundTag soldierData = soldier.getPersistentData();
         return data.hasUUID(TAG_CARD_OWNER) && soldierData.hasUUID(TAG_CARD_OWNER)
            && data.getUUID(TAG_CARD_OWNER).equals(soldierData.getUUID(TAG_CARD_OWNER));
      }
      if (this.level() instanceof ServerLevel level) {
         IskandarEntity iskandar = this.getIskandar(level);
         return iskandar != null && iskandar.isAlliedTo(entity);
      }
      return false;
   }

   private boolean isValidTarget(@Nullable LivingEntity target) {
      if (target == null || !target.isAlive() || target == this || this.isAlliedTo(target)) {
         return false;
      }
      return !EntityUtils.isImmunePlayerTarget(target) || isMarkedIonioiTarget(target);
   }

   @Nullable
   private LivingEntity formationTarget(IskandarEntity owner, ServerLevel level) {
      FormationTargetCache cache = FORMATION_TARGET_CACHE.computeIfAbsent(owner.getUUID(), ignored -> new FormationTargetCache());
      cache.refresh(owner, level, this);
      LivingEntity cached = cache.targetForFormation(owner, level, formationNumber());
      if (cached != null) {
         return cached;
      }
      LivingEntity directThreat = this.directNpcArmyThreat(owner);
      return directThreat != null ? directThreat : cache.fallbackTarget(owner, level);
   }

   @Nullable
   private LivingEntity directNpcArmyThreat(IskandarEntity owner) {
      LivingEntity target = owner.getTarget();
      if (isValidTarget(target)) {
         return target;
      }
      target = this.getTarget();
      if (isValidTarget(target)) {
         return target;
      }
      target = this.getLastHurtByMob();
      if (isValidTarget(target)) {
         return target;
      }
      target = owner.getLastHurtMob();
      if (isValidTarget(target)) {
         return target;
      }
      target = owner.getLastHurtByMob();
      return isValidTarget(target) ? target : null;
   }

   private boolean isAssignedIonioiTarget(IskandarEntity owner, LivingEntity candidate) {
      if (candidate == null || !candidate.isAlive() || candidate == this || this.isAlliedTo(candidate)) {
         return false;
      }
      CompoundTag data = candidate.getPersistentData();
      CompoundTag ownerData = owner.getPersistentData();
      return data.hasUUID(TAG_IONIOI_TARGET_OWNER)
         && owner.getUUID().equals(data.getUUID(TAG_IONIOI_TARGET_OWNER))
         && ownerData.hasUUID(TAG_IONIOI_SESSION)
         && data.hasUUID(TAG_IONIOI_TARGET_SESSION)
         && ownerData.getUUID(TAG_IONIOI_SESSION).equals(data.getUUID(TAG_IONIOI_TARGET_SESSION));
   }

   private boolean isAssignedCardIonioiTarget(ServerPlayer owner, LivingEntity candidate) {
      if (candidate == null || !candidate.isAlive() || candidate == this || this.isAlliedTo(candidate)) {
         return false;
      }
      CompoundTag data = candidate.getPersistentData();
      CompoundTag ownerData = owner.getPersistentData();
      return data.hasUUID(TAG_IONIOI_TARGET_OWNER)
         && owner.getUUID().equals(data.getUUID(TAG_IONIOI_TARGET_OWNER))
         && ownerData.hasUUID(TAG_CARD_IONIOI_SESSION)
         && data.hasUUID(TAG_IONIOI_TARGET_SESSION)
         && ownerData.getUUID(TAG_CARD_IONIOI_SESSION).equals(data.getUUID(TAG_IONIOI_TARGET_SESSION));
   }

   private boolean isMarkedIonioiTarget(LivingEntity candidate) {
      CompoundTag data = candidate.getPersistentData();
      if (!data.hasUUID(TAG_IONIOI_TARGET_OWNER)) {
         return false;
      }
      if (this.iskandarUuid != null && this.iskandarUuid.equals(data.getUUID(TAG_IONIOI_TARGET_OWNER))) {
         if (!(this.level() instanceof ServerLevel level)) {
            return false;
         }
         IskandarEntity owner = this.getIskandar(level);
         return owner != null
            && owner.getPersistentData().hasUUID(TAG_IONIOI_SESSION)
            && data.hasUUID(TAG_IONIOI_TARGET_SESSION)
            && owner.getPersistentData().getUUID(TAG_IONIOI_SESSION).equals(data.getUUID(TAG_IONIOI_TARGET_SESSION));
      }
      CompoundTag selfData = this.getPersistentData();
      return selfData.hasUUID(TAG_CARD_OWNER)
         && selfData.getUUID(TAG_CARD_OWNER).equals(data.getUUID(TAG_IONIOI_TARGET_OWNER))
         && this.level() instanceof ServerLevel level
         && level.getServer().getPlayerList().getPlayer(selfData.getUUID(TAG_CARD_OWNER)) instanceof ServerPlayer owner
         && owner.getPersistentData().hasUUID(TAG_CARD_IONIOI_SESSION)
         && data.hasUUID(TAG_IONIOI_TARGET_SESSION)
         && owner.getPersistentData().getUUID(TAG_CARD_IONIOI_SESSION).equals(data.getUUID(TAG_IONIOI_TARGET_SESSION));
   }

   private boolean isThreateningNpcArmy(IskandarEntity owner, LivingEntity candidate) {
      if (!isValidTarget(candidate)) {
         return false;
      }
      if (candidate == owner.getTarget() || candidate == owner.getLastHurtMob() || candidate == owner.getLastHurtByMob()
         || candidate == this.getTarget() || candidate == this.getLastHurtByMob()) {
         return true;
      }
      if (isFriendly(candidate.getLastHurtMob())) {
         return true;
      }
      if (candidate instanceof Mob mob && isFriendly(mob.getTarget())) {
         return true;
      }
      return candidate instanceof Monster && candidate.distanceToSqr(owner) <= FORMATION_THREAT_SCAN_RADIUS * FORMATION_THREAT_SCAN_RADIUS;
   }

   private boolean isThreateningCardArmy(ServerPlayer owner, LivingEntity candidate) {
      if (!isValidTarget(candidate)) {
         return false;
      }
      if (candidate == owner.getLastHurtMob() || candidate == owner.getLastHurtByMob()
         || candidate == this.getTarget() || candidate == this.getLastHurtByMob()) {
         return true;
      }
      if (isFriendly(candidate.getLastHurtMob())) {
         return true;
      }
      if (candidate instanceof Mob mob && isFriendly(mob.getTarget())) {
         return true;
      }
      return candidate instanceof Monster && candidate.distanceToSqr(owner) <= FORMATION_THREAT_SCAN_RADIUS * FORMATION_THREAT_SCAN_RADIUS;
   }

   private void moveInFormation(IskandarEntity owner, @Nullable LivingEntity target) {
      Vec3 slot = formationSlot(owner, target);
      double speed = target == null ? 1.05 : 1.16;
      if (this.distanceToSqr(slot) > 1.0) {
         moveDirectlyInFormation(slot, speed, target != null);
      } else {
         this.getNavigation().stop();
         this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
      }
      if ((this.tickCount + Math.floorMod(this.poolIndex, 5)) % 5 == 0) {
         facePosition(target != null ? target.position() : owner.position().add(owner.getLookAngle()));
      }
   }

   private Vec3 formationSurroundSlot(LivingEntity owner, LivingEntity target) {
      int formationIndex = Math.floorMod(this.poolIndex >= 0 ? this.poolIndex : this.getId(), FORMATION_ACTIVE_CAP);
      int formation = formationIndex / FORMATION_SIZE;
      int local = formationIndex % FORMATION_SIZE;
      int row = local / FORMATION_SIDE;
      int col = local % FORMATION_SIDE;
      double angle = Math.PI * 2.0 * formation / Math.max(1, FORMATION_ACTIVE_CAP / FORMATION_SIZE);
      Vec3 outward = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
      Vec3 tangent = new Vec3(-outward.z, 0.0, outward.x);
      double radius = 3.0 + row * 0.8;
      double tangentOffset = (col - (FORMATION_SIDE - 1) * 0.5) * 0.95;
      Vec3 slot = target.position().add(outward.scale(radius)).add(tangent.scale(tangentOffset));
      Vec3 fromOwner = horizontal(slot.subtract(owner.position()));
      if (fromOwner.lengthSqr() > FORMATION_THREAT_SCAN_RADIUS * FORMATION_THREAT_SCAN_RADIUS) {
         fromOwner = fromOwner.normalize().scale(FORMATION_THREAT_SCAN_RADIUS);
         slot = owner.position().add(fromOwner);
      }
      return slot;
   }

   private void moveInCardFormation(ServerPlayer owner, @Nullable LivingEntity target) {
      Vec3 slot = cardFormationSlot(owner, target);
      double speed = target == null ? 1.05 : 1.16;
      if (this.distanceToSqr(slot) > 1.0) {
         moveDirectlyInFormation(slot, speed, target != null);
      } else {
         this.getNavigation().stop();
         this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
      }
      if ((this.tickCount + Math.floorMod(this.poolIndex, 5)) % 5 == 0) {
         facePosition(target != null ? target.position() : owner.position().add(owner.getLookAngle()));
      }
   }

   private void tryDirectSpearAttack(LivingEntity target) {
      if (this.directSpearCooldown > 0 || !isFormationVolleyWindow() || !shouldCheckPersonalSpearThisTick()) {
         return;
      }
      facePosition(target.position());
      if (canSpearReach(target)) {
         this.directSpearCooldown = SPEAR_ATTACK_INTERVAL_TICKS + Math.floorMod(this.poolIndex >= 0 ? this.poolIndex : this.getId(), 3);
         this.swing(InteractionHand.MAIN_HAND);
         emitSpearThrustFeedback(target);
         this.doHurtTarget(target);
      }
   }

   private boolean isFormationVolleyWindow() {
      long now = this.level().getGameTime();
      int phase = Math.floorMod((int)(now + formationNumber() * 3L), FORMATION_VOLLEY_INTERVAL_TICKS);
      return phase < FORMATION_VOLLEY_WINDOW_TICKS;
   }

   private boolean shouldCheckPersonalSpearThisTick() {
      int phase = Math.floorMod(this.poolIndex >= 0 ? this.poolIndex : this.getId(), PERSONAL_SPEAR_CHECK_STRIDE);
      return Math.floorMod(this.tickCount + phase, PERSONAL_SPEAR_CHECK_STRIDE) == 0;
   }

   private boolean canSpearReach(LivingEntity target) {
      Vec3 soldierCenter = this.position().add(0.0, this.getBbHeight() * 0.45, 0.0);
      Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      Vec3 toTarget = targetCenter.subtract(soldierCenter);
      Vec3 flatToTarget = horizontal(toTarget);
      double distanceSqr = flatToTarget.lengthSqr();
      if (distanceSqr < 1.0E-4) {
         return false;
      }
      double reach = this.getBbWidth() * 2.0F + target.getBbWidth() + SPEAR_REACH_BONUS;
      if (distanceSqr > reach * reach || Math.abs(toTarget.y) > 2.6) {
         return false;
      }
      Vec3 facing = horizontal(this.getLookAngle());
      if (facing.lengthSqr() < 1.0E-4) {
         facing = flatToTarget;
      }
      double dot = facing.normalize().dot(flatToTarget.normalize());
      if (dot < SPEAR_FORWARD_DOT_MIN) {
         return false;
      }
      double sideDistanceSqr = Math.max(0.0, distanceSqr * (1.0 - dot * dot));
      double lane = SPEAR_LANE_HALF_WIDTH + target.getBbWidth() * 0.5;
      return sideDistanceSqr <= lane * lane && this.getSensing().hasLineOfSight(target);
   }

   private void emitSpearThrustFeedback(LivingEntity target) {
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      int local = Math.floorMod(this.poolIndex >= 0 ? this.poolIndex : this.getId(), FORMATION_SIZE);
      if (local % FORMATION_SIDE != 0) {
         return;
      }
      Vec3 hit = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, hit.x, hit.y, hit.z, 1, 0.12, 0.08, 0.12, 0.0);
      level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.28F, 0.82F + this.getRandom().nextFloat() * 0.12F);
   }

   private void moveDirectlyInFormation(Vec3 slot, double speed, boolean activeAdvance) {
      long now = this.level().getGameTime();
      int cadence = activeAdvance ? FORMATION_DIRECT_PATH_REFRESH_TICKS : FORMATION_IDLE_PATH_REFRESH_TICKS;
      if (this.lastPathSlot != null && now < this.nextPathRefreshTick && this.lastPathSlot.distanceToSqr(slot) < 1.0) {
         return;
      }
      this.lastPathSlot = slot;
      this.nextPathRefreshTick = now + cadence + Math.floorMod(this.poolIndex >= 0 ? this.poolIndex : this.getId(), cadence);
      this.getNavigation().stop();
      if (this.distanceToSqr(slot) > FORMATION_DIRECT_MOVE_STOP_DISTANCE_SQR) {
         if (this.level() instanceof ServerLevel level) {
            slot = new Vec3(slot.x, cachedFormationY(level, Mth.floor(slot.x), Mth.floor(slot.z), now), slot.z);
         }
         this.getMoveControl().setWantedPosition(slot.x, slot.y, slot.z, speed);
      }
   }

   private void recoverIfEmbedded(ServerLevel level) {
      if ((this.tickCount + this.getId()) % EMBEDDED_RECOVERY_INTERVAL != 0
         || (!this.isInWall() && level.noCollision(this, this.getBoundingBox()))) {
         return;
      }
      for (int up = 1; up <= 5; up++) {
         AABB moved = this.getBoundingBox().move(0.0, up, 0.0);
         if (level.noCollision(this, moved)) {
            this.setPos(this.getX(), this.getY() + up, this.getZ());
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 0.0, 0.2));
            return;
         }
      }
      int safeY = findSafeSpawnY(level, Mth.floor(this.getX()), Mth.floor(this.getZ()));
      this.moveTo(this.getX(), safeY, this.getZ(), this.getYRot(), this.getXRot());
      this.setDeltaMovement(Vec3.ZERO);
   }

   private static int cachedFormationY(ServerLevel level, int x, int z, long now) {
      long key = BlockPos.asLong(x, 0, z);
      CachedFormationY cached = FORMATION_Y_CACHE.get(key);
      if (cached != null && now - cached.tick <= 100L) {
         return cached.y;
      }
      int y = findSafeSpawnY(level, x, z);
      if (FORMATION_Y_CACHE.size() > 8192) {
         FORMATION_Y_CACHE.clear();
      }
      FORMATION_Y_CACHE.put(key, new CachedFormationY(y, now));
      return y;
   }

   private static int findSafeSpawnY(ServerLevel level, int x, int z) {
      int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      int minY = level.getMinBuildHeight() + 1;
      int maxY = level.getMaxBuildHeight() - 2;
      int startY = Mth.clamp(surfaceY - 1, minY, maxY);
      int endY = Mth.clamp(surfaceY + 5, minY, maxY);
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, startY, z);
      for (int y = startY; y <= endY; y++) {
         cursor.set(x, y, z);
         BlockState state = level.getBlockState(cursor);
         if (!state.isAir()
            && state.isFaceSturdy(level, cursor, Direction.UP)
            && level.getBlockState(cursor.above()).isAir()
            && level.getBlockState(cursor.above(2)).isAir()) {
            return y + 1;
         }
      }
      return Mth.clamp(72, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
   }

   private Vec3 formationSlot(IskandarEntity owner, @Nullable LivingEntity target) {
      Vec3 forward = target != null
         ? horizontal(target.position().subtract(owner.position()))
         : horizontal(owner.getLookAngle());
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      int formationIndex = Math.floorMod(this.poolIndex >= 0 ? this.poolIndex : this.getId(), FORMATION_ACTIVE_CAP);
      int formation = formationIndex / FORMATION_SIZE;
      int local = formationIndex % FORMATION_SIZE;
      int row = local / FORMATION_SIDE;
      int col = local % FORMATION_SIDE;
      double soldierCenter = (FORMATION_SIDE - 1) * 0.5;
      double formationCenterX = (FORMATION_GRID_WIDTH - 1) * 0.5;
      double formationCenterZ = (FORMATION_GRID_DEPTH - 1) * 0.5;
      double groupPitch = FORMATION_SPACING * FORMATION_SIDE + FORMATION_GAP;
      double localX = (formation % FORMATION_GRID_WIDTH - formationCenterX) * groupPitch + (col - soldierCenter) * FORMATION_SPACING;
      double localZ = (formation / FORMATION_GRID_WIDTH - formationCenterZ) * groupPitch + (row - soldierCenter) * FORMATION_SPACING;
      double advance = 0.0;
      if (target != null) {
         double distance = Math.sqrt(owner.distanceToSqr(target));
         advance = Math.max(0.0, Math.min(FORMATION_ADVANCE_LIMIT, distance - FORMATION_KEEP_DISTANCE));
      }
      Vec3 center = owner.position().add(forward.scale(advance));
      return center.add(right.scale(localX)).add(forward.scale(localZ));
   }

   private Vec3 cardFormationSlot(ServerPlayer owner, @Nullable LivingEntity target) {
      Vec3 forward = target != null
         ? horizontal(target.position().subtract(owner.position()))
         : horizontal(owner.getLookAngle());
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      int formationIndex = Math.floorMod(this.poolIndex >= 0 ? this.poolIndex : this.getId(), FORMATION_ACTIVE_CAP);
      int formation = formationIndex / FORMATION_SIZE;
      int local = formationIndex % FORMATION_SIZE;
      int row = local / FORMATION_SIDE;
      int col = local % FORMATION_SIDE;
      double soldierCenter = (FORMATION_SIDE - 1) * 0.5;
      double formationCenterX = (FORMATION_GRID_WIDTH - 1) * 0.5;
      double formationCenterZ = (FORMATION_GRID_DEPTH - 1) * 0.5;
      double groupPitch = FORMATION_SPACING * FORMATION_SIDE + FORMATION_GAP;
      double localX = (formation % FORMATION_GRID_WIDTH - formationCenterX) * groupPitch + (col - soldierCenter) * FORMATION_SPACING;
      double localZ = (formation / FORMATION_GRID_WIDTH - formationCenterZ) * groupPitch + (row - soldierCenter) * FORMATION_SPACING;
      double advance = 0.0;
      if (target != null) {
         double distance = Math.sqrt(owner.distanceToSqr(target));
         advance = Math.max(0.0, Math.min(FORMATION_ADVANCE_LIMIT, distance - FORMATION_KEEP_DISTANCE));
      }
      Vec3 center = owner.position().add(forward.scale(advance));
      return center.add(right.scale(localX)).add(forward.scale(localZ));
   }

   private int formationNumber() {
      int formationIndex = Math.floorMod(this.poolIndex >= 0 ? this.poolIndex : this.getId(), FORMATION_ACTIVE_CAP);
      return formationIndex / FORMATION_SIZE;
   }

   private void facePosition(Vec3 target) {
      Vec3 direction = horizontal(target.subtract(this.position()));
      if (direction.lengthSqr() < 1.0E-4) {
         return;
      }
      direction = direction.normalize();
      float yaw = (float)(Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI);
      this.setYRot(yaw);
      this.setYHeadRot(yaw);
      this.setYBodyRot(yaw);
      this.getLookControl().setLookAt(target.x, target.y + 1.0, target.z, 30.0F, 30.0F);
   }

   private static Vec3 horizontal(Vec3 vector) {
      return new Vec3(vector.x, 0.0, vector.z);
   }

   private static final class MacedonianSpearAttackGoal extends MeleeAttackGoal {
      private final MacedonianSoldierEntity soldier;
      private int spearAttackCooldown;

      private MacedonianSpearAttackGoal(MacedonianSoldierEntity soldier, double speedModifier, boolean followingTargetEvenIfNotSeen) {
         super(soldier, speedModifier, followingTargetEvenIfNotSeen);
         this.soldier = soldier;
      }

      @Override
      protected void checkAndPerformAttack(LivingEntity target) {
         this.spearAttackCooldown = Math.max(this.spearAttackCooldown - 1, 0);
         if (this.canPerformAttack(target)) {
            this.spearAttackCooldown = this.adjustedTickDelay(SPEAR_ATTACK_INTERVAL_TICKS);
            this.soldier.swing(InteractionHand.MAIN_HAND);
            this.soldier.doHurtTarget(target);
         }
      }

      @Override
      protected boolean canPerformAttack(LivingEntity target) {
         return this.spearAttackCooldown <= 0 && this.soldier.canSpearReach(target);
      }
   }

   private static final class FormationTargetCache {
      private long scanTick = Long.MIN_VALUE;
      private long rotationTick = Long.MIN_VALUE;
      private int rotation;
      private final List<UUID> targets = new ArrayList<>();
      @Nullable private UUID fallbackTarget;

      private void refresh(IskandarEntity owner, ServerLevel level, MacedonianSoldierEntity soldier) {
         long now = level.getGameTime();
         if (now - this.scanTick < FORMATION_TARGET_SCAN_INTERVAL) {
            return;
         }
         this.scanTick = now;
         this.targets.clear();
         List<LivingEntity> scanned = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(FORMATION_THREAT_SCAN_RADIUS),
            candidate -> soldier.isAssignedIonioiTarget(owner, candidate) || soldier.isThreateningNpcArmy(owner, candidate));
         scanned.sort((left, right) -> Double.compare(left.distanceToSqr(owner), right.distanceToSqr(owner)));
         List<UUID> threatTargets = new ArrayList<>();
         LivingEntity fallback = null;
         for (LivingEntity target : scanned) {
            if (soldier.isAssignedIonioiTarget(owner, target)) {
               this.targets.add(target.getUUID());
            } else {
               threatTargets.add(target.getUUID());
               if (fallback == null) {
                  fallback = target;
               }
            }
         }
         if (this.targets.isEmpty()) {
            this.targets.addAll(threatTargets);
         }
         this.fallbackTarget = fallback != null ? fallback.getUUID() : null;
      }

      private void refreshCard(ServerPlayer owner, ServerLevel level, MacedonianSoldierEntity soldier) {
         long now = level.getGameTime();
         if (now - this.scanTick < FORMATION_TARGET_SCAN_INTERVAL) {
            return;
         }
         this.scanTick = now;
         this.targets.clear();
         List<LivingEntity> scanned = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(FORMATION_THREAT_SCAN_RADIUS),
            candidate -> soldier.isAssignedCardIonioiTarget(owner, candidate) || soldier.isThreateningCardArmy(owner, candidate));
         scanned.sort((left, right) -> Double.compare(left.distanceToSqr(owner), right.distanceToSqr(owner)));
         List<UUID> threatTargets = new ArrayList<>();
         LivingEntity fallback = null;
         for (LivingEntity target : scanned) {
            if (soldier.isAssignedCardIonioiTarget(owner, target)) {
               this.targets.add(target.getUUID());
            } else {
               threatTargets.add(target.getUUID());
               if (fallback == null) {
                  fallback = target;
               }
            }
         }
         if (this.targets.isEmpty()) {
            this.targets.addAll(threatTargets);
         }
         this.fallbackTarget = fallback != null ? fallback.getUUID() : null;
      }

      @Nullable
      private LivingEntity targetForFormation(IskandarEntity owner, ServerLevel level, int formation) {
         if (this.targets.isEmpty()) {
            return null;
         }
         long currentRotationTick = level.getGameTime() / 80L;
         if (this.targets.size() <= 4) {
            this.rotation = 0;
         } else if (this.rotationTick != currentRotationTick) {
            this.rotationTick = currentRotationTick;
            this.rotation = (int)(currentRotationTick % this.targets.size());
         }
         UUID targetId = this.targets.get(Math.floorMod(formation + this.rotation, this.targets.size()));
         Entity entity = level.getEntity(targetId);
         return entity instanceof LivingEntity living && living.isAlive() && !owner.isAlliedTo(living) ? living : null;
      }

      @Nullable
      private LivingEntity targetForCardFormation(ServerPlayer owner, ServerLevel level, int formation) {
         if (this.targets.isEmpty()) {
            return null;
         }
         long currentRotationTick = level.getGameTime() / 80L;
         if (this.targets.size() <= 4) {
            this.rotation = 0;
         } else if (this.rotationTick != currentRotationTick) {
            this.rotationTick = currentRotationTick;
            this.rotation = (int)(currentRotationTick % this.targets.size());
         }
         UUID targetId = this.targets.get(Math.floorMod(formation + this.rotation, this.targets.size()));
         Entity entity = level.getEntity(targetId);
         return entity instanceof LivingEntity living && living.isAlive() && !owner.isAlliedTo(living) ? living : null;
      }

      @Nullable
      private LivingEntity fallbackTarget(IskandarEntity owner, ServerLevel level) {
         if (this.fallbackTarget == null) {
            return null;
         }
         Entity entity = level.getEntity(this.fallbackTarget);
         return entity instanceof LivingEntity living && living.isAlive() && !owner.isAlliedTo(living) ? living : null;
      }

      @Nullable
      private LivingEntity fallbackCardTarget(ServerPlayer owner, ServerLevel level) {
         if (this.fallbackTarget == null) {
            return null;
         }
         Entity entity = level.getEntity(this.fallbackTarget);
         return entity instanceof LivingEntity living && living.isAlive() && !owner.isAlliedTo(living) ? living : null;
      }
   }

   private static final class CachedFormationY {
      private final int y;
      private final long tick;

      private CachedFormationY(int y, long tick) {
         this.y = y;
         this.tick = tick;
      }
   }

   @Nullable
   private IskandarEntity getIskandar(ServerLevel level) {
      if (this.iskandarUuid == null) {
         return null;
      }
      Entity entity = level.getEntity(this.iskandarUuid);
      return entity instanceof IskandarEntity iskandar ? iskandar : null;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.iskandarUuid != null) {
         tag.putUUID("IonioiHetairoiOwner", this.iskandarUuid);
      }
      tag.putInt("IonioiHetairoiPoolIndex", this.poolIndex);
      tag.putString("IonioiHetairoiRank", this.getSoldierRank().name());
      tag.putLong("IonioiHetairoiActionStartTick", this.actionStartTick);
      tag.putFloat("MacedonianSoldierVisualScale", this.getVisualScale());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID("IonioiHetairoiOwner")) {
         this.iskandarUuid = tag.getUUID("IonioiHetairoiOwner");
      }
      this.poolIndex = tag.getInt("IonioiHetairoiPoolIndex");
      this.actionStartTick = tag.getLong("IonioiHetairoiActionStartTick");
      if (this.actionStartTick <= 0L && this.iskandarUuid != null && this.level() instanceof ServerLevel level) {
         this.actionStartTick = level.getGameTime() + IskandarEntity.IONIOI_FORMATION_DELAY_TICKS;
      }
      this.setSoldierRank(StatRank.fromKey(tag.getString("IonioiHetairoiRank")));
      if (tag.contains("MacedonianSoldierVisualScale")) {
         this.setVisualScale(tag.getFloat("MacedonianSoldierVisualScale"));
      } else {
         this.setVisualScale(0.9F + this.getRandom().nextFloat() * 0.1F);
      }
      this.equipPhalanxGear();
   }

}

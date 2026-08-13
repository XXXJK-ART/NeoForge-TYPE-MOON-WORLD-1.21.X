package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.BucephalusEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GordiusWheelEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.IskandarMountEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MacedonianSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.iskandar.IonioiHetairoiRankPool;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
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
   private static final ResourceLocation LEADERSHIP_ATTACK_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "iskandar_leadership_attack");
   private static final ResourceLocation MOUNT_ARMOR_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "iskandar_mount_armor");
   private static final int LEADERSHIP_COOLDOWN = 25 * 20;
   private static final int LEADERSHIP_DURATION = 20 * 20;
   private static final int MILITARY_TACTICS_COOLDOWN = 20 * 20;
   private static final int MILITARY_TACTICS_DURATION = 15 * 20;
   private static final int MOUNT_SUMMON_COOLDOWN = 30 * 20;
   private static final int WHEEL_CHARGE_COOLDOWN = 15 * 20;
   private static final int IONIOI_COOLDOWN = 90 * 20;
   public static final int IONIOI_FORMATION_DELAY_TICKS = 2 * 20;
   private static final int IONIOI_FREE_UPKEEP = 30 * 20;
   private static final int IONIOI_ACTIVE_CAP = 200;
   private static final double IONIOI_MP_COST = 120.0;
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
      ensurePersistentMounts(level);
      updateMountArmor();
      tickLeadership(level);
      tickMilitaryTactics(level);
      tickWheelCombat(level);
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

   private void ensurePersistentMounts(ServerLevel level) {
      BucephalusEntity horse = getBucephalus(level);
      if (horse == null && level.getGameTime() >= this.getPersistentData().getLong("IskandarMountSummonCooldown")) {
         horse = ModEntities.BUCEPHALUS.get().create(level);
         if (horse != null) {
            horse.moveTo(this.getX() + 2.0, this.getY(), this.getZ() + 1.0, this.getYRot(), 0.0F);
            horse.bindIskandar(this, this.getEntityMaster());
            level.addFreshEntity(horse);
            this.bucephalusUuid = horse.getUUID();
         }
      }
      GordiusWheelEntity wheel = getGordiusWheel(level);
      if (wheel == null && level.getGameTime() >= this.getPersistentData().getLong("IskandarMountSummonCooldown")) {
         wheel = ModEntities.GORDIUS_WHEEL.get().create(level);
         if (wheel != null) {
            wheel.moveTo(this.getX(), this.getY() + 1.2, this.getZ() + 3.0, this.getYRot(), 0.0F);
            wheel.bindIskandar(this, this.getEntityMaster());
            level.addFreshEntity(wheel);
            this.gordiusWheelUuid = wheel.getUUID();
            this.getPersistentData().putLong("IskandarMountSummonCooldown", level.getGameTime() + MOUNT_SUMMON_COOLDOWN);
         }
      }
      if (!this.isPassenger() && wheel != null && wheel.isAlive() && this.getTarget() != null && this.distanceToSqr(wheel) < 36.0) {
         this.startRiding(wheel, true);
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

   private void tickWheelCombat(ServerLevel level) {
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
         startIonioiHetairoi(level);
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
      LivingEntity target = this.getTarget();
      if (target == null || !target.isAlive()) {
         return false;
      }
      int enemies = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(24.0),
         entity -> entity != this && entity.isAlive() && !this.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(entity)).size();
      return enemies >= 5 || target.getMaxHealth() >= 300.0F || this.getHealth() <= this.getMaxHealth() * 0.3F;
   }

   private void startIonioiHetairoi(ServerLevel level) {
      CompoundTag data = this.getPersistentData();
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
      spawnInitialFormation(level);
      level.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.2, this.getZ(), 4, 0.0, 0.0, 0.0, 0.0);
      level.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.4F, 0.75F);
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
      for (UUID id : List.copyOf(this.ionioiSoldiers)) {
         Entity entity = level.getEntity(id);
         if (entity instanceof MacedonianSoldierEntity soldier) {
            soldier.discard();
         }
      }
      this.ionioiSoldiers.clear();
      level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 60, 10.0, 2.0, 10.0, 0.12);
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
   private BucephalusEntity getBucephalus(ServerLevel level) {
      if (this.bucephalusUuid == null) {
         return null;
      }
      Entity entity = level.getEntity(this.bucephalusUuid);
      return entity instanceof BucephalusEntity horse ? horse : null;
   }

   @Nullable
   private GordiusWheelEntity getGordiusWheel(ServerLevel level) {
      if (this.gordiusWheelUuid == null) {
         return null;
      }
      Entity entity = level.getEntity(this.gordiusWheelUuid);
      return entity instanceof GordiusWheelEntity wheel ? wheel : null;
   }

   public static boolean isIonioiArea(BlockPos pos) {
      return false;
   }
}

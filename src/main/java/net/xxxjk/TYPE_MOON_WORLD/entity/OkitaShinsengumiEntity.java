package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;

public final class OkitaShinsengumiEntity extends ShinsengumiEntity {
   private static final String TAG_OWNER = "OkitaShinsengumiOwner";
   private static final String TAG_TARGET = "OkitaShinsengumiTarget";
   private static final String TAG_EXPIRES = "OkitaShinsengumiExpires";
   private static final String TAG_NATURAL_TIMEOUT = "OkitaShinsengumiNaturalTimeout";
   private static final double FOLLOW_STOP_DISTANCE_SQR = 3.0 * 3.0;
   private static final double FOLLOW_START_DISTANCE_SQR = 5.0 * 5.0;
   private static final double FOLLOW_URGENT_DISTANCE_SQR = 14.0 * 14.0;
   @Nullable private UUID ownerUuid;
   @Nullable private UUID targetUuid;

   public OkitaShinsengumiEntity(EntityType<? extends OkitaShinsengumiEntity> type, Level level) {
      super(type, level);
      this.setSchool(KendoSchool.TENNEN);
      this.setProficiency(85);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createMobAttributes()
         .add(Attributes.MAX_HEALTH, 200.0)
         .add(Attributes.MOVEMENT_SPEED, 0.30)
         .add(Attributes.ATTACK_DAMAGE, 15.0)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.FOLLOW_RANGE, 48.0)
         .add(Attributes.ATTACK_SPEED, 4.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.35);
   }

   public void initializeForOkita(OkitaSoujiSaberEntity owner, @Nullable LivingEntity target, long expiresAt) {
      this.ownerUuid = owner.getUUID();
      this.targetUuid = target == null ? null : target.getUUID();
      this.getPersistentData().putUUID(TAG_OWNER, owner.getUUID());
      if (target != null) {
         this.getPersistentData().putUUID(TAG_TARGET, target.getUUID());
         this.setTarget(target);
      }
      this.getPersistentData().putLong(TAG_EXPIRES, expiresAt);
      this.getPersistentData().putBoolean(TAG_NATURAL_TIMEOUT, false);
      this.setSchool(KendoSchool.TENNEN);
      this.setProficiency(90);
      this.equipOkitaLoadout();
      this.clearPersonalName();
      this.setHealth(this.getMaxHealth());
      this.setPersistenceRequired();
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
                                       @Nullable SpawnGroupData data) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
      this.setSchool(KendoSchool.TENNEN);
      this.setProficiency(90);
      this.equipOkitaLoadout();
      this.clearPersonalName();
      return result;
   }

   @Override
   protected void customServerAiStep() {
      if (!(this.level() instanceof ServerLevel level)) {
         super.customServerAiStep();
         return;
      }
      CompoundTag data = this.getPersistentData();
      if (this.ownerUuid == null && data.hasUUID(TAG_OWNER)) {
         this.ownerUuid = data.getUUID(TAG_OWNER);
      }
      if (this.targetUuid == null && data.hasUUID(TAG_TARGET)) {
         this.targetUuid = data.getUUID(TAG_TARGET);
      }
      long expires = data.getLong(TAG_EXPIRES);
      OkitaSoujiSaberEntity owner = this.getOwner(level);
      if (expires > 0L && level.getGameTime() >= expires || owner == null || !owner.isAlive()) {
         data.putBoolean(TAG_NATURAL_TIMEOUT, true);
         this.discard();
         return;
      }
      LivingEntity target = owner.getTarget();
      if (!this.isValidAssignedTarget(owner, target)) {
         target = this.getTargetByUuid(level, this.targetUuid);
      }
      if (this.isValidAssignedTarget(owner, target)) {
         this.targetUuid = target.getUUID();
         data.putUUID(TAG_TARGET, this.targetUuid);
         this.setTarget(target);
      } else {
         this.targetUuid = null;
         data.remove(TAG_TARGET);
         this.setTarget(null);
      }
      this.equipOkitaLoadout();
      super.customServerAiStep();
      this.clearPersonalName();
      if (this.getTarget() == null) {
         this.followOwnerWhenIdle(owner);
      }
   }

   @Override
   public void ensureRandomName() {
      this.clearPersonalName();
   }

   @Override
   protected boolean canProactivelyTarget(LivingEntity target) {
      return false;
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) {
         return true;
      }
      if (!(this.level() instanceof ServerLevel level)) {
         return false;
      }
      OkitaSoujiSaberEntity owner = this.getOwner(level);
      if (owner == null) {
         return false;
      }
      if (other == owner || owner.isAlliedTo(other)) {
         return true;
      }
      ServerPlayer master = owner.getEntityMaster();
      if (master != null && (other == master || master.isAlliedTo(other))) {
         return true;
      }
      if (other instanceof OkitaShinsengumiEntity soldier) {
         UUID otherOwner = soldier.ownerUuid;
         if (otherOwner == null && soldier.getPersistentData().hasUUID(TAG_OWNER)) {
            otherOwner = soldier.getPersistentData().getUUID(TAG_OWNER);
         }
         return this.ownerUuid != null && this.ownerUuid.equals(otherOwner);
      }
      return false;
   }

   @Override
   public void die(net.minecraft.world.damagesource.DamageSource source) {
      if (!this.getPersistentData().getBoolean(TAG_NATURAL_TIMEOUT) && this.level() instanceof ServerLevel level) {
         OkitaSoujiSaberEntity owner = this.getOwner(level);
         if (owner != null) {
            owner.onShinsengumiKilled();
         }
      }
      super.die(source);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.ownerUuid != null) {
         tag.putUUID(TAG_OWNER, this.ownerUuid);
      }
      if (this.targetUuid != null) {
         tag.putUUID(TAG_TARGET, this.targetUuid);
      }
      tag.putLong(TAG_EXPIRES, this.getPersistentData().getLong(TAG_EXPIRES));
      tag.putBoolean(TAG_NATURAL_TIMEOUT, this.getPersistentData().getBoolean(TAG_NATURAL_TIMEOUT));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.ownerUuid = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
      this.targetUuid = tag.hasUUID(TAG_TARGET) ? tag.getUUID(TAG_TARGET) : null;
      this.getPersistentData().putLong(TAG_EXPIRES, tag.getLong(TAG_EXPIRES));
      this.getPersistentData().putBoolean(TAG_NATURAL_TIMEOUT, tag.getBoolean(TAG_NATURAL_TIMEOUT));
      if (this.ownerUuid != null) {
         this.getPersistentData().putUUID(TAG_OWNER, this.ownerUuid);
      }
      if (this.targetUuid != null) {
         this.getPersistentData().putUUID(TAG_TARGET, this.targetUuid);
      }
      this.equipOkitaLoadout();
      this.clearPersonalName();
   }

   @Nullable
   private OkitaSoujiSaberEntity getOwner(ServerLevel level) {
      if (this.ownerUuid == null) {
         return null;
      }
      Entity entity = level.getEntity(this.ownerUuid);
      return entity instanceof OkitaSoujiSaberEntity okita ? okita : null;
   }

   @Nullable
   private LivingEntity getTargetByUuid(ServerLevel level, @Nullable UUID uuid) {
      if (uuid == null) {
         return null;
      }
      Entity entity = level.getEntity(uuid);
      return entity instanceof LivingEntity living ? living : null;
   }

   private boolean isValidAssignedTarget(OkitaSoujiSaberEntity owner, @Nullable LivingEntity target) {
      return target != null && target.isAlive() && target != this && target != owner
         && !owner.isAlliedTo(target) && !this.isAlliedTo(target)
         && !EntityUtils.isImmunePlayerTarget(target);
   }

   private void followOwnerWhenIdle(OkitaSoujiSaberEntity owner) {
      double distanceSqr = this.distanceToSqr(owner);
      if (distanceSqr <= FOLLOW_STOP_DISTANCE_SQR) {
         this.getNavigation().stop();
         return;
      }
      if (distanceSqr < FOLLOW_START_DISTANCE_SQR && !this.getNavigation().isDone()) {
         return;
      }
      double speed = distanceSqr >= FOLLOW_URGENT_DISTANCE_SQR ? 1.35 : 1.05;
      this.getNavigation().moveTo(owner, speed);
   }

   private void clearPersonalName() {
      if (!this.level().isClientSide()) {
         this.setCustomName(null);
         this.setCustomNameVisible(false);
      }
   }

   private void equipOkitaLoadout() {
      if (!this.getMainHandItem().is(ModItems.KATANA.get())) {
         this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.KATANA.get()));
         this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
      }
   }
}

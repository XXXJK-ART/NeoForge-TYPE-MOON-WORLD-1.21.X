package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.SoulSnapshot;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills;

public final class SoulEchoEntity extends OwnedPaleRiderMob {
   private static final EntityDataAccessor<String> SOURCE_TYPE = SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<String> SOUL_KIND = SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<String> PLAYER_PROFILE = SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<String> PLAYER_UUID = SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Float> SOURCE_WIDTH = SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> SOURCE_HEIGHT = SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.FLOAT);
   private UUID soulId;
   private SoulSnapshot snapshot;
   private long nextTargetScanTick;

   public SoulEchoEntity(EntityType<? extends SoulEchoEntity> type, Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.MOVEMENT_SPEED, 0.23)
         .add(Attributes.ATTACK_DAMAGE, 3.0)
         .add(Attributes.ARMOR, 0.0)
         .add(Attributes.ARMOR_TOUGHNESS, 0.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
         .add(Attributes.FOLLOW_RANGE, 48.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(SOURCE_TYPE, "minecraft:zombie");
      builder.define(SOUL_KIND, SoulSnapshot.SoulKind.CREATURE.name());
      builder.define(PLAYER_PROFILE, "");
      builder.define(PLAYER_UUID, "");
      builder.define(SOURCE_WIDTH, 0.6F);
      builder.define(SOURCE_HEIGHT, 1.8F);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (this.getSoulKind() == SoulSnapshot.SoulKind.SERVANT) {
         PaleRiderEntity oldOwner = this.getPaleRiderOwner();
         if (oldOwner != null && this.soulId != null) {
            oldOwner.consumeManifestedSoul(this.soulId);
         }
         this.discard();
         return;
      }
      LivingEntity owner = this.getPaleRiderLivingOwner();
      boolean active = owner instanceof PaleRiderEntity rider ? rider.isUnderworldActive() || rider.isPossessing(this)
         : owner instanceof ServerPlayer player && PaleRiderInfectionService.isPaleRiderCardPlayer(player)
            && (ServantCardPaleRiderSkills.isUnderworldActive(player) || player.getVehicle() == this);
      if (owner == null || !owner.isAlive() || !active) {
         if (owner instanceof ServerPlayer player && this.snapshot != null) {
            ServantCardPaleRiderSkills.returnManifestedSoul(player, this);
         }
         this.discard();
         return;
      }
      if (this.getTarget() == null || !this.getTarget().isAlive() || this.getTarget().isAlliedTo(owner) || owner.isAlliedTo(this.getTarget())) {
         if (this.getTarget() != null) this.setTarget(null);
         long now = this.level().getGameTime();
         if (this.nextTargetScanTick == 0L) {
            this.nextTargetScanTick = now + Math.floorMod(this.getId(), 20);
         }
         if (now < this.nextTargetScanTick) return;
         this.nextTargetScanTick = now + 20L + Math.floorMod(this.getId(), 10);
         LivingEntity target = owner instanceof PaleRiderEntity rider ? rider.findPaleRiderEnemy(50.0)
            : owner instanceof ServerPlayer player ? ServantCardPaleRiderSkills.findSoulEchoTarget(player, this) : null;
         this.setTarget(target);
      }
   }

   public void applySnapshot(SoulSnapshot soul) {
      this.soulId = soul.id();
      this.snapshot = soul;
      this.entityData.set(SOURCE_TYPE, soul.entityType());
      this.entityData.set(SOUL_KIND, soul.kind().name());
      this.entityData.set(PLAYER_PROFILE, soul.playerProfile());
      this.entityData.set(PLAYER_UUID, soul.playerUuid());
      this.entityData.set(SOURCE_WIDTH, Math.max(0.1F, soul.width()));
      this.entityData.set(SOURCE_HEIGHT, Math.max(0.1F, soul.height()));
      this.refreshDimensions();
      this.setCustomName(net.minecraft.network.chat.Component.literal(soul.displayName()));
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(1.0, soul.maxHealth()));
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(0.0, soul.attackDamage()));
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Math.max(0.05, soul.movementSpeed()));
      this.getAttribute(Attributes.ARMOR).setBaseValue(Math.max(0.0, soul.armor()));
      this.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(Math.max(0.0, soul.armorToughness()));
      this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(Math.max(0.0, Math.min(1.0, soul.knockbackResistance())));
      this.setHealth(this.getMaxHealth());
   }

   public SoulSnapshot getSnapshot() {
      return this.snapshot;
   }

   public UUID getSoulId() {
      return this.soulId;
   }

   public String getSourceType() {
      return this.entityData.get(SOURCE_TYPE);
   }

   public SoulSnapshot.SoulKind getSoulKind() {
      try {
         return SoulSnapshot.SoulKind.valueOf(this.entityData.get(SOUL_KIND));
      } catch (IllegalArgumentException ignored) {
         return SoulSnapshot.SoulKind.CREATURE;
      }
   }

   public String getPlayerProfile() {
      return this.entityData.get(PLAYER_PROFILE);
   }

   public String getPlayerUuid() {
      return this.entityData.get(PLAYER_UUID);
   }

   @Override
   public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
      if (this.entityData == null) return super.getDefaultDimensions(pose);
      return net.minecraft.world.entity.EntityDimensions.scalable(this.entityData.get(SOURCE_WIDTH), this.entityData.get(SOURCE_HEIGHT));
   }

   @Override
   public void die(DamageSource source) {
      PaleRiderEntity owner = this.getPaleRiderOwner();
      if (owner != null && this.soulId != null) {
         owner.consumeManifestedSoul(this.soulId);
      }
      super.die(source);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.soulId != null) tag.putUUID("SoulId", this.soulId);
      if (this.snapshot != null) tag.put("SoulSnapshot", this.snapshot.save());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID("SoulId")) this.soulId = tag.getUUID("SoulId");
      if (tag.contains("SoulSnapshot")) {
         this.snapshot = SoulSnapshot.load(tag.getCompound("SoulSnapshot"));
         this.applySnapshot(this.snapshot);
      }
   }
}
